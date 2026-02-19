package com.martmists.engine.render

import com.martmists.engine.util.ResourceWithCleanup
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.opengl.GL46.*

class GLVertexArray : ResourceWithCleanup() {
    private val id = glGenVertexArrays()

    private var attrib = 0
    private var offset = 0
    var stride = 0

    fun bind() {
        glBindVertexArray(id)
    }

    fun unbind() {
        glBindVertexArray(0)
    }

    fun attrib(num: Int, stride: Int = this.stride, loc: Int = this.attrib++, offset: Int = this.offset, type: Int = GL_FLOAT, normalized: Boolean = false, divisor: Int = 0) {
        glEnableVertexAttribArray(loc)
        when (type) {
            GL_FLOAT -> {
                glVertexAttribPointer(loc, num, type, normalized, stride, offset.toLong())
            }
            GL_INT -> {
                glVertexAttribIPointer(loc, num, type, stride, offset.toLong())
            }
        }
        this.offset += num * when (type) {
            GL_FLOAT, GL_INT -> 4
            else -> error("Unsupported vertex array attribute type: $type")
        }
        check(offset < stride)
        glVertexAttribDivisor(loc, divisor)
    }

    fun resetOffset() {
        offset = 0
    }

    fun resetAttrib() {
        attrib = 0
    }

    override fun toString(): String {
        return "GLVertexArray(#$id)"
    }

    override fun createCleaner(): Runnable = VAOCleaner(glfwGetCurrentContext(), id)

    private class VAOCleaner(val ctx: Long, val id: Int) : Runnable {
        override fun run() {
            val toRestore = glfwGetCurrentContext()
            glfwMakeContextCurrent(ctx)
            glDeleteVertexArrays(id)
            glfwMakeContextCurrent(toRestore)
        }
    }
}
