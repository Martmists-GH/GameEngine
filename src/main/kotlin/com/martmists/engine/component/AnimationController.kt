package com.martmists.engine.component

import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3
import com.martmists.engine.scene.GameObject
import com.martmists.engine.animation.Animation
import com.martmists.engine.animation.Interpolation
import com.martmists.engine.animation.Keyframe
import org.lwjgl.assimp.Assimp.aiAnimInterpolation_Linear
import org.lwjgl.assimp.Assimp.aiAnimInterpolation_Spherical_Linear

class AnimationController(gameObject: GameObject) : Component(gameObject) {
    private data class AnimationState(
        val animation: Animation,
        var progress: Float,
        val repeat: Boolean,
    )

    private var animationState: AnimationState? = null

    fun availableAnimations() = gameObject.getComponent<ModelRenderer>().model!!.model.animations
    fun playAnimation(animation: String, repeat: Boolean) {
        val anim = availableAnimations().firstOrNull { it.name == animation } ?: return
        animationState = AnimationState(anim, 0f, repeat)
    }

    override fun onUpdate(delta: Float) {
        val state = animationState ?: return

        val model = gameObject.getComponent<ModelRenderer>().model ?: return

        state.progress += delta * state.animation.ticksPerSecond
        if (state.progress >= state.animation.duration) {
            if (state.repeat) {
                state.progress %= state.animation.duration
            } else {
                model.root.forEachRecursively {
                    it.transform = it.partRef.transform
                }
                animationState = null
                return
            }
        }

        for (channel in state.animation.channels) {
            val part = model.getPartByName(channel.nodeName) ?: continue
            val translation = interpolateKeyframes(channel.translationKeyframes, state.progress, state.repeat, Vec3.Zero)
            val rotation = interpolateKeyframes(channel.rotationKeyframes, state.progress, state.repeat, Quat.Identity)
            val scale = interpolateKeyframes(channel.scaleKeyframes, state.progress, state.repeat, Vec3.One)

            part.transform = Mat4x4.Identity
                .translate(translation)
                .rotate(rotation)
                .scale(scale)
        }
    }

    private fun <T> interpolateKeyframes(
        keyframes: Array<Keyframe<T>>,
        time: Float,
        repeat: Boolean,
        default: T,
    ): T {
        if (keyframes.isEmpty()) return default
        if (keyframes.size == 1) return keyframes[0].value

        val current = keyframes.lastOrNull { it.time <= time }
        val next = keyframes.firstOrNull { it.time > time }
            ?: if (repeat) keyframes.first() else null

        if (current == null) return next?.value ?: keyframes.first().value
        if (next == null || current == next) return current.value

        val frameTime = if (next.time < current.time) {
            (animationState!!.animation.duration - current.time) + next.time
        } else {
            next.time - current.time
        }

        val elapsed = if (time < current.time) {
            (animationState!!.animation.duration - current.time) + time
        } else {
            time - current.time
        }

        val factor = (elapsed / frameTime).coerceIn(0f, 1f)
        val interp = when (default) {
            is Vec3 -> when (current.interpolation) {
                aiAnimInterpolation_Linear -> Interpolation.LINEAR_VEC3
                else -> error("Unknown interpolation ${current.interpolation} for vec3")
            }
            is Quat -> when (current.interpolation) {
                aiAnimInterpolation_Linear -> Interpolation.LINEAR_QUAT
                aiAnimInterpolation_Spherical_Linear -> Interpolation.SPHERICAL_LINEAR_QUAT
                else -> error("Unknown interpolation ${current.interpolation} for quat")
            }
            else -> error("Unknown type to interpolate keyframes for: ${current::class.simpleName}")
        } as Interpolation<T>
        return interp.interpolate(current.value, next.value, factor)
    }

    override fun copyFor(other: GameObject) = AnimationController(other).also {
        it.animationState = animationState?.copy()
    }
}
