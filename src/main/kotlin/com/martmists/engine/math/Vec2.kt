package com.martmists.engine.math

import org.joml.*

// Value class over JOML for immutability
@JvmInline
value class Vec2(val joml: Vector2fc) {
    constructor(value: Float) : this(Vector2f(value))
    constructor(x: Float, y: Float) : this(Vector2f(x, y))

    val x: Float
        get() = joml.x()
    val y: Float
        get() = joml.y()
    operator fun component1() = x
    operator fun component2() = y

    val length: Float
        get() = joml.length()

    val absolute: Vec2
        get() = Vec2(joml.absolute(Vector2f()))


    operator fun plus(other: Vec2) = Vec2(joml + other.joml)
    operator fun minus(other: Vec2) = Vec2(joml - other.joml)
    operator fun times(other: Float) = Vec2(joml * other)
    operator fun times(other: Vec2) = Vec2(joml * other.joml)
    operator fun div(other: Float) = Vec2(joml / other)
    operator fun div(other: Vec2) = Vec2(joml / other.joml)
    operator fun unaryMinus() = Vec2(-joml)
    infix fun dot(other: Vec2) = joml dot other.joml
    infix fun distance(other: Vec2) = joml distance other.joml
    infix fun angle(other: Vec2) = joml angle other.joml

    override fun toString() = "Vec2($x, $y)"

    companion object {
        val Zero = Vec2(0f)
    }
}
