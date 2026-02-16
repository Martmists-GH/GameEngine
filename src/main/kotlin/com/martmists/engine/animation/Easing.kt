package com.martmists.engine.animation

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun interface Easing {
    fun ease(value: Float): Float

    companion object {
        val LINEAR = Easing { it }
        // Implementations based on https://easings.net/
        val IN_SINE = Easing { 1 - cos(PI * it / 2).toFloat() }
        val OUT_SINE = Easing { sin(PI * it / 2).toFloat() }
        val IN_OUT_SINE = Easing { -(cos(PI * it).toFloat() - 1) / 2 }
        val IN_QUAD = Easing { it * it }
        val OUT_QUAD = Easing { 1 - (1 - it) * (1 - it) }
        val IN_OUT_QUAD = Easing {
            if (it < 0.5) 2 * it * it
            else 1 - (-2 * it + 2).pow(2) / 2
        }
        val IN_CUBIC = Easing { it * it * it }
        val OUT_CUBIC = Easing { 1 - (1 - it).pow(3) }
        val IN_OUT_CUBIC = Easing {
            if (it < 0.5) 4 * it * it * it
            else 1 - (-2 * it + 2).pow(3) / 2
        }
        val IN_EXPO = Easing {
            if (it == 0f) 0f
            else 2f.pow(10 * it - 10)
        }
        val OUT_EXPO = Easing {
            if (it == 1f) 1f
            else 1 - 2f.pow(-10 * it)
        }
        val IN_OUT_EXPO = Easing {
            when {
                it == 0f -> 0f
                it == 1f -> 1f
                it < 0.5f -> 2f.pow(20 * it - 10) / 2
                else -> (2 - 2f.pow(-20 * it + 10)) / 2
            }
        }
        val IN_CIRC = Easing { 1 - sqrt(1 - it.pow(2)) }
        val OUT_CIRC = Easing { sqrt(1 - (it - 1).pow(2)) }
        val IN_OUT_CIRC = Easing {
            if (it < 0.5) (1 - sqrt(1 - (2 * it).pow(2))) / 2
            else (sqrt(1 - (-2 * it + 2).pow(2)) + 1) / 2
        }
        val IN_BACK = Easing {
            val c1 = 1.70158f
            val c3 = c1 + 1
            c3 * it.pow(3) - c1 * it.pow(2)
        }
        val OUT_BACK = Easing {
            val c1 = 1.70158f
            val c3 = c1 + 1
            1 + c3 * (it - 1).pow(3) + c1 * (it - 1).pow(2)
        }
        val IN_OUT_BACK = Easing {
            val c1 = 1.70158f
            val c2 = c1 * 1.525f

            if (it < 0.5) ((2 * it).pow(2) * ((c2 + 1) * 2 * it - c2)) / 2
            else ((2 * it - 2).pow(2) * ((c2 + 1) * (it * 2 - 2) + c2) + 2) / 2
        }
        val IN_ELASTIC = Easing {
            val c4 = (2 * PI).toFloat() / 3
            when (it) {
                0f -> 0f
                1f -> 1f
                else -> -(2f.pow(10 * it - 10)) * sin((it * 10 - 10.75f) * c4)
            }
        }
        val OUT_ELASTIC = Easing {
            val c4 = (2 * PI).toFloat() / 3
            when (it) {
                0f -> 0f
                1f -> 1f
                else -> 2f.pow(-10 * it) * sin((it * 10 - 0.75f) * c4) + 1
            }
        }
        val IN_OUT_ELASTIC = Easing {
            val c5 = (2 * PI).toFloat() / 4.5f
            when {
                it == 0f -> 0f
                it == 1f -> 1f
                it < 0.5f -> -(2f.pow(20 * it - 10) * sin((20 * it - 11.125f) * c5)) / 2
                else -> (2f.pow(-20 * it + 10) * sin((20 * it - 11.125f) * c5)) / 2 + 1
            }
        }
        val IN_BOUNCE = Easing {
            1 - OUT_BOUNCE.ease(it)
        }
        val OUT_BOUNCE = Easing {
            val n1 = 7.5625f
            val d1 = 2.75f
            when {
                it < 1 / d1 -> n1 * it * it
                it < 2 / d1 -> n1 * (it - 1.5f / d1).pow(2) + 0.75f
                it < 2.5 / d1 -> n1 * (it - 2.25f / d1).pow(2) + 0.9375f
                else -> n1 * (it - 2.625f / d1).pow(2) + 0.984375f
            }
        }
        val IN_OUT_BOUNCE = Easing {
            if (it < 0.5f) (1 - OUT_BOUNCE.ease(1 - 2 * it)) / 2
            else (1 + OUT_BOUNCE.ease(2 * it - 1)) / 2
        }
    }
}
