package com.martmists.engine.data

import com.martmists.engine.render.Shader
import com.martmists.engine.util.Resource
import com.martmists.engine.util.ResourceLoader
import com.martmists.engine.util.contextLazy

object BuiltinShaders {
    val default by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/default.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/default.frag.glsl")),
        )
    }
    val wireframeAxes by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe_axes.vert.glsl")),
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe.frag.glsl")),
        )
    }
    val wireframeLightsDirectional by contextLazy {
        Shader(
            ResourceLoader.loadTextFile(Resource("res:shaders/wireframe_lights_directional.vert.glsl")),
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
