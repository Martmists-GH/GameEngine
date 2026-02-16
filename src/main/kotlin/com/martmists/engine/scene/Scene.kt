package com.martmists.engine.scene

import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.util.Serializable
import java.nio.ByteBuffer

class Scene : Serializable {
    val objects: List<GameObject>
        field = mutableListOf()

    fun addObject(go: GameObject) {
        objects.add(go)
    }

    override fun serialize(): ByteArray {
        val objectCount = objects.size
        val childBuffers = objects.map(Serializable::serialize)
        return buildBuffer(4 + childBuffers.sumOf { it.size }) {
            putInt(objectCount)
            for (child in childBuffers) {
                put(child)
            }
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        val objectCount = buffer.getInt()
        repeat(objectCount) {
            addObject(GameObject().also { it.deserialize(buffer) })
        }
    }

    companion object {
        val Empty = Scene()
    }
}
