package com.martmists.engine.util

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.opengl.GL46.*

object GLGarbageCollector {
    private val vbos = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val vaos = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val fbos = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val rbos = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val shaders = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val textures = atomic(mutableMapOf<Long, MutableList<Int>>())
    private val callbacks = atomic(mutableMapOf<Long, MutableList<() -> Unit>>())
    private val contexts = atomic(mutableListOf<Long>())

    fun clean() {
        val toRestore = glfwGetCurrentContext()
        for ((ctx, vbos) in vbos.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            glDeleteBuffers(vbos.toIntArray())
        }
        for ((ctx, vaos) in vaos.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            glDeleteVertexArrays(vaos.toIntArray())
        }
        for ((ctx, fbos) in fbos.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            glDeleteFramebuffers(fbos.toIntArray())
        }
        for ((ctx, rbos) in rbos.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            glDeleteRenderbuffers(rbos.toIntArray())
        }
        for ((ctx, shaders) in shaders.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            for (shader in shaders) {
                glDeleteProgram(shader)
            }
        }
        for ((ctx, textures) in textures.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            glDeleteTextures(textures.toIntArray())
        }
        for ((ctx, callbacks) in callbacks.getAndSet(mutableMapOf())) {
            glfwMakeContextCurrent(ctx)
            for (callback in callbacks) {
                callback.invoke()
            }
        }
        for (ctx in contexts.getAndSet(mutableListOf())) {
            glfwMakeContextCurrent(ctx)
            glfwFreeCallbacks(ctx)
            glfwDestroyWindow(ctx)
        }
        glfwMakeContextCurrent(toRestore)
    }

    fun markVBO(ctx: Long, vbo: Int) { vbos.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(vbo); it } }
    fun markVAO(ctx: Long, vao: Int) { vaos.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(vao); it } }
    fun markFBO(ctx: Long, fbo: Int) { fbos.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(fbo); it } }
    fun markRBO(ctx: Long, rbo: Int) { rbos.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(rbo); it } }
    fun markShader(ctx: Long, shader: Int)  { shaders.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(shader); it } }
    fun markTexture(ctx: Long, texture: Int)  { textures.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(texture); it } }
    fun markCallback(ctx: Long, callback: () -> Unit)  { callbacks.getAndUpdate { it.getOrPut(ctx, ::mutableListOf).add(callback); it } }
    fun markContext(ctx: Long)  { contexts.getAndUpdate { it.add(ctx); it } }
}
