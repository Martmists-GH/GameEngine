package com.martmists.engine.component

import com.martmists.engine.scene.GameObject
import com.martmists.engine.sprite.Sprite

class SpriteRenderer(gameObject: GameObject) : Component(gameObject) {
    var sprite: Sprite? = null
    var frame: Int = 0

    override fun copyFor(other: GameObject) = SpriteRenderer(other).also {
        it.sprite = sprite
        it.frame = frame
    }
}
