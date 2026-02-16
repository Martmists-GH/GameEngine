package com.martmists.engine.util

import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent

class ContextLazy<T>(private val provider: () -> T) : Lazy<T> {
    private val contextValues = mutableMapOf<Long, T>()

    override val value: T
        get() = contextValues.getOrPut(glfwGetCurrentContext(), provider)

    override fun isInitialized(): Boolean {
        return contextValues.containsKey(glfwGetCurrentContext())
    }
}

class ContextLazyWithCleanup<T : ResourceWithCleanup>(private val provider: () -> T) : Lazy<T>, ResourceWithCleanup() {
    private val contextValues = mutableMapOf<Long, T>()

    override val value: T
        get() {
            return contextValues.getOrPut(glfwGetCurrentContext(), provider)
        }

    override fun isInitialized(): Boolean {
        return contextValues.containsKey(glfwGetCurrentContext())
    }

    override fun cleanup() {
        // Ensure proper context is active during cleanup
        println("PERFORMING CLEANUP")
        val currentContext = glfwGetCurrentContext()
        for ((ctx, value) in contextValues) {
            glfwMakeContextCurrent(ctx)
            value.doCleanup()
        }
        glfwMakeContextCurrent(currentContext)
    }
}

fun <T> contextLazy(provider: () -> T): Lazy<T> = ContextLazy(provider)
@JvmName("contextLazyWithCleanup")
fun <T : ResourceWithCleanup> contextLazy(provider: () -> T): Lazy<T> = ContextLazyWithCleanup(provider)
