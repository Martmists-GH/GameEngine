package com.martmists.engine.util

import org.lwjgl.glfw.GLFW.glfwGetCurrentContext

class ContextLazy<T>(private val provider: () -> T) : Lazy<T> {
    private val contextValues = mutableMapOf<Long, T>()

    override val value: T
        get() = contextValues.getOrPut(glfwGetCurrentContext(), provider)

    override fun isInitialized(): Boolean {
        return contextValues.containsKey(glfwGetCurrentContext())
    }
}

fun <T> contextLazy(provider: () -> T): Lazy<T> = ContextLazy(provider)
