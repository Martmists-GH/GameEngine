package com.martmists.engine.math

import org.joml.*

@JvmInline
value class Mat4x4(val joml: Matrix4fc) {
    operator fun times(other: Mat4x4) = Mat4x4(joml.mul(other.joml, Matrix4f()))
    fun scale(other: Vec3) = Mat4x4(joml.scale(other.joml, Matrix4f()))
    fun translate(other: Vec3) = Mat4x4(joml.translate(other.joml, Matrix4f()))
    fun rotate(other: Quat) = Mat4x4(joml.rotate(other.joml, Matrix4f()))
    fun perspective(fovy: Float, aspectRatio: Float, nearPlane: Float, farPlane: Float) = Mat4x4(joml.perspective(fovy, aspectRatio, nearPlane, farPlane, Matrix4f()))
    fun ortho(left: Float, right: Float, bottom: Float, top: Float, nearPlane: Float, farPlane: Float) = Mat4x4(joml.ortho(left, right, bottom, top, nearPlane, farPlane, Matrix4f()))
    fun invert() = Mat4x4(joml.invert(Matrix4f()))

    companion object {
        val Zero = Mat4x4(Matrix4f().zero())
        val Identity = Mat4x4(Matrix4f().identity())
        fun makeSRT(scale: Vec3, rotation: Quat, translation: Vec3): Mat4x4 {
            return Identity.translate(translation).rotate(rotation).scale(scale)
        }
    }
}
