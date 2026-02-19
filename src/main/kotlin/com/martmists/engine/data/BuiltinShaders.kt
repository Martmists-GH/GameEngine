package com.martmists.engine.data

import com.martmists.engine.render.Shader
import com.martmists.engine.util.Resource
import com.martmists.engine.util.ResourceLoader
import com.martmists.engine.util.contextLazy

/**
 * Various shaders used in the game engine by default.
 */
object BuiltinShaders {
    val default by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/default.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/default.frag.glsl")),
        )
    }
    val texturedQuad by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/textured_quad.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/textured_quad.frag.glsl")),
        )
    }

    val wireframeSimple by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe_simple.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe.frag.glsl")),
        )
    }
    val wireframeMesh by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe_mesh.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe.frag.glsl")),
        )
    }
}
