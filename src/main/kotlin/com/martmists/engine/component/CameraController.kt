package com.martmists.engine.component

import com.martmists.engine.scene.GameObject
import com.martmists.engine.Input
import com.martmists.engine.math.Vec3
import org.lwjgl.glfw.GLFW.*

class CameraController(gameObject: GameObject) : Component(gameObject) {
    var speed = 5f

    override fun onUpdate(delta: Float) {
        var dx = 0f
        var dy = 0f
        var dz = 0f

        if (Input.isAnyKeyDown(GLFW_KEY_W)) dz -= 1f
        if (Input.isAnyKeyDown(GLFW_KEY_S)) dz += 1f
        if (Input.isAnyKeyDown(GLFW_KEY_A)) dx -= 1f
        if (Input.isAnyKeyDown(GLFW_KEY_D)) dx += 1f
        if (Input.isAnyKeyDown(GLFW_KEY_SPACE)) dy += 1f
        if (Input.isAnyKeyDown(GLFW_KEY_LEFT_SHIFT)) dy -= 1f

        if (dx != 0f || dy != 0f || dz != 0f) {
            val rot = gameObject.transform.worldRotation.conjugate
            val right = (Vec3(1f, 0f, 0f) * rot).copy(y = 0f).normalized()
            val forward = Vec3(0f, 1f, 0f) cross right

            var horizontalDir = (forward * -dz) + (right * dx)
            if (horizontalDir.length > 0f) {
                horizontalDir = horizontalDir.normalized()
            }
            val dir = horizontalDir + Vec3(0f, dy, 0f)
            gameObject.transform.translation += dir * speed * delta
        }
    }

    override fun copyFor(other: GameObject) = CameraController(other).also {
        it.speed = speed
    }

    // TODO: Serialization
}
