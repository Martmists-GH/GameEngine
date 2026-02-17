package com.martmists.engine.model

import com.martmists.engine.math.Mat4x4
import com.martmists.engine.render.GLBuffer
import com.martmists.engine.render.GLVertexArray
import com.martmists.engine.render.Mesh
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryUtil

class ModelMesh(
    name: String,
    vertices: FloatArray,
    indices: IntArray,
    boneIds: IntArray? = null,
    boneWeights: FloatArray? = null
) : Mesh<Array<Mat4x4>>(name, vertices, indices) {
    internal val bvbo = boneIds?.let(::GLBuffer)
    internal val bwvbo = boneWeights?.let(::GLBuffer)
    internal val bssbo = boneIds?.let { GLBuffer(GL_SHADER_STORAGE_BUFFER) }

    init {
        require((boneIds == null) == (boneWeights == null)) {
            "boneIds and boneWeights must both be null or not-null"
        }
    }

    override fun stride() = (3 + 3 + 2 + 2 + 3 + 3) * 4
    override val extraOptional = true

    override fun bindData(data: List<Array<Mat4x4>>) {
        if (data.isNotEmpty()) {
            val maxBones = 200
            val boneBuffer = MemoryUtil.memAllocFloat(data.size * maxBones * 16)
            for (instanceIdx in data.indices) {
                val matrices = data[instanceIdx]
                val here = boneBuffer.position()
                for (i in 0 until maxBones) {
                    if (i < matrices.size) {
                        matrices[i].joml.get(boneBuffer)
                    } else {
                        Mat4x4.Identity.joml.get(boneBuffer)
                    }
                    boneBuffer.position(here + (i + 1) * 16)
                }
            }
            boneBuffer.flip()

            bssbo!!.setData(boneBuffer, usage=GL_DYNAMIC_DRAW)
            MemoryUtil.memFree(boneBuffer)
            bssbo.bindBase(0)
        }
    }

    override fun GLVertexArray.setupAttributes() {
        // aPos
        attrib(3)
        // aNormal
        attrib(3)
        // aTexCoord0
        attrib(2)
        // aTexCoord1
        attrib(2)
        // aTangent
        attrib(3)
        // aBitangent
        attrib(3)

        // aBoneIds
        bvbo?.let {
            it.bind()
            stride = 4 * 4
            resetOffset()
            attrib(4, type = GL_INT)
        }

        // aBoneWeights
        bwvbo?.let {
            it.bind()
            stride = 4 * 4
            resetOffset()
            attrib(4)
        }

        ivbo(8)
    }

    override fun toString(): String {
        return "ModelMesh(name='$name')"
    }
}
