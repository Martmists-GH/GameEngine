package com.martmists.engine.render

import com.martmists.engine.scene.Viewport

interface RenderPipeline {
    fun render(viewport: Viewport, buffer: Framebuffer)
}

