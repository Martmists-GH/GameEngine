package com.martmists.engine.util

import java.io.File

@JvmInline
value class Resource(val path: String) {
    val source: String
        get() = path.substringBefore(':', "")

    val pathComponent: String
        get() = path.substringAfter(':')

    val absolutePath: String
        get() = when (source) {
            "res" -> path
            "" -> File(path).absolutePath
            else -> error("Unknown resource type: $source")
        }

    fun exists(): Boolean {
        return when (source) {
            "res" -> Thread.currentThread().contextClassLoader.getResource(pathComponent) != null
            "" -> File(path).exists()
            else -> error("Unknown resource type: $source")
        }
    }

    fun readAllBytes(): ByteArray {
        return when (source) {
            "res" -> Thread.currentThread().contextClassLoader.getResourceAsStream(pathComponent)?.readAllBytes() ?: error("Resource does not exist: '$path'")
            "" -> File(path).readBytes()
            else -> error("Unknown resource type: $source")
        }
    }

    val parentResource: Resource
        get() = when (source) {
            "res" -> Resource(path.substringBeforeLast('/'))
            "" -> Resource(File(path).parentFile.path)
            else -> error("Unknown resource type: $source")
        }

    fun resolve(childPath: String): Resource {
        return when (source) {
            "res" -> {
                var resPath = if (path.endsWith('/')) path.substring(0, path.length - 1) else path
                val parts = childPath.split('/').toMutableList()

                while (parts.isNotEmpty()) {
                    when (val chunk = parts.removeFirst()) {
                        "." -> { }
                        ".." -> {
                            resPath = resPath.substringBeforeLast('/')
                        }
                        else -> {
                            resPath = "$resPath/$chunk"
                        }
                    }
                }

                Resource(resPath)
            }
            "" -> Resource(File(path).resolve(childPath).path)
            else -> error("Unknown resource type: $source")
        }
    }
}
