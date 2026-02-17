package com.martmists.engine.sprite

import com.martmists.engine.math.Vec2i
import com.martmists.engine.util.Resource

class Sprite(
    val resource: Resource,
    val size: Vec2i,
    val numFrames: Int,
    val offset9Slice: Array<Vec2i> = emptyArray(),
) {
    val is9Slice: Boolean
        get() = offset9Slice.isNotEmpty()
}
