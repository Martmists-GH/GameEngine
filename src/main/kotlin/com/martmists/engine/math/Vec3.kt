package com.martmists.engine.math

import org.joml.*

// Value class over JOML for immutability
@JvmInline
value class Vec3(val joml: Vector3fc) {
    constructor(value: Float) : this(Vector3f(value))
    constructor(x: Float, y: Float, z: Float) : this(Vector3f(x, y, z))

    val x: Float
        get() = joml.x()
    val y: Float
        get() = joml.y()
    val z: Float
        get() = joml.z()
    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    val length: Float
        get() = joml.length()

    val absolute: Vec3
        get() = Vec3(joml.absolute(Vector3f()))

    operator fun plus(other: Vec3) = Vec3(joml + other.joml)
    operator fun minus(other: Vec3) = Vec3(joml - other.joml)
    operator fun times(other: Float) = Vec3(joml * other)
    operator fun times(other: Vec3) = Vec3(joml * other.joml)
    operator fun times(other: Quat) = Vec3(joml.rotate(other.joml, Vector3f()))
    operator fun div(other: Float) = Vec3(joml / other)
    operator fun div(other: Vec3) = Vec3(joml / other.joml)
    operator fun unaryMinus() = Vec3(-joml)

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z) = Vec3(x, y, z)
    fun normalized() = Vec3(joml.normalize(Vector3f()))

    infix fun dot(other: Vec3) = joml dot other.joml
    infix fun cross(other: Vec3) = Vec3(joml cross other.joml)
    infix fun distance(other: Vec3) = joml distance other.joml
    infix fun angle(other: Vec3) = joml angle other.joml

    override fun toString() = "Vec3($x, $y, $z)"

    companion object {
        val Zero = Vec3(0f)
        val One = Vec3(1f)
    }
}
