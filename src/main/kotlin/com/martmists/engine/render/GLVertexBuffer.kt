package com.martmists.engine.render

import com.martmists.engine.util.ResourceWithCleanup
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.opengl.GL46.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GLVertexBuffer(private val type: Int = GL_ARRAY_BUFFER) : ResourceWithCleanup() {
    constructor(data: FloatArray, usage: Int = GL_STATIC_DRAW, type: Int = GL_ARRAY_BUFFER) : this(type) {
        setData(data, usage, type)
    }
    constructor(data: IntArray, usage: Int = GL_STATIC_DRAW, type: Int = GL_ARRAY_BUFFER) : this(type) {
        setData(data, usage, type)
    }

    private val id = glGenBuffers()

    fun bind(target: Int = type) {
        glBindBuffer(target, id)
    }

    fun bindBase(index: Int, target: Int = type) {
        glBindBufferBase(target, index, id)
    }

    fun setData(data: FloatArray, usage: Int = GL_STATIC_DRAW, target: Int = type) {
        bind(target)
        glBufferData(target, data, usage)
    }

    fun setData(data: IntArray, usage: Int = GL_STATIC_DRAW, target: Int = type) {
        bind(target)
        glBufferData(target, data, usage)
    }

    fun setData(data: FloatBuffer, usage: Int = GL_STATIC_DRAW, target: Int = type) {
        bind(target)
        glBufferData(target, data, usage)
    }

    fun setData(data: IntBuffer, usage: Int = GL_STATIC_DRAW, target: Int = type) {
        bind(target)
        glBufferData(target, data, usage)
    }

    fun setData(data: ByteBuffer, usage: Int = GL_STATIC_DRAW, target: Int = type) {
        bind(target)
        glBufferData(target, data, usage)
    }

    override fun toString(): String {
        return "GLBuffer(#$id)"
    }

    override fun createCleaner(): Runnable = VBOCleaner(glfwGetCurrentContext(), id)

    private class VBOCleaner(val ctx: Long, val id: Int) : Runnable {
        override fun run() {
            val toRestore = glfwGetCurrentContext()
            glfwMakeContextCurrent(ctx)
            glDeleteBuffers(id)
            glfwMakeContextCurrent(toRestore)
        }
    }
}
