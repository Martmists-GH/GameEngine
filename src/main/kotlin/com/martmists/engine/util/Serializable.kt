package com.martmists.engine.util

import java.nio.ByteBuffer

/**
 * Utility interface for serialization.
 * Mainly used to (de-)serialize prefabs.
 */
interface Serializable {
    fun serialize(): ByteArray
    fun deserialize(buffer: ByteBuffer)
}
