package com.martmists.engine.scene

import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.ext.getQuat
import com.martmists.engine.ext.getVec3
import com.martmists.engine.ext.putQuat
import com.martmists.engine.ext.putVec3
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec3
import com.martmists.engine.util.Serializable
import java.nio.ByteBuffer

class Transform(internal val gameObject: GameObject) : Serializable {
    var scale = Vec3.One
    var translation = Vec3.Zero
    var rotation = Quat.Identity

    var worldScale: Vec3
        get() = (parent?.worldScale ?: Vec3.One) * scale
        set(value) {
            scale = value / (parent?.worldScale ?: Vec3.One)
        }
    var worldTranslation: Vec3
        get() = (parent?.worldTranslation ?: Vec3.Zero) + translation
        set(value) {
            translation = value - (parent?.worldTranslation ?: Vec3.Zero)
        }
    var worldRotation: Quat
        get() = (parent?.worldRotation ?: Quat.Identity) premul rotation
        set(value) {
            rotation = value premul ((parent?.worldRotation ?: Quat.Identity).conjugate)
        }

    private var objectRefHolder: GameObject? = null
    var parent: Transform? = null

    val children: Set<Transform>
        field = mutableSetOf<Transform>()
    fun addChild(child: Transform) {
        child.parent?.removeChild(child)
        child.parent = this
        children.add(child)
    }
    fun removeChild(child: Transform) {
        if (child in children) {
            child.parent = null
            children.remove(child)
        }
    }
    fun addChild(child: GameObject) = addChild(child.transform)
    fun removeChild(child: GameObject) = removeChild(child.transform)

    // TODO: Figure out if it's faster to access `worldXYZ` attributes
    fun modelMatrix(base: Mat4x4 = Mat4x4.Identity): Mat4x4 = (parent?.modelMatrix(base) ?: base).translate(translation).rotate(rotation).scale(scale)

    fun copyFrom(other: Transform) {
        children.clear()
        scale = other.scale
        translation = other.translation
        rotation = other.rotation
        for (child in other.children) {
            addChild(child.objectRefHolder!!.clone())
        }
    }

    override fun serialize(): ByteArray {
        val children = children.map { it.gameObject.serialize() }
        return buildBuffer(44 + children.sumOf { it.size }) {
            putVec3(translation)
            putVec3(scale)
            putQuat(rotation)
            putInt(children.size)
            for (child in children) {
                put(child)
            }
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        translation = buffer.getVec3()
        scale = buffer.getVec3()
        rotation = buffer.getQuat()
        val objects = List(buffer.getInt()) {
            val obj = GameObject()
            obj.deserialize(buffer)
            obj
        }
        children.clear()
        for (obj in objects) {
            addChild(obj)
        }
    }
}
