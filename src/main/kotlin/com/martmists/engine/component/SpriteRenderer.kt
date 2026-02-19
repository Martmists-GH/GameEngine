package com.martmists.engine.component

import com.martmists.engine.math.Vec2
import com.martmists.engine.scene.GameObject
import com.martmists.engine.sprite.Sprite

class SpriteRenderer(gameObject: GameObject) : Component(gameObject) {
    var sprite: Sprite? = null
    // If sprite is 9-spliced and greater than 1, this will stretch only the correct slices
    // Otherwise, it will stretch/shrink the texture
    var stretch: Vec2 = Vec2.One

    override fun copyFor(other: GameObject) = SpriteRenderer(other).also {
        it.sprite = sprite
    }
}
