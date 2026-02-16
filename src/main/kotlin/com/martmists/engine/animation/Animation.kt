package com.martmists.engine.animation

import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3

class Animation(
    val name: String,
    val duration: Float,
    val ticksPerSecond: Float,
    val channels: Array<AnimationChannel>
) {
    override fun toString(): String {
        return "Animation(name='$name', duration=$duration, ticksPerSecond=$ticksPerSecond, channels=${channels.size})"
    }
}

data class Keyframe<T>(
    val time: Float,
    val value: T,
    val interpolation: Int,
)

class AnimationChannel(
    val nodeName: String,
    val translationKeyframes: Array<Keyframe<Vec3>>,
    val rotationKeyframes: Array<Keyframe<Quat>>,
    val scaleKeyframes: Array<Keyframe<Vec3>>,
) {
    override fun toString(): String {
        return "AnimationChannel(nodeName='$nodeName')"
    }
}
