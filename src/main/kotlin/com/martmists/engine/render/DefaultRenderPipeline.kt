package com.martmists.engine.render

import com.martmists.engine.scene.Viewport
import com.martmists.engine.component.*
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Vec3
import com.martmists.engine.model.Material
import com.martmists.engine.model.ModelMesh
import com.martmists.engine.model.Model
import com.martmists.engine.model.ModelPartInstance
import com.martmists.engine.util.ImGuiRenderUtil
import org.lwjgl.opengl.GL46.*

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
        viewport.camera?.let { cameraObj ->
            val camera = cameraObj.getComponent<Camera>()

            val directionalLights = scene.objects.filter { it.hasComponent<DirectionalLight>() }.map { it.getComponent<DirectionalLight>() }
            val pointLights = scene.objects.filter { it.hasComponent<PointLight>() }.map { it.getComponent<PointLight>() }
            val spotLights = scene.objects.filter { it.hasComponent<SpotLight>() }.map { it.getComponent<SpotLight>() }

            val batches = mutableMapOf<BatchInfo, MutableList<BatchEntry>>()
            val modelInfo = mutableMapOf<BatchInfo, Model>()

            for (go in scene.objects) {
                if (go.hasComponent<ModelRenderer>()) {
                    val mr = go.getComponent<ModelRenderer>()
                    val modelInst = mr.model ?: continue
                    val rootTransform = go.transform.modelMatrix()
                    val boneMatrices = if (modelInst.model.boneOffsets.isNotEmpty()) modelInst.calculateBoneMatrices(rootTransform) else null

                    fun collectParts(part: ModelPartInstance, modelTransform: Mat4x4) {
                        val currentTransform = if (boneMatrices != null) rootTransform else modelTransform * part.transform
                        for (geom in part.partRef.geometries) {
                            val info = BatchInfo(geom.mesh, geom.material, modelInst.model.makeShader(), boneMatrices != null)
                            batches.getOrPut(info, ::mutableListOf).add(BatchEntry(currentTransform, boneMatrices))
                            modelInfo.getOrPut(info) { modelInst.model }
                        }
                        for (child in part.children) {
                            collectParts(child, modelTransform * part.transform)
                        }
                    }
                    collectParts(modelInst.root, rootTransform)
                }
            }

            for ((info, batch) in batches.entries) {
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
        }

        val imguiObjects = scene.objects.filter { it.hasComponent<ImguiRenderer>() }
        ImGuiRenderUtil.render {
            imguiObjects.forEach {
                val ir = it.getComponent<ImguiRenderer>()
                ir.render()
            }
        }
    }
}
