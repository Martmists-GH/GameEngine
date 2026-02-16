package com.martmists.engine.model

import com.martmists.engine.math.Mat4x4

class ModelInstance(
    val model: Model,
    val root: ModelPartInstance,
) {
    internal fun getPartByName(name: String) = root.firstRecursively { it.partRef.name == name }

    fun calculateBoneMatrices(rootTransform: Mat4x4 = Mat4x4.Identity): Array<Mat4x4> {
        val boneNames = model.boneOffsets.keys
        val matrices = Array(boneNames.size) { Mat4x4.Identity }
        val nodeTransforms = mutableMapOf<String, Mat4x4>()

        fun computeTransform(part: ModelPartInstance, parentTransform: Mat4x4) {
            val globalTransform = parentTransform * part.transform
            nodeTransforms[part.partRef.name] = globalTransform
            for (child in part.children) {
                computeTransform(child, globalTransform)
            }
        }

        computeTransform(root, rootTransform)

        for ((i, name) in boneNames.withIndex()) {
            val nodeTransform = nodeTransforms[name] ?: Mat4x4.Identity
            val offset = model.boneOffsets[name] ?: Mat4x4.Identity
            matrices[i] = nodeTransform * offset
        }

        return matrices
    }
}

class ModelPartInstance(
    val partRef: ModelPart,
    var transform: Mat4x4,
    val children: Array<ModelPartInstance>,
) {
    internal fun firstRecursively(filter: (ModelPartInstance) -> Boolean): ModelPartInstance? {
        if (filter(this)) return this
        for (c in children) {
            val res = c.firstRecursively(filter)
            if (res != null) return res
        }
        return null
    }

    internal fun filterRecursively(filter: (ModelPartInstance) -> Boolean): List<ModelPartInstance> {
        val out = mutableListOf<ModelPartInstance>()
        if (filter(this)) out.add(this)
        out.addAll(children.flatMap { it.filterRecursively(filter) })
        return out
    }

    internal fun <R> mapRecursively(map: (ModelPartInstance) -> R): List<R> {
        val out = mutableListOf<R>()
        out.add(map(this))
        out.addAll(children.flatMap { it.mapRecursively(map) })
        return out
    }

    internal fun forEachRecursively(action: (ModelPartInstance) -> Unit) {
        action(this)
        children.forEach { it.forEachRecursively(action) }
    }
}
