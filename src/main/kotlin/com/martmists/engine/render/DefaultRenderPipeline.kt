package com.martmists.engine.render

import com.martmists.engine.scene.Viewport
import com.martmists.engine.component.*
import com.martmists.engine.data.BuiltinShaders
import com.martmists.engine.ext.putVec2
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Vec2i
import com.martmists.engine.math.Vec3
import com.martmists.engine.model.Material
import com.martmists.engine.model.ModelMesh
import com.martmists.engine.model.Model
import com.martmists.engine.model.ModelPartInstance
import com.martmists.engine.sprite.SpriteAtlas
import com.martmists.engine.sprite.SpriteAtlasManager
import com.martmists.engine.util.ImGuiRenderUtil
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryUtil

object DefaultRenderPipeline : RenderPipeline {
    private data class BatchInfo(
        val mesh: ModelMesh,
        val material: Material,
        val shader: Shader,
        val hasBones: Boolean,
    )
    private class BatchEntry(val transform: Mat4x4, val boneMatrices: Array<Mat4x4>?)

    object Settings {
        var disableAmbient = false
        var disableDiffuse = false
        var disableEmissive = false
        var disableSpecular = false
        var disableNormal = false
        var disableDisplacement = true
        var useRayMarchDisplacement = true
        var renderNormals = false
    }

    private class SpriteInfo(
        val transform: Mat4x4,
        val entry: SpriteAtlas.Entry,
    )

    private object SpriteMesh : Mesh<SpriteInfo>("Sprite", floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f,
    ), indices = intArrayOf(0, 1, 2, 1, 3, 2)) {
        val spriteDataVbo = GLVertexBuffer()

        override fun bindData(data: List<SpriteInfo>) {
            val buffer = MemoryUtil.memAlloc(data.size * 4 * 4)
            for (si in data) {
                buffer.putVec2(si.entry.uvOffset())
                buffer.putVec2(si.entry.uvSize())
            }
            buffer.flip()
            spriteDataVbo.setData(buffer, usage = GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(buffer)
        }

        override fun GLVertexArray.setupAttributes() {
            attrib(2)  // aPos

            spriteDataVbo.bind()
            resetOffset()
            stride = (2 + 2) * 4
            attrib(2, divisor = 1)  // aUVOffset
            attrib(2, divisor = 1)  // aUVScale

            ivbo(3)
        }

        override fun stride() = 2 * 4
    }

    override fun render(viewport: Viewport, buffer: Framebuffer) {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_FRONT)  // FIXME: Why does this work instead of GL_BACK?
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        val scene = viewport.scene
        val objects = scene.allObjects()
        viewport.camera?.let { cameraObj ->
            val camera = cameraObj.getComponent<Camera>()

            val directionalLights = objects.filter { it.hasComponent<DirectionalLight>() }.map { it.getComponent<DirectionalLight>() }
            val pointLights = objects.filter { it.hasComponent<PointLight>() }.map { it.getComponent<PointLight>() }
            val spotLights = objects.filter { it.hasComponent<SpotLight>() }.map { it.getComponent<SpotLight>() }

            val modelBatches = mutableMapOf<BatchInfo, MutableList<BatchEntry>>()
            val modelInfo = mutableMapOf<BatchInfo, Model>()
            val spriteBatches = mutableMapOf<SpriteAtlas, MutableList<SpriteInfo>>()

            // TODO: Also collect nested objects
            for (go in objects) {
                if (go.hasComponent<ModelRenderer>()) {
                    val mr = go.getComponent<ModelRenderer>()
                    val modelInst = mr.model ?: continue
                    val rootTransform = go.transform.modelMatrix()
                    val boneMatrices = if (modelInst.model.boneOffsets.isNotEmpty()) modelInst.calculateBoneMatrices(rootTransform) else null

                    fun collectParts(part: ModelPartInstance, modelTransform: Mat4x4) {
                        val currentTransform = if (boneMatrices != null) rootTransform else modelTransform * part.transform
                        for (geom in part.partRef.geometries) {
                            val info = BatchInfo(geom.mesh, geom.material, modelInst.model.makeShader(), boneMatrices != null)
                            modelBatches.getOrPut(info, ::mutableListOf).add(BatchEntry(currentTransform, boneMatrices))
                            modelInfo.getOrPut(info) { modelInst.model }
                        }
                        for (child in part.children) {
                            collectParts(child, modelTransform * part.transform)
                        }
                    }
                    collectParts(modelInst.root, rootTransform)
                }

                if (go.hasComponent<SpriteRenderer>()) {
                    val sr = go.getComponent<SpriteRenderer>()
                    val sprite = sr.sprite ?: continue
                    val entry = SpriteAtlasManager.registerSprite(sprite)
                    val items = spriteBatches.getOrPut(entry.atlas, ::mutableListOf)
                    if (sprite.is9Slice) {
                        val corners = sprite.offset9SliceCorners!!
                        var xMul = 1f
                        var yMul = 1f
                        if (sprite.aspectRatio >= 1f) {
                            xMul = sprite.aspectRatio
                        } else {
                            yMul = 1 / sprite.aspectRatio
                        }

                        val seg1X = corners.first.x.toFloat()
                        val seg3X = sprite.size.x - corners.second.x.toFloat()
                        val seg2Xa = (corners.second.x - corners.first.x).toFloat()
                        val seg2X = sprite.size.x * sr.stretch.x - seg1X - seg3X
                        val seg2XScale = seg2X / seg2Xa
                        val seg1Y = corners.first.y.toFloat()
                        val seg3Y = sprite.size.y - corners.second.y.toFloat()
                        val seg2Ya = (corners.second.y - corners.first.y).toFloat()
                        val seg2Y = sprite.size.y * sr.stretch.y - seg1Y - seg3Y
                        val seg2YScale = seg2Y / seg2Ya

                        for (i in 0 until 9) {
                            val col = i % 3
                            val row = i / 3

                            var tr = go.transform.modelMatrix()

                            val xRange = when (col) {
                                0 -> Vec2i(0, corners.first.x)
                                1 -> Vec2i(corners.first.x, corners.second.x)
                                2 -> Vec2i(corners.second.x, sprite.size.x)
                                else -> error("Never happens")
                            }

                            val yRange = when (row) {
                                0 -> Vec2i(0, corners.first.y)
                                1 -> Vec2i(corners.first.y, corners.second.y)
                                2 -> Vec2i(corners.second.y, sprite.size.y)
                                else -> error("Never happens")
                            }

                            if (xRange.x == xRange.y || yRange.x == yRange.y) {
                                // one of the dimensions is 0, don't render
                                continue
                            }

                            val deltaX = when (col) {
                                0 -> 0f
                                1 -> seg1X
                                2 -> seg1X + seg2X
                                else -> error("Never happens")
                            }
                            val deltaY = when (row) {
                                0 -> seg3Y + seg2Y
                                1 -> seg3Y
                                2 -> 0f
                                else -> error("Never happens")
                            }
                            val partAspectRatio = when (i) {
                                0 -> seg1X / seg1Y
                                1 -> seg2Xa / seg1Y
                                2 -> seg3X / seg1Y
                                3 -> seg1X / seg2Ya
                                4 -> seg2Xa / seg2Ya
                                5 -> seg3X / seg2Ya
                                6 -> seg1X / seg3Y
                                7 -> seg2Xa / seg3Y
                                8 -> seg3X / seg3Y
                                else -> error("Never happens")
                            }
                            var partXMul = 1f
                            var partYMul = 1f
                            if (partAspectRatio >= 1f) {
                                partXMul = partAspectRatio
                            } else {
                                partYMul = 1f / partAspectRatio
                            }

                            tr = tr.translate(Vec3(deltaX * xMul / sprite.size.x, deltaY * yMul / sprite.size.y, 0f))

                            if (col == 1) {
                                tr = tr.scale(Vec3(seg2XScale * partXMul, 1f, 1f))
                            }
                            if (row == 1) {
                                tr = tr.scale(Vec3(1f, seg2YScale * partYMul, 1f))
                            }

                            items.add(SpriteInfo(
                                tr,
                                entry.slice(xRange, yRange),
                            ))
                        }
                    } else {
                        items.add(SpriteInfo(
                            go.transform.modelMatrix().scale(Vec3(sr.stretch.x, sr.stretch.y, 1f)),
                            entry,
                        ))
                    }
                }
            }

            for ((info, batch) in modelBatches.entries) {
                val (mesh, mat, shader) = info

                shader.bind()
                shader.setUniform("u_View", camera.viewMatrix)
                shader.setUniform("u_Proj", camera.projectionMatrix)
                shader.setUniform("u_ViewPos", cameraObj.transform.worldTranslation)
                shader.setUniform("u_HasBones", info.hasBones)

                shader.setUniform("u_DebugSettings.disableAmbient", Settings.disableAmbient)
                shader.setUniform("u_DebugSettings.disableDiffuse", Settings.disableDiffuse)
                shader.setUniform("u_DebugSettings.disableEmissive", Settings.disableEmissive)
                shader.setUniform("u_DebugSettings.disableSpecular", Settings.disableSpecular)
                shader.setUniform("u_DebugSettings.disableNormal", Settings.disableNormal)
                shader.setUniform("u_DebugSettings.disableDisplacement", Settings.disableDisplacement)
                shader.setUniform("u_DebugSettings.useRayMarchDisplacement", Settings.useRayMarchDisplacement)
                shader.setUniform("u_DebugSettings.viewNormals", Settings.renderNormals)

                shader.setUniform("u_NumDirLights", directionalLights.size.coerceAtMost(4))
                directionalLights.take(4).forEachIndexed { i, it ->
                    shader.setUniform("u_DirLights[$i].direction", Vec3(0f, 0f, 1f) * it.transform.worldRotation)
                    shader.setUniform("u_DirLights[$i].color", it.color)
                    shader.setUniform("u_DirLights[$i].intensity", it.intensity)
                }

                shader.setUniform("u_NumPointLights", pointLights.size.coerceAtMost(16))
                pointLights.take(16).forEachIndexed { i, it ->
                    shader.setUniform("u_PointLights[$i].position", it.transform.worldTranslation)
                    shader.setUniform("u_PointLights[$i].color", it.color)
                    shader.setUniform("u_PointLights[$i].intensity", it.intensity)
                }

                shader.setUniform("u_NumSpotLights", spotLights.size.coerceAtMost(8))
                spotLights.take(8).forEachIndexed { i, it ->
                    shader.setUniform("u_SpotLights[$i].position", it.transform.worldTranslation)
                    shader.setUniform("u_SpotLights[$i].direction", Vec3(0f, 0f, 1f) * it.transform.worldRotation)
                    shader.setUniform("u_SpotLights[$i].color", it.color)
                    shader.setUniform("u_SpotLights[$i].intensity", it.intensity)
                    shader.setUniform("u_SpotLights[$i].range", it.range)
                }

                shader.setUniform("u_Material.ambient", mat.ambientColor)
                shader.setUniform("u_Material.diffuse", mat.diffuseColor)
                shader.setUniform("u_Material.emissive", mat.emissiveColor)
                shader.setUniform("u_Material.specular", mat.specularColor)
                shader.setUniform("u_Material.shininess", mat.shininess)
                shader.setUniform("u_Material.opacity", mat.opacity)
                shader.setUniform("u_Material.reflectivity", mat.reflectivity)

                shader.setUniform("u_Material.ambientUVIndex", mat.ambientUVIndex)
                shader.setUniform("u_Material.diffuseUVIndex", mat.diffuseUVIndex)
                shader.setUniform("u_Material.emissiveUVIndex", mat.emissiveUVIndex)
                shader.setUniform("u_Material.specularUVIndex", mat.specularUVIndex)
                shader.setUniform("u_Material.normalUVIndex", mat.normalUVIndex)

                shader.setUniform("u_Material.hasAmbientTexture", mat.ambientTexture != null)
                mat.ambientTexture?.let {
                    it.bind(0)
                    shader.setUniform("u_Material.ambientTexture", 0)
                }
                shader.setUniform("u_Material.hasDiffuseTexture", mat.diffuseTexture != null)
                mat.diffuseTexture?.let {
                    it.bind(1)
                    shader.setUniform("u_Material.diffuseTexture", 1)
                }
                shader.setUniform("u_Material.hasEmissiveTexture", mat.emissiveTexture != null)
                mat.emissiveTexture?.let {
                    it.bind(2)
                    shader.setUniform("u_Material.emissiveTexture", 2)
                }
                shader.setUniform("u_Material.hasSpecularTexture", mat.specularTexture != null)
                mat.specularTexture?.let {
                    it.bind(3)
                    shader.setUniform("u_Material.specularTexture", 3)
                }
                shader.setUniform("u_Material.hasNormalTexture", mat.normalTexture != null)
                mat.normalTexture?.let {
                    it.bind(4)
                    shader.setUniform("u_Material.normalTexture", 4)
                }
                shader.setUniform("u_Material.hasDisplacementTexture", mat.displacementTexture != null)
                mat.displacementTexture?.let {
                    it.bind(5)
                    shader.setUniform("u_Material.displacementTexture", 5)
                }
                mesh.render(batch.map { it.transform }, batch.takeIf { info.hasBones }?.map { it.boneMatrices!! } ?: emptyList())

                shader.unbind()
            }

            val shader = BuiltinShaders.texturedQuad
            shader.bind()
            shader.setUniform("u_View", camera.viewMatrix)
            shader.setUniform("u_Proj", camera.projectionMatrix)
            shader.setUniform("u_TextureAtlas", 0)
            for ((atlas, sprites) in spriteBatches) {
                atlas.texture.bind(0)
                SpriteMesh.render(sprites.map(SpriteInfo::transform), sprites)
            }
            shader.unbind()
        }

        val imguiObjects = objects.filter { it.hasComponent<ImguiRenderer>() }
        ImGuiRenderUtil.render {
            imguiObjects.forEach {
                val ir = it.getComponent<ImguiRenderer>()
                ir.render()
            }
        }
    }
}
