package com.martmists.engine.sprite

import com.martmists.engine.math.Vec2
import com.martmists.engine.math.Vec2i
import com.martmists.engine.render.TextureHandle
import com.martmists.engine.util.ResourceLoader
import org.lwjgl.opengl.GL46.*

class SpriteAtlas(
    val size: Vec2i = Vec2i(2048, 2048),
) {
    private class PositionInfo(
        val size: Vec2i,
        val offset: Vec2i,
    ) {
        fun overlaps(otherOffset: Vec2i, otherSize: Vec2i): Boolean {
            return offset.x < otherOffset.x + otherSize.x &&
                    offset.x + size.x > otherOffset.x &&
                    offset.y < otherOffset.y + otherSize.y &&
                    offset.y + size.y > otherOffset.y
        }
    }

    private val spriteMap = mutableMapOf<Sprite, PositionInfo>()
    internal val texture = TextureHandle(GL_NEAREST, GL_NEAREST).apply {
        setEmpty(size.x, size.y)
    }

    class Entry(
        val atlas: SpriteAtlas,
        val sprite: Sprite,
        val size: Vec2i,
        val offset: Vec2i,
    ) {
        fun uvOffset(frame: Int): Vec2 {
            return Vec2(
                (offset.x.toFloat() + (frame.toFloat() / sprite.numFrames) * size.x) / atlas.size.x,
                offset.y.toFloat() / atlas.size.y,
            )
        }
        fun uvSize(): Vec2 {
            return Vec2(
                size.x.toFloat() / atlas.size.x / sprite.numFrames,
                size.y.toFloat() / atlas.size.y,
            )
        }
    }

    private fun nextPow2(num: Int): Int {
        val hb = num.takeHighestOneBit()
        return if (hb == num) hb else (hb shl 1)
    }

    private fun gridPositions(alignedSize: Vec2i) = sequence {
        for (y in 0 until size.y / alignedSize.y) {
            for (x in 0 until size.x / alignedSize.x) {
                yield(Vec2i(x * alignedSize.x, y * alignedSize.y))
            }
        }
    }

    private fun gridAlignedPosition(size: Vec2i): PositionInfo? {
        val aligned = Vec2i(nextPow2(size.x), nextPow2(size.y))
        val validPos = gridPositions(aligned).firstOrNull { offset -> spriteMap.values.none {
            it.overlaps(offset, aligned)
        } } ?: return null
        return PositionInfo(size, validPos)
    }

    fun addSprite(sprite: Sprite): Entry? {
        val data = spriteMap.getOrPut(sprite) {
            val pos = gridAlignedPosition(sprite.size) ?: return null
            val image = ResourceLoader.loadImage(sprite.resource, true) ?: return null
            glCopyImageSubData(
                image.id, GL_TEXTURE_2D, 0, 0, 0, 0,
                texture.id, GL_TEXTURE_2D, pos.offset.x, pos.offset.y, 0, 0,
                sprite.size.x, sprite.size.y, 1
            )
            pos
        }
        return Entry(this, sprite, data.size, data.offset)
    }
}
