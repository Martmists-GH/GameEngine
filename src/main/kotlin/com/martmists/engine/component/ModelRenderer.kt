package com.martmists.engine.component

import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.ext.getResource
import com.martmists.engine.ext.putResource
import com.martmists.engine.model.ModelInstance
import com.martmists.engine.scene.GameObject
import com.martmists.engine.util.ResourceLoader
import java.nio.ByteBuffer

/**
 * Model renderer for [ModelInstance][com.martmists.engine.model.ModelInstance] objects.
 */
class ModelRenderer(gameObject: GameObject) : Component(gameObject) {
    var model: ModelInstance? = null

    override fun copyFor(other: GameObject) = ModelRenderer(other).also {
        it.model = model?.model?.instantiate()
    }

    override fun serialize(): ByteArray {
        val m = model
        if (m != null) {
            return buildBuffer(4 + m.model.resource.path.length) {
                putResource(m.model.resource)
            }
        } else {
            return buildBuffer(4) {
                putInt(0)
            }
        }
    }

    override fun deserialize(buffer: ByteBuffer) {
        val here = buffer.position()
        if (buffer.getInt() != 0) {
            buffer.position(here)
            val modelRes = buffer.getResource()
            model = ResourceLoader.loadModel(modelRes).instantiate()
        }
    }
}
