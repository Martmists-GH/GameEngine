package com.martmists.engine.model

import com.martmists.engine.math.Mat4x4
import com.martmists.engine.animation.Animation
import com.martmists.engine.render.Shader
import com.martmists.engine.util.Resource
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext

class Model(
    val resource: Resource,
    val root: ModelPart,
    val animations: Array<Animation>,
    val boneOffsets: Map<String, Mat4x4>,
    private val shaderFactory: () -> Shader,
)  {
    private val shaderMap = mutableMapOf<Long, Shader>()

    fun makeShader() = shaderMap.getOrPut(glfwGetCurrentContext(), shaderFactory)
    fun instantiate() = ModelInstance(this, root.instantiate())

    override fun toString(): String {
        return "Model(resource='$resource', root=$root, bones=${boneOffsets.size})"
    }
}

class ModelPart(
    val name: String,
    val transform: Mat4x4,
    val geometries: Array<Geometry>,
    val children: Array<ModelPart>,
) {
    internal fun instantiate(): ModelPartInstance = ModelPartInstance(this, transform, Array(children.size) { children[it].instantiate() })

    override fun toString(): String {
        return "ModelPart(name='$name', transform=$transform)"
    }
}
