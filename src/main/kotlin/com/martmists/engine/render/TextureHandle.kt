package com.martmists.engine.render

import com.martmists.engine.util.ResourceWithCleanup
import org.lwjgl.opengl.GL46.*
import java.nio.ByteBuffer

class TextureHandle private constructor(val id: Int, minFilter: Int, magFilter: Int) : ResourceWithCleanup() {
    constructor(minFilter: Int = GL_LINEAR, magFilter: Int = GL_LINEAR) : this(glGenTextures(), minFilter, magFilter)

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    init {
        bind()
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter)
        unbind()
    }

    fun bind(unit: Int = 0) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_2D, id)
    }

    fun unbind() {
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun setData(data: ByteBuffer, width: Int, height: Int, isSRGB: Boolean = false) {
        this.width = width
        this.height = height

        bind()
        val internalFormat = if (isSRGB) GL_SRGB8_ALPHA8 else GL_RGBA8
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        glGenerateMipmap(GL_TEXTURE_2D)
        unbind()
    }

    fun setEmpty(width: Int, height: Int) {
        this.width = width
        this.height = height

        bind()
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA8,
            width,
            height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            0
        )
        unbind()
    }

    override fun cleanup() {
        glDeleteTextures(id)
    }

    override fun toString(): String {
        return "Texture(#$id)"
    }

    companion object {
        val Default = TextureHandle(0)
    }
}
