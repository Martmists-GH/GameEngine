package com.martmists.engine.component

import com.martmists.engine.scene.GameObject
import imgui.ImGui

/**
 * Simple ImGui renderer component.
 *
 * Invokes [renderCallback] every frame.
 */
open class ImguiRenderer(gameObject: GameObject) : Component(gameObject) {
    var renderCallback: () -> Unit = {}
    var windowName: String = "ImGui Window"

    fun render() {
        if (ImGui.begin(windowName)) {
            renderCallback()
        }
        ImGui.end()
    }

    override fun copyFor(other: GameObject) = ImguiRenderer(other).also {
        it.renderCallback = renderCallback
        it.windowName = windowName
    }
}
