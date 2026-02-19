package com.martmists.engine.render

import com.martmists.engine.math.Mat4x4
import com.martmists.engine.util.contextLazy
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryUtil

/**
 * Represents a Mesh on the GPU, designed for instanced rendering.
 */
abstract class Mesh<T>(
    val name: String,
    vertices: FloatArray,
    indices: IntArray? = null,
    private val drawMode: Int = GL_TRIANGLES,
) {
    internal val vertexCount = indices?.size ?: (vertices.size / (stride() / 4))
    internal val vbo = GLVertexBuffer(vertices)
    internal val ebo = indices?.let { GLVertexBuffer(it, type = GL_ELEMENT_ARRAY_BUFFER) }
    internal val ivbo = GLVertexBuffer()
    private val vao by contextLazy {
        val v = GLVertexArray()
        v.bind()
        v.stride = stride()
        vbo.bind()
        ebo?.bind()
        v.setupAttributes()
        v.unbind()
        v
    }

    protected open val extraOptional = false
    open fun bindData(data: List<T>) {}
    protected abstract fun GLVertexArray.setupAttributes()
    protected abstract fun stride(): Int

    /**
     * Bind the instance VBO at the specified layout location.
     */
    protected fun GLVertexArray.ivbo(index: Int) {
        ivbo.bind()
        stride = 16 * 4
        for (i in 0..3) {
            val loc = index + i
            attrib(4, offset=i * 4 * 4, loc=loc, divisor = 1)
        }
    }

    fun renderSingle(pos: Mat4x4, extra: T?) = render(listOf(pos), listOfNotNull(extra))

    fun render(transforms: List<Mat4x4>, extraData: List<T>) {
        require(transforms.size == extraData.size || (extraOptional && extraData.isEmpty()))

        val buffer = MemoryUtil.memAllocFloat(transforms.size * 16)
        transforms.forEach {
            val here = buffer.position()
            it.joml.get(buffer)
            buffer.position(here + 16)
        }
        buffer.flip()

        ivbo.setData(buffer, usage = GL_DYNAMIC_DRAW)
        MemoryUtil.memFree(buffer)

        bindData(extraData)

        vao.bind()
        if (ebo == null) {
            glDrawArraysInstanced(drawMode, 0, vertexCount, transforms.size)
        } else {
            glDrawElementsInstanced(drawMode, vertexCount, GL_UNSIGNED_INT, 0, transforms.size)
        }
        vao.unbind()
    }

    override fun toString(): String {
        return "Mesh(name='$name')"
    }
}
