package com.martmists.engine.math

import org.joml.*

// Value class over JOML for immutability
@JvmInline
value class Color(val joml: Vector4fc) {
    constructor(r: Float, g: Float, b: Float) : this(Vector4f(r, g, b, 1f))
    constructor(r: Float, g: Float, b: Float, a: Float) : this(Vector4f(r, g, b, a))

    val r: Float
        get() = joml.x()
    val g: Float
        get() = joml.y()
    val b: Float
        get() = joml.z()
    val a: Float
        get() = joml.w()

    operator fun component1() = r
    operator fun component2() = g
    operator fun component3() = b
    operator fun component4() = a

    override fun toString() = "Color($r, $g, $b, $a)"

    companion object {
        val White = Color(1f, 1f, 1f)
        val Black = Color(0f, 0f, 0f)

        fun fromHSV(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
            val c = v * s
            val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
            val m = v - c
            val (r, g, b) = when {
                h < 60 -> Triple(c, x, 0f)
                h < 120 -> Triple(x, c, 0f)
                h < 180 -> Triple(0f, c, x)
                h < 240 -> Triple(0f, x, c)
                h < 300 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return Color(r + m, g + m, b + m, alpha)
        }
    }
}
