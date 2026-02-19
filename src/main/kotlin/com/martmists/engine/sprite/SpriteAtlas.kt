package com.martmists.engine.sprite

import com.martmists.engine.math.Color
import com.martmists.engine.math.Vec2
import com.martmists.engine.math.Vec2i
import com.martmists.engine.render.TextureHandle
import com.martmists.engine.util.ResourceLoader
import org.lwjgl.opengl.GL46.*

class SpriteAtlas(
    val size: Vec2i = Vec2i(2048, 2048),
) {
    internal class PositionInfo(
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

    internal val spritesheetMap = mutableMapOf<Spritesheet, PositionInfo>()
    internal val spriteMap = mutableMapOf<Sprite, Entry>()
    internal val texture = TextureHandle(GL_NEAREST, GL_NEAREST).apply {
        setEmpty(size.x, size.y, Color.Transparent)
    }

    data class Entry(
        val atlas: SpriteAtlas,
        val sprite: Sprite,
        val size: Vec2i,
        val offset: Vec2i,
    ) {
        fun uvOffset(): Vec2 {
            return Vec2(
                (offset.x + sprite.offset.x).toFloat() / atlas.size.x,
                (offset.y + sprite.offset.y).toFloat() / atlas.size.y,
            )
        }
        fun uvSize(): Vec2 {
            return Vec2(
                size.x.toFloat() / atlas.size.x,
                size.y.toFloat() / atlas.size.y,
            )
        }
        fun slice(xRange: Vec2i, yRange: Vec2i): Entry {
            return Entry(
                atlas,
                sprite,
                Vec2i(xRange.y - xRange.x, yRange.y - yRange.x),
                offset + Vec2i(xRange.x, yRange.x)
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
        val validPos = gridPositions(aligned).firstOrNull { offset -> spritesheetMap.values.none {
            it.overlaps(offset, aligned)
        } } ?: return null
        return PositionInfo(size, validPos)
    }

    fun addSprite(sprite: Sprite): Entry? {
         return spriteMap.getOrPut(sprite) {
            val sheetPos = spritesheetMap.getOrPut(sprite.spritesheet) {
                val pos = gridAlignedPosition(sprite.spritesheet.size) ?: return null
                val image = ResourceLoader.loadImage(sprite.spritesheet.resource, true) ?: return null
                glCopyImageSubData(
                    image.id, GL_TEXTURE_2D, 0, 0, 0, 0,
                    texture.id, GL_TEXTURE_2D, pos.offset.x, pos.offset.y, 0, 0,
                    sprite.size.x, sprite.size.y, 1
                )
                pos
            }

            Entry(this, sprite, sprite.size, sheetPos.offset + sprite.offset)
        }
    }
}
