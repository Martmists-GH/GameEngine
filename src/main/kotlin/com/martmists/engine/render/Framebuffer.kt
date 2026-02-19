package com.martmists.engine.render

import com.martmists.engine.util.GLGarbageCollector
import com.martmists.engine.util.ResourceWithCleanup
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.GL46.*

class Framebuffer(val width: Int, val height: Int) : ResourceWithCleanup() {
    val id = glGenFramebuffers()
    val texture = TextureHandle()
    private val rbo = glGenRenderbuffers()

    init {
        registerCleaner()

        bind()

        texture.setEmpty(width, height)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.id, 0)

        glBindRenderbuffer(GL_RENDERBUFFER, rbo)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo)

        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glReadBuffer(GL_COLOR_ATTACHMENT0)

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is not complete!")
        }

        unbind()
    }

    fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
        glViewport(0, 0, width, height)
    }

    fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun blit(fb: Int = 0, x: Int = 0, y: Int = 0) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, id)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fb)
        glBlitFramebuffer(0, 0, width, height, x, y, x + width, y + height, GL_COLOR_BUFFER_BIT, GL_NEAREST)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
    }

    override fun createCleaner(): Runnable = FBOCleaner(glfwGetCurrentContext(), id, rbo)

    private class FBOCleaner(val ctx: Long, val id: Int, val rbo: Int) : Runnable {
        override fun run() {
            GLGarbageCollector.markFBO(ctx, id)
            GLGarbageCollector.markRBO(ctx, rbo)
        }
    }
}
