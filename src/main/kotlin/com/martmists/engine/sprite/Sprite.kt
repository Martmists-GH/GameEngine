package com.martmists.engine.sprite

import com.martmists.engine.math.Vec2i

class Sprite(
    val spritesheet: Spritesheet,
    val size: Vec2i,
    val offset: Vec2i,
    val offset9SliceCorners: Pair<Vec2i, Vec2i>? = null,
) {
    val is9Slice: Boolean
        get() = offset9SliceCorners != null
    val aspectRatio: Float
        get() = size.x.toFloat() / size.y
}
