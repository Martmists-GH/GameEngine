package com.martmists.engine.render

import com.martmists.engine.math.Color
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Vec3
import com.martmists.engine.util.GLGarbageCollector
import com.martmists.engine.util.ResourceWithCleanup
import org.intellij.lang.annotations.Language
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryStack
import kotlin.collections.iterator

class Shader(shaders: Map<Int, String>) : ResourceWithCleanup() {
    constructor(
        @Language("GLSL")
        vertexSource: String,
        @Language("GLSL")
        fragmentSource: String,
    ) : this(mapOf(
        GL_VERTEX_SHADER to vertexSource,
        GL_FRAGMENT_SHADER to fragmentSource,
    ))

    constructor(@Language("GLSL") computeSource: String) : this(mapOf(
        GL_COMPUTE_SHADER to computeSource,
    ))

    private val id = glCreateProgram()
    private val uniformCache = mutableMapOf<String, Any>()
    private fun ifNotCached(key: String, value: Any, block: () -> Unit) {
        if (uniformCache[key] != value) {
            uniformCache[key] = value
            block()
        }
    }

    init {
        registerCleaner()

        val shaderList = mutableListOf<Int>()

        for ((type, source) in shaders) {
            shaderList.add(compileShader(type, source))
        }

        for (shader in shaderList) {
            glAttachShader(id, shader)
        }

        glLinkProgram(id)

        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            throw RuntimeException("Error linking shader program: ${glGetProgramInfoLog(id)}")
        }

        for (shader in shaderList) {
            glDetachShader(id, shader)
            glDeleteShader(shader)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw RuntimeException("Error compiling shader: ${glGetShaderInfoLog(shader)}")
        }

        return shader
    }

    fun bind() {
        glUseProgram(id)
    }

    fun unbind() {
        glUseProgram(0)
    }

    fun setUniform(name: String, value: Mat4x4) = ifNotCached(name, value) {
        val loc = glGetUniformLocation(id, name)
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(16)
            value.joml.get(buffer)
            glUniformMatrix4fv(loc, false, buffer)
        }
    }

    fun setUniform(name: String, value: Array<Mat4x4>) {
        val loc = glGetUniformLocation(id, name)
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(value.size * 16)
            for (i in value.indices) {
                value[i].joml.get(i * 16, buffer)
            }
            glUniformMatrix4fv(loc, false, buffer)
        }
    }

    fun setUniform(name: String, value: Vec3) = ifNotCached(name, value)  {
        val loc = glGetUniformLocation(id, name)
        glUniform3f(loc, value.x, value.y, value.z)
    }

    fun setUniform(name: String, value: Color) = ifNotCached(name, value)  {
        val loc = glGetUniformLocation(id, name)
        glUniform4f(loc, value.r, value.g, value.b, value.a)
    }

    fun setUniform(name: String, value: Float) = ifNotCached(name, value)  {
        val loc = glGetUniformLocation(id, name)
        glUniform1f(loc, value)
    }

    fun setUniform(name: String, value: Int) = ifNotCached(name, value)  {
        val loc = glGetUniformLocation(id, name)
        glUniform1i(loc, value)
    }

    fun setUniform(name: String, value: Boolean) = ifNotCached(name, value)  {
        val loc = glGetUniformLocation(id, name)
        glUniform1i(loc, if (value) 1 else 0)
    }


    override fun createCleaner(): Runnable = ShaderCleaner(glfwGetCurrentContext(), id)

    private class ShaderCleaner(val ctx: Long, val id: Int) : Runnable {
        override fun run() {
            GLGarbageCollector.markShader(ctx, id)
        }
    }
}
