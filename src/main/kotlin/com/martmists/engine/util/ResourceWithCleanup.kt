package com.martmists.engine.util

import java.lang.ref.Cleaner

abstract class ResourceWithCleanup {
    protected fun registerCleaner() {
        cleaner.register(this, createCleaner())
    }

    abstract fun createCleaner(): Runnable

    companion object {
        private val cleaner = Cleaner.create()
    }
}
