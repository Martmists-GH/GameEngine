package com.martmists.engine.scene

import com.martmists.engine.component.Camera
import com.martmists.engine.render.DefaultRenderPipeline
import com.martmists.engine.render.RenderPipeline
import org.joml.Vector2i
import org.joml.Vector2ic

class Viewport(var scene: Scene = Scene.Empty) {
    var size: Vector2ic = Vector2i(1280, 720)
    var pipeline: RenderPipeline = DefaultRenderPipeline
    var activeCameraIndex: Int = 0

    var camera: GameObject?
        get() {
            val cameras = scene.objects.filter { it.hasComponent<Camera>() }
            return if (activeCameraIndex in cameras.indices) {
                cameras[activeCameraIndex]
            } else null
        }
        set(value: GameObject?) {
            activeCameraIndex = if (camera == null) {
                -1
            } else {
                scene.objects.filter { it.hasComponent<Camera>() }.indexOf(value)
            }
        }
}
