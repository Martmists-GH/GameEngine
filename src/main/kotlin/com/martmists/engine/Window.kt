package com.martmists.engine

import com.martmists.engine.render.Framebuffer
import com.martmists.engine.scene.Viewport
import com.martmists.engine.util.ResourceWithCleanup
import org.joml.Vector2i
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46.*
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil.NULL

class Window(initialWidth: Int, initialHeight: Int, title: String) : ResourceWithCleanup() {
    val handle: Long
    val viewport = Viewport()
    var framebuffer: Framebuffer
    var width: Int = initialWidth
        private set
    var height: Int = initialHeight
        private set
    private val capabilities: GLCapabilities

    init {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        handle = glfwCreateWindow(initialWidth, initialHeight, title, NULL, gHandle)
        if (handle == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        if (gHandle == NULL) {
            gHandle = handle
        }

        glfwMakeContextCurrent(handle)
        capabilities = GL.createCapabilities()
        glfwSwapInterval(1)

        Input.init(handle)

        val wArr = IntArray(1)
        val hArr = IntArray(1)
        glfwGetFramebufferSize(handle, wArr, hArr)
        width = wArr[0]
        height = hArr[0]

        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            makeContextCurrent()
            width = w
            height = h
            viewport.size = Vector2i(w, h)
            framebuffer.cleanup()
            framebuffer = Framebuffer(w, h)
        }?.free()

        framebuffer = Framebuffer(width, height)
        viewport.size = Vector2i(width, height)

        glfwShowWindow(handle)
    }

    fun makeContextCurrent() {
        glfwMakeContextCurrent(handle)
        check(glfwGetCurrentContext() == handle) {
            "Wrong OpenGL context current!"
        }
        GL.setCapabilities(capabilities)
    }

    fun shouldClose(): Boolean = glfwWindowShouldClose(handle)

    fun render() {
        makeContextCurrent()
        glViewport(0, 0, width, height)


        framebuffer.bind()
        viewport.pipeline.render(viewport, framebuffer)
        framebuffer.unbind()

        framebuffer.blit()
        glfwSwapBuffers(handle)
    }

    override fun cleanup() {
        framebuffer.cleanup()
        glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
    }

    companion object {
        private var gHandle: Long = NULL
    }
}
