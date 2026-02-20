package com.martmists.engine.util

import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.internal.ImGuiContext
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext

object ImGuiRenderUtil {
    fun render(content: () -> Unit) {
        ImGui.setCurrentContext(data.context)
        data.gl3.newFrame()
        data.glfw.newFrame()
        ImGui.newFrame()
        content()
        ImGui.render()
        data.gl3.renderDrawData(ImGui.getDrawData())
    }

    private val data by contextLazy(::ImguiObjects)

    private class ImguiObjects : ResourceWithCleanup() {
        val gl3 = ImGuiImplGl3()
        val glfw = ImGuiImplGlfw()
        val context: ImGuiContext = ImGui.createContext()

        init {
            registerCleaner()

            check(glfw.init(glfwGetCurrentContext(), true))
            check(gl3.init())
        }

        override fun createCleaner(): Runnable = ImguiCleaner(glfwGetCurrentContext(), gl3, glfw, context)

        private class ImguiCleaner(val ctx: Long,
                                   val gl3: ImGuiImplGl3,
                                   val glfw: ImGuiImplGlfw,
                                   val context: ImGuiContext) : Runnable {
            override fun run() {
                GLGarbageCollector.markCallback(ctx) {
                    ImGui.destroyContext(context)
                    gl3.shutdown()
                    glfw.shutdown()
                }
            }
        }
    }
}
