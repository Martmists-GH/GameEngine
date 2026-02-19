package com.martmists.engine.animation

import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3

/**
 * Container class for animations.
 *
 * @property name The name of the animation.
 * @property duration The duration of the animation in ticks.
 * @property ticksPerSecond How many ticks per second the animation uses.
 * @property channels The channels in the animation.
 */
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

/**
 * A single keyframe in a channel
 *
 * @param T The type of data this keyframe uses.
 * @property time Which tick this keyframe starts at.
 * @property value The value of the channel at this time.
 * @property interpolation Which interpolation to use between keyframes. This should be an aiAnimInterpolation.
 */
data class Keyframe<T>(
    val time: Float,
    val value: T,
    val interpolation: Int,
)

/**
 * An animation channel
 *
 * @property nodeName The name of the node in the model this applies to.
 * @property translationKeyframes The keyframes applying translation.
 * @property rotationKeyframes The keyframes applying rotation.
 * @property scaleKeyframes The keyframes applying scaling.
 */
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
