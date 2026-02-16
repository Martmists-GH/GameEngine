package com.martmists.engine.component

import com.martmists.engine.scene.GameObject
import com.martmists.engine.scene.Transform
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3

class Camera(gameObject: GameObject) : Component(gameObject) {
    var viewMatrix = Mat4x4.Identity
        private set
    var projectionMatrix = Mat4x4.Identity
        private set

    var fov = 70f
    var aspectRatio = 16f/9f
    var near = 0.01f
    var far = 1000f

    override fun init() {
        projectionMatrix = Mat4x4.Identity.perspective(Math.toRadians(fov.toDouble()).toFloat(), aspectRatio, near, far)
    }

    override fun onUpdate(delta: Float) {
        viewMatrix = Mat4x4.Identity
            .rotate(gameObject.transform.worldRotation)
            .translate(-gameObject.transform.worldTranslation)
    }

    fun lookAt(other: GameObject) = lookAt(other.transform)
    fun lookAt(other: Transform) = lookAt(other.worldTranslation)
    fun lookAt(other: Vec3) {
        val delta = gameObject.transform.worldTranslation - other
        gameObject.transform.worldRotation = Quat.Identity.lookAt(delta)
    }

    override fun copyFor(other: GameObject) = Camera(other)

    // TODO: Serialization
}
