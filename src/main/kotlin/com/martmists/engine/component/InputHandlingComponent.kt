package com.martmists.engine.component

import com.martmists.engine.input.InputEvent
import com.martmists.engine.input.InputHandler
import com.martmists.engine.scene.GameObject

abstract class InputHandlingComponent(gameObject: GameObject) : Component(gameObject) {
    val isActiveHandler: Boolean
        get() = InputHandler.activeHandler == this

    fun markActiveInputHandler() {
        InputHandler.activeHandler = this
    }

    protected fun getEventsToProcess(): List<InputEvent> = InputHandler.queuedEvents.getAndSet(mutableListOf())
}
