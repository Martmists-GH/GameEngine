package com.martmists.engine.ext

import com.martmists.engine.math.Color
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec2
import com.martmists.engine.math.Vec2i
import com.martmists.engine.math.Vec3
import com.martmists.engine.util.Resource
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ByteBuffer.putVec2(value: Vec2) {
    putFloat(value.x)
    putFloat(value.y)
}

fun ByteBuffer.putVec2i(value: Vec2i) {
    putInt(value.x)
    putInt(value.y)
}

fun ByteBuffer.putVec3(value: Vec3) {
    putFloat(value.x)
    putFloat(value.y)
    putFloat(value.z)
}

fun ByteBuffer.putQuat(value: Quat) {
    putFloat(value.x)
    putFloat(value.y)
    putFloat(value.z)
    putFloat(value.w)
}

fun ByteBuffer.putColor(value: Color) {
    putFloat(value.r)
    putFloat(value.g)
    putFloat(value.b)
    putFloat(value.a)
}


fun ByteBuffer.putString(value: String) {
    val bytes = value.encodeToByteArray()
    putInt(bytes.size)
    put(bytes)
}

fun ByteBuffer.putResource(resource: Resource) {
    putString(resource.path)
}

fun ByteBuffer.getVec2(): Vec2 {
    return Vec2(getFloat(), getFloat())
}

fun ByteBuffer.getVec2i(): Vec2i {
    return Vec2i(getInt(), getInt())
}

fun ByteBuffer.getVec3(): Vec3 {
    return Vec3(getFloat(), getFloat(), getFloat())
}

fun ByteBuffer.getQuat(): Quat {
    return Quat(getFloat(), getFloat(), getFloat(), getFloat())
}

fun ByteBuffer.getColor(): Color {
    return Color(getFloat(), getFloat(), getFloat(), getFloat())
}

fun ByteBuffer.getString(): String {
    val arr = ByteArray(getInt())
    get(arr)
    return arr.decodeToString()
}

fun ByteBuffer.getResource(): Resource {
    return Resource(getString())
}

fun buildBuffer(size: Int, block: ByteBuffer.() -> Unit): ByteArray {
    val array = ByteArray(size)
    val buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)
    buffer.block()
    return array
}
