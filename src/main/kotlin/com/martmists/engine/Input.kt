package com.martmists.engine

import org.lwjgl.glfw.GLFW.*

object Input {
    private val keys = mutableMapOf<Long, BooleanArray>()

    fun init(window: Long) {
        keys[window] = BooleanArray(GLFW_KEY_LAST)
        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key in 0 until GLFW_KEY_LAST) {
                keys[window]!![key] = action != GLFW_RELEASE
            }
        }?.free()
    }

    fun isKeyDown(window: Long, key: Int): Boolean {
        return keys[window]?.get(key) ?: false
    }

    fun isAnyKeyDown(key: Int): Boolean {
        return keys.values.any { it[key] }
    }
}
