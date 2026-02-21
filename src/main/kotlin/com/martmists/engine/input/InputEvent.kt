package com.martmists.engine.input

import com.martmists.engine.math.Vec2

sealed interface InputEvent
data class KeyInputEvent(val key: Int, val press: Boolean, val modifiers: Int) : InputEvent
data class CursorPosEvent(val relative: Vec2, val absolute: Vec2) : InputEvent
data class MouseInputEvent(val button: Int, val press: Boolean, val modifiers: Int) : InputEvent
