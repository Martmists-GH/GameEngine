package com.martmists.engine.math

import org.joml.*

// Value class over JOML for immutability
@JvmInline
value class Quat(val joml: Quaternionfc) {
    constructor(x: Float, y: Float, z: Float, w: Float) : this(Quaternionf(x, y, z, w))

    val x: Float
        get() = joml.x()
    val y: Float
        get() = joml.y()
    val z: Float
        get() = joml.z()
    val w: Float
        get() = joml.w()

    val angle: Float
        get() = joml.angle()
    val conjugate: Quat
        get() = Quat(joml.conjugate(Quaternionf()))

    operator fun plus(other: Quat) = Quat(joml + other.joml)
    operator fun minus(other: Quat) = Quat(joml - other.joml)
    operator fun times(other: Float) = Quat(joml * other)
    operator fun times(other: Quat) = Quat(joml * other.joml)
    operator fun div(other: Float) = Quat(joml / other)
    operator fun div(other: Quat) = Quat(joml / other.joml)
    infix fun difference(other: Quat) = Quat(joml difference other.joml)
    infix fun premul(other: Quat) = Quat(joml.premul(other.joml, Quaternionf()))
    fun lookAt(direction: Vec3) = lookAlong(direction, Vec3(0f, 1f, 0f))
    fun lookAlong(direction: Vec3, up: Vec3) = Quat(joml.lookAlong(direction.joml, up.joml, Quaternionf()))
    fun toEuler() = Vec3(joml.getEulerAnglesXYZ(Vector3f()))
    fun rotate(axes: Vec3) = Quat(Quaternionf(joml).rotateX(axes.x).rotateY(axes.y).rotateZ(axes.z))

    override fun toString() = "Quat($x, $y, $z, $w)"

    companion object {
        val Identity = Quat(Quaternionf().identity())

        fun fromAxisAngle(axis: Vec3, angle: Float): Quat = Quat(Quaternionf().fromAxisAngleRad(axis.joml, angle))
    }
}
