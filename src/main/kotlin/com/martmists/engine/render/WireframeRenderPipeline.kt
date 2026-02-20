package com.martmists.engine.render

import com.martmists.engine.scene.Viewport
import com.martmists.engine.component.*
import com.martmists.engine.data.BuiltinShaders
import com.martmists.engine.math.Color
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Vec3
import com.martmists.engine.model.ModelMesh
import com.martmists.engine.model.ModelPartInstance
import com.martmists.engine.scene.GameObject
import com.martmists.engine.util.ImGuiRenderUtil
import com.martmists.engine.util.contextLazy
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryUtil
import java.util.WeakHashMap
import kotlin.random.Random

object WireframeRenderPipeline : RenderPipeline {
    // === Axes ===
    private object Axes : Mesh<Unit>("Axes", run {
        val inf = 10000f
        floatArrayOf(
            0f, 0f, 0f, 1f, 0f, 0f,
            inf, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0f, 1f, 0f,
            0f, inf, 0f, 0f, 1f, 0f,
            0f, 0f, 0f, 0f, 0f, 1f,
            0f, 0f, inf, 0f, 0f, 1f,
        )
    }, drawMode = GL_LINES) {
        override fun GLVertexArray.setupAttributes() {
            attrib(3)  // aPos
            attrib(3)  // aColor
            ivbo(2)
        }

        override fun stride() = 6 * 4
    }

    // === Directional Lights ===
    private object DirectionalLights : Mesh<DirectionalLight>("DirectionalLights", floatArrayOf(
        0f, 0f, 0f,
        0f, 0f, -1f,
        0f, 0f, -1f,
        0.1f, 0.1f, -0.9f,
        0f, 0f, -1f,
        -0.1f, 0.1f, -0.9f,
        0f, 0f, -1f,
        0.1f, -0.1f, -0.9f,
        0f, 0f, -1f,
        -0.1f, -0.1f, -0.9f,
    ), drawMode = GL_LINES) {
        private val dataVbo = GLVertexBuffer()

        override fun bindData(data: List<DirectionalLight>) {
            val buffer = MemoryUtil.memAllocFloat(data.size * 3)
            for (light in data) {
                buffer.put(light.color.r * light.intensity)
                buffer.put(light.color.g * light.intensity)
                buffer.put(light.color.b * light.intensity)
            }
            buffer.flip()
            dataVbo.setData(buffer, usage = GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(buffer)
        }

        override fun GLVertexArray.setupAttributes() {
            // aPos
            attrib(3)

            // aColor
            dataVbo.bind()
            stride = 3 * 4
            resetOffset()
            attrib(3, divisor = 1)

            // aInstanceMatrix
            ivbo(2)
        }

        override fun stride() = 3 * 4
    }

    // === Point Lights ===
    // TODO

    // === Spot Lights ===
    // TODO

    // === Mesh Wrapper ===

    private class MeshWrapper(private val original: ModelMesh) {
        private val vbo = GLVertexBuffer()
        private val vao by contextLazy {
            val v = GLVertexArray()
            v.bind()
            v.stride = (3 + 3 + 2 + 2 + 3 + 3) * 4
            original.vbo.bind()
            original.ebo!!.bind()
            v.apply {
                attrib(3)
                attrib(3)
                attrib(2)
                attrib(2)
                attrib(3)
                attrib(3)
                original.bvbo?.let {
                    it.bind()
                    stride = 4 * 4
                    resetOffset()
                    attrib(4, type = GL_INT)
                }
                original.bwvbo?.let {
                    it.bind()
                    stride = 4 * 4
                    resetOffset()
                    attrib(4)
                }

                // aColor
                vbo.bind()
                stride = 3 * 4
                resetOffset()
                attrib(3, divisor = 1)

                original.ivbo.bind()
                stride = 16 * 4
                for (i in 0..3) {
                    val loc = 9 + i
                    attrib(4, offset=i * 4 * 4, loc=loc, divisor = 1)
                }
            }
            v
        }

        fun render(transforms: List<Mat4x4>, boneTransforms: List<Array<Mat4x4>>, colors: List<Color>) {
            require(transforms.size == boneTransforms.size || boneTransforms.isEmpty())
            require(transforms.size == colors.size)

            val buffer = MemoryUtil.memAllocFloat(transforms.size * 16)
            transforms.forEach {
                val here = buffer.position()
                it.joml.get(buffer)
                buffer.position(here + 16)
            }
            buffer.flip()

            original.ivbo.setData(buffer, usage = GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(buffer)

            original.bindData(boneTransforms)

            val colorBuffer = MemoryUtil.memAllocFloat(colors.size * 3 * 4)
            colors.forEach {
                colorBuffer.put(it.r)
                colorBuffer.put(it.g)
                colorBuffer.put(it.b)
            }
            colorBuffer.flip()
            vbo.setData(colorBuffer, usage = GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(colorBuffer)

            vao.bind()
            glDrawElementsInstanced(GL_TRIANGLES, original.vertexCount, GL_UNSIGNED_INT, 0, transforms.size)
        }
    }

    // === Quad Wireframe ===

    private object WireframeMesh : Mesh<Color>("Quad", floatArrayOf(
        0f, 0f, 0f,
        0f, 1f, 0f,
        1f, 0f, 0f,
        1f, 1f, 0f,
    ), indices = intArrayOf(0, 1, 2, 1, 3, 2)) {
        private val colorVbo = GLVertexBuffer()

        override fun bindData(data: List<Color>) {
            val colorBuffer = MemoryUtil.memAllocFloat(data.size * 3)
            data.forEach {
                colorBuffer.put(it.r)
                colorBuffer.put(it.g)
                colorBuffer.put(it.b)
            }
            colorBuffer.flip()
            colorVbo.setData(colorBuffer, usage = GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(colorBuffer)
        }

        override fun GLVertexArray.setupAttributes() {
            attrib(3)
            colorVbo.bind()
            resetOffset()
            attrib(3, divisor = 1)
            ivbo(2)
        }

        override fun stride() = 3 * 4
    }

    private val meshWrappersCache = WeakHashMap<ModelMesh, MeshWrapper>()

    class BatchEntry(
        val transform: Mat4x4,
        val boneMatrices: Array<Mat4x4>?,
        val color: Color,
    )

    override fun render(viewport: Viewport, buffer: Framebuffer) {
        val cameraObj = viewport.camera ?: return
        val camera = cameraObj.getComponent<Camera>()
        val scene = viewport.scene
        val objects = scene.allObjects()

        glClearColor(0f, 0f, 0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        var shader = BuiltinShaders.wireframeSimple

        shader.bind()
        shader.setUniform("u_View", camera.viewMatrix)
        shader.setUniform("u_Proj", camera.projectionMatrix)
        Axes.renderSingle(Mat4x4.Identity, Unit)
        shader.unbind()

        val directionalLights = objects.filter { it.hasComponent<DirectionalLight>() }
        val pointLights = objects.filter { it.hasComponent<PointLight>() }
        val spotLights = objects.filter { it.hasComponent<SpotLight>() }

        if (directionalLights.isNotEmpty()) {
            shader = BuiltinShaders.wireframeSimple
            shader.bind()
            shader.setUniform("u_View", camera.viewMatrix)
            shader.setUniform("u_Proj", camera.projectionMatrix)

            DirectionalLights.render(directionalLights.map { it.transform.modelMatrix() }, directionalLights.map { it.getComponent<DirectionalLight>() })
            shader.unbind()
        }

        if (pointLights.isNotEmpty()) {
            // TODO
        }

        if (spotLights.isNotEmpty()) {
            // TODO
        }

        // TODO: Instanced rendering
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        shader = BuiltinShaders.wireframeSimple
        shader.bind()
        shader.setUniform("u_View", camera.viewMatrix)
        shader.setUniform("u_Proj", camera.projectionMatrix)
        for (obj in objects) {
            if (obj.hasComponent<SpriteRenderer>()) {
                val sr = obj.getComponent<SpriteRenderer>()
                val sprite = sr.sprite ?: continue
                WireframeMesh.render(listOf(obj.transform.modelMatrix().scale(Vec3(sr.stretch.x * sprite.aspectRatio, sr.stretch.y, 1f))), listOf(obj.meshColor()))
            }
        }
        shader.unbind()

        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        shader = BuiltinShaders.wireframeMesh
        shader.bind()
        shader.setUniform("u_View", camera.viewMatrix)
        shader.setUniform("u_Proj", camera.projectionMatrix)

        val renderData = mutableMapOf<ModelMesh, MutableList<BatchEntry>>()

        for (go in objects) {
            if (!go.hasComponent<ModelRenderer>()) continue
            val mr = go.getComponent<ModelRenderer>()
            val modelInst = mr.model ?: continue

            val hasBones = modelInst.model.boneOffsets.isNotEmpty()
            val rootTransform = go.transform.modelMatrix()
            val boneMatrices = if (hasBones) modelInst.calculateBoneMatrices(rootTransform) else null
            val color = go.meshColor()

            fun collectParts(p: ModelPartInstance, modelTransform: Mat4x4) {
                val currentTransform = if (boneMatrices != null) rootTransform else modelTransform * p.transform

                for (geometry in p.partRef.geometries) {
                    renderData.getOrPut(geometry.mesh, ::mutableListOf).add(BatchEntry(currentTransform, boneMatrices, color))
                }

                for (child in p.children) {
                    collectParts(child, modelTransform * p.transform)
                }
            }

            collectParts(modelInst.root, rootTransform)
        }

        for ((mesh, data) in renderData) {
            val wrapper = meshWrappersCache.getOrPut(mesh) { MeshWrapper(mesh) }
            shader.setUniform("u_HasBones", data.any { it.boneMatrices != null })
            wrapper.render(
                data.map { it.transform },
                data.mapNotNull { it.boneMatrices },
                data.map { it.color }
            )
        }
        shader.unbind()

        val imguiObjects = objects.filter { it.hasComponent<ImguiRenderer>() }
        ImGuiRenderUtil.render {
            imguiObjects.forEach {
                val ir = it.getComponent<ImguiRenderer>()
                ir.render()
            }
        }
    }

    private fun GameObject.meshColor(): Color {
        // hashCode default to addr, assuming 8-byte alignment this should remove the meaningless first 3 bits
        return Random(this.hashCode() shr 3).let { Color.fromHSV(it.nextFloat() * 360f, 1.0f, 1.0f) }
    }
}
