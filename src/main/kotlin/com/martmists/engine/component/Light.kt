package com.martmists.engine.component

import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.ext.getColor
import com.martmists.engine.ext.putColor
import com.martmists.engine.math.Color
import com.martmists.engine.scene.GameObject
import java.nio.ByteBuffer

/**
 * Base class for lights.
 */
abstract class Light(gameObject: GameObject) : Component(gameObject) {
    var color = Color.White
    var intensity = 1.0f

    override fun serialize(): ByteArray {
        return buildBuffer(20) {
            putColor(color)
            putFloat(intensity)
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        color = buffer.getColor()
        intensity = buffer.getFloat()
    }
}

/**
 * Global directional light.
 *
 * The [GameObject]'s rotation is used for direction, and the translation is exclusively used for wireframe rendering.
 */
class DirectionalLight(gameObject: GameObject) : Light(gameObject) {
    override fun copyFor(other: GameObject) = DirectionalLight(other).also {
        it.color = color
        it.intensity = intensity
    }
}

/**
 * Point light.
 */
class PointLight(gameObject: GameObject) : Light(gameObject) {
    var range = 10.0f

    override fun copyFor(other: GameObject) = PointLight(other).also {
        it.color = color
        it.intensity = intensity
        it.range = range
    }

    override fun serialize(): ByteArray {
        return buildBuffer(24) {
            putColor(color)
            putFloat(intensity)
            putFloat(range)
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        color = buffer.getColor()
        intensity = buffer.getFloat()
        range = buffer.getFloat()
    }
}

/**
 * Spot light.
 */
class SpotLight(gameObject: GameObject) : Light(gameObject) {
    var range = 10.0f
    var angle = 45.0f

    override fun copyFor(other: GameObject) = SpotLight(other).also {
        it.color = color
        it.intensity = intensity
        it.range = range
        it.angle = angle
    }

    override fun serialize(): ByteArray {
        return buildBuffer(28) {
            putColor(color)
            putFloat(intensity)
            putFloat(range)
            putFloat(angle)
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        color = buffer.getColor()
        intensity = buffer.getFloat()
        range = buffer.getFloat()
        angle = buffer.getFloat()
    }
}
