package com.martmists.engine.util

import java.lang.ref.Cleaner
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class ResourceWithCleanup {
    init {
        cleaner.register(this, createCleaner())
    }

    abstract fun createCleaner(): Runnable

    companion object {
        private val cleaner = Cleaner.create()
    }
}
