package com.martmists.engine.math

import org.joml.*

// Value class over JOML for immutability
@JvmInline
value class Vec2i(val joml: Vector2ic) {
    constructor(value: Int) : this(Vector2i(value))
    constructor(x: Int, y: Int) : this(Vector2i(x, y))

    val x: Int
        get() = joml.x()
    val y: Int
        get() = joml.y()
    operator fun component1() = x
    operator fun component2() = y

    val length: Float
        get() = joml.length().toFloat()

    val absolute: Vec2i
        get() = Vec2i(joml.absolute(Vector2i()))

    operator fun plus(other: Vec2i) = Vec2i(joml + other.joml)
    operator fun minus(other: Vec2i) = Vec2i(joml - other.joml)
    operator fun times(other: Int) = Vec2i(joml * other)
    operator fun times(other: Vec2i) = Vec2i(joml * other.joml)
    operator fun div(other: Int) = Vec2i(joml / other)
    operator fun div(other: Float) = Vec2i(joml / other)
    operator fun unaryMinus() = Vec2i(-joml)
    infix fun distance(other: Vec2i) = joml distance other.joml

    override fun toString() = "Vec2i($x, $y)"

    companion object {
        val Zero = Vec2i(0)
    }
}
