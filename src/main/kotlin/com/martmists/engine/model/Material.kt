package com.martmists.engine.model

import com.martmists.engine.math.Color
import com.martmists.engine.render.TextureHandle

data class Material(
    val name: String,
    val ambientColor: Color,
    val diffuseColor: Color,
    val emissiveColor: Color,
    val specularColor: Color,
    val ambientTexture: TextureHandle?,
    val diffuseTexture: TextureHandle?,
    val emissiveTexture: TextureHandle?,
    val specularTexture: TextureHandle?,
    val normalTexture: TextureHandle?,
    val displacementTexture: TextureHandle?,
    val ambientUVIndex: Int,
    val diffuseUVIndex: Int,
    val emissiveUVIndex: Int,
    val specularUVIndex: Int,
    val normalUVIndex: Int,
    val opacity: Float,
    val reflectivity: Float,
    val shininess: Float,
)
