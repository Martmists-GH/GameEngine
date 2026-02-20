package com.martmists.engine.component

import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.math.Vec2
import com.martmists.engine.scene.GameObject
import com.martmists.engine.sprite.Sprite
import java.nio.ByteBuffer

/**
 * Sprite renderer for [Sprite] objects.
 */
class SpriteRenderer(gameObject: GameObject) : Component(gameObject) {
    var sprite: Sprite? = null
    /**
     * How much to stretch the sprite.
     * In regular sprites this just stretches, but this is mainly useful for 9-sliced sprites.
     * In 9-sliced sprites, it stretches the relevant edges without distorting the other edges.
     */
    var stretch: Vec2 = Vec2.One

    override fun copyFor(other: GameObject) = SpriteRenderer(other).also {
        it.sprite = sprite
    }

    // TODO: Serialization
}
