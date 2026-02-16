package com.martmists.engine.animation

import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3
import org.joml.Quaternionf

fun interface Interpolation<T> {
    fun interpolate(start: T, end: T, value: Float): T

    companion object {
        val LINEAR_VEC3 = Interpolation<Vec3> { start, end, value ->
            start + (end - start) * value
        }
        val LINEAR_QUAT = Interpolation<Quat> { start, end, value ->
            Quat(start.joml.nlerp(end.joml, value, Quaternionf()))
        }
        val SPHERICAL_LINEAR_QUAT = Interpolation<Quat> { start, end, value ->
            Quat(start.joml.slerp(end.joml, value, Quaternionf()))
        }
    }
}

