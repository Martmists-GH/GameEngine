package com.martmists.engine.input

import com.martmists.engine.Window
import com.martmists.engine.component.InputHandlingComponent
import com.martmists.engine.math.Vec2
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.lwjgl.glfw.GLFW

object InputHandler {
    internal val queuedEvents = atomic(mutableListOf<InputEvent>())
    var activeHandler: InputHandlingComponent? = null
        internal set

    val lastActiveKeys: Set<Int>
        field = mutableSetOf<Int>()
    val activeKeys: Set<Int>
        field = mutableSetOf<Int>()
    val lastActiveButtons: Set<Int>
        field = mutableSetOf<Int>()
    val activeButtons: Set<Int>
        field = mutableSetOf<Int>()
    var cursorPos = Vec2.Zero
        private set

    fun isKeyHeld(key: Int) = activeKeys.contains(key)
    fun isKeyPressed(key: Int) = !lastActiveKeys.contains(key) && activeKeys.contains(key)
    fun isKeyReleased(key: Int) = lastActiveKeys.contains(key) && !activeKeys.contains(key)
    fun isButtonHeld(button: Int) = activeButtons.contains(button)
    fun isButtonPressed(button: Int) = !lastActiveButtons.contains(button) && activeButtons.contains(button)
    fun isButtonReleased(button: Int) = lastActiveButtons.contains(button) && !activeButtons.contains(button)

    fun stepFrame() {
        lastActiveKeys.clear()
        lastActiveButtons.clear()
        lastActiveKeys.addAll(activeKeys)
        lastActiveButtons.addAll(activeButtons)
        queuedEvents.value = mutableListOf()
    }

    fun setupHandlersFor(window: Window) {
        GLFW.glfwSetKeyCallback(window.handle) { _, key, _, action, mods ->
            when (action) {
                GLFW.GLFW_PRESS -> {
                    activeKeys.add(key)
                    queuedEvents.update { it.add(KeyInputEvent(key, true, mods)); it }
                }

                GLFW.GLFW_RELEASE -> {
                    activeKeys.remove(key)
                    queuedEvents.update { it.add(KeyInputEvent(key, false, mods)); it }
                }
            }
        }?.free()
        GLFW.glfwSetCursorPosCallback(window.handle) { _, xPos, yPos ->
            val now = Vec2(xPos.toFloat(), yPos.toFloat())
            val delta = now - cursorPos
            queuedEvents.update { it.add(CursorPosEvent(delta, now)); it }
            cursorPos = now
        }?.free()
        GLFW.glfwSetMouseButtonCallback(window.handle) { _, button, action, mods ->
            when (action) {
                GLFW.GLFW_PRESS -> {
                    activeButtons.add(button)
                    queuedEvents.update { it.add(MouseInputEvent(button, true, mods)); it }
                }

                GLFW.GLFW_RELEASE -> {
                    activeButtons.remove(button)
                    queuedEvents.update { it.add(MouseInputEvent(button, false, mods)); it }
                }
            }
        }?.free()
    }
}
