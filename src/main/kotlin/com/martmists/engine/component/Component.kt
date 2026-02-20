package com.martmists.engine.component

import com.martmists.engine.scene.GameObject
import com.martmists.engine.scene.Transform
import com.martmists.engine.util.Serializable
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

/**
 * Base Component class.
 *
 * Every component is expected to have a 1-arg GameObject constructor for (de-)serialization.
 */
abstract class Component(val gameObject: GameObject) : Serializable {
    val transform: Transform
        get() = gameObject.transform

    /**
     * Invoked after attached to an object and configured
     */
    open fun init() {}

    /**
     * early update step
     */
    open fun preUpdate(delta: Float) {}

    /**
     * update step
     */
    open fun onUpdate(delta: Float) {}

    /**
     * late update step
     */
    open fun postUpdate(delta: Float) {}

    /**
     * Invoked once the object goes out of scope
     */
    open fun cleanup() {}

    /**
     * Must implement constructing a copy of `this` for the new object.
     */
    abstract fun copyFor(other: GameObject): Component

    // Default serialization: do nothing
    open val isSerializable = true
    override fun serialize(): ByteArray { return byteArrayOf() }
    override fun deserialize(buffer: ByteBuffer) { }
}
