package com.martmists.engine.util

import java.lang.ref.Cleaner
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class ResourceWithCleanup {
    private val didClean = AtomicBoolean(false)
    init {
        cleaner.register(this, this::doCleanup)
    }

    fun doCleanup() {
        if (didClean.compareAndSet(expectedValue = false, newValue = true)) return
        cleanup()
    }

    abstract fun cleanup()

    companion object {
        private val cleaner = Cleaner.create()
    }
}
