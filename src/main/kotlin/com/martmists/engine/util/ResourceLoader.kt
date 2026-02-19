package com.martmists.engine.util

import com.martmists.engine.animation.Animation
import com.martmists.engine.animation.AnimationChannel
import com.martmists.engine.animation.Keyframe
import com.martmists.engine.math.Color
import com.martmists.engine.math.Mat4x4
import com.martmists.engine.math.Quat
import com.martmists.engine.math.Vec2i
import com.martmists.engine.math.Vec3
import com.martmists.engine.model.Geometry
import com.martmists.engine.model.Material
import com.martmists.engine.model.ModelMesh
import com.martmists.engine.model.Model
import com.martmists.engine.model.ModelPart
import com.martmists.engine.render.Shader
import com.martmists.engine.render.TextureHandle
import com.martmists.engine.sprite.Sprite
import com.martmists.engine.sprite.Spritesheet
import org.joml.Matrix4f
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

object ResourceLoader {
    private val modelCache = mutableMapOf<String, Model>()
    private val textureCache = mutableMapOf<String, TextureHandle?>()
    private val spritesheetCache = mutableMapOf<String, Spritesheet?>()

    private val MODEL_FLAGS = arrayOf(
        aiProcess_CalcTangentSpace,
        aiProcess_FindInvalidData,
        aiProcess_FixInfacingNormals,
        aiProcess_FlipWindingOrder,
        aiProcess_GenNormals,
        aiProcess_GenUVCoords,
        aiProcess_FindInstances,
        aiProcess_FlipUVs,
        aiProcess_ImproveCacheLocality,
        aiProcess_JoinIdenticalVertices,
        aiProcess_LimitBoneWeights,
        aiProcess_OptimizeGraph,
        aiProcess_OptimizeMeshes,
        aiProcess_RemoveRedundantMaterials,
        aiProcess_SortByPType,
        aiProcess_SplitByBoneCount,
        aiProcess_Triangulate,
    ).reduce(Int::or)

    private class LoadContext(
        val directory: Resource,
        val embeddedTextures: Map<String, ByteBuffer>,
    ) {
        val boneOffsetsMap = mutableMapOf<String, Mat4x4>()
        val boneNameList = mutableListOf<String>()
    }


    fun loadModel(res: Resource): Model {
        return modelCache.getOrPut(res.absolutePath) {
            val store = aiCreatePropertyStore() ?: error("Unable to create property store")
            aiSetImportPropertyInteger(store, AI_CONFIG_PP_SBBC_MAX_BONES, 200)

            val bytes = res.readAllBytes()
            val buffer = MemoryUtil.memAlloc(bytes.size)
            val aiScene = try {
                buffer.put(bytes)
                buffer.flip()
                aiImportFileFromMemoryWithProperties(buffer, MODEL_FLAGS, res.pathComponent, store) ?:
                    error(aiGetErrorString()?.takeIf { it.isNotBlank() } ?: "Error in Assimp loading")
            } finally {
                MemoryUtil.memFree(buffer)
            }
            val animations = Array(aiScene.mNumAnimations()) {
                val anim = AIAnimation.create(aiScene.mAnimations()!![it])
                parseAnimation(anim)
            }
            val embeddedTextures = List(aiScene.mNumTextures()) {
                val tex = AITexture.create(aiScene.mTextures()!![it])
                val filename = tex.mFilename().dataString()
                listOfNotNull(
                    "*$it" to tex.pcDataCompressed(),
                    (filename to tex.pcDataCompressed()).takeIf { filename.isNotBlank() }
                )
            }.flatten().toMap()
            val context = LoadContext(
                res.parentResource,
                embeddedTextures
            )
            with(context) {
                val materials = Array(aiScene.mNumMaterials()) {
                    val mat = AIMaterial.create(aiScene.mMaterials()!![it])
                    parseMaterial(mat)
                }
                val parts = Array(aiScene.mNumMeshes()) {
                    val aiMesh = AIMesh.create(aiScene.mMeshes()!![it])
                    val mat = materials[aiMesh.mMaterialIndex()]
                    val mesh = parseMesh(aiMesh)
                    Geometry(
                        mesh,
                        mat,
                    )
                }
                val root = parseTree(aiScene.mRootNode()!!, parts)
                Model(res, root, animations, boneOffsetsMap.toMap()) {
                    Shader(
                        loadTextFile(Resource("res:shaders/default.vert.glsl")),
                        loadTextFile(Resource("res:shaders/default.frag.glsl")),
                    )
                }
            }
        }
    }

    context(ctx: LoadContext)
    private fun parseMaterial(mat: AIMaterial): Material {
        return Material(
            mat.getString(AI_MATKEY_NAME),
            mat.getColor(AI_MATKEY_COLOR_AMBIENT),
            mat.getColor(AI_MATKEY_COLOR_DIFFUSE),
            mat.getColor(AI_MATKEY_COLOR_EMISSIVE),
            mat.getColor(AI_MATKEY_COLOR_SPECULAR),
            mat.getTexture(aiTextureType_AMBIENT, true),
            mat.getTexture(aiTextureType_DIFFUSE, true),
            mat.getTexture(aiTextureType_EMISSIVE, true),
            mat.getTexture(aiTextureType_SPECULAR, true),
            mat.getTexture(aiTextureType_NORMALS, false),
            mat.getTexture(aiTextureType_DISPLACEMENT, false)
                ?: mat.getTexture(aiTextureType_HEIGHT, false)
                ?: mat.getTexture(aiTextureType_DIFFUSE_ROUGHNESS, false),
            mat.getUVIndex(aiTextureType_AMBIENT),
            mat.getUVIndex(aiTextureType_DIFFUSE),
            mat.getUVIndex(aiTextureType_EMISSIVE),
            mat.getUVIndex(aiTextureType_SPECULAR),
            mat.getUVIndex(aiTextureType_NORMALS),
            mat.getFloat(AI_MATKEY_OPACITY, 1f),
            mat.getFloat(AI_MATKEY_REFLECTIVITY, 0f),
            mat.getFloat(AI_MATKEY_SHININESS, 32f),
        )
    }

    private fun AIMaterial.getFloat(key: String, default: Float): Float {
        val ptr = PointerBuffer.allocateDirect(1).also { aiGetMaterialProperty(this, key, it) }
        val prop = ptr.get()
        if (prop == 0L) return default
        return AIMaterialProperty.create(prop).mData().asFloatBuffer().get()
    }

    private fun AIMaterial.getString(key: String): String {
        val string = AIString.create().also { aiGetMaterialString(this, key, 0, 0, it) }
        return string.dataString()
    }

    private fun AIMaterial.getColor(key: String): Color {
        val color = AIColor4D.create().also { aiGetMaterialColor(this, key, 0, 0, it) }
        return Color(color.r(), color.g(), color.b(), color.a())
    }

    context(ctx: LoadContext)
    private fun AIMaterial.getTexture(key: Int, isSRGB: Boolean): TextureHandle? {
        val texPath = AIString.create().also { aiGetMaterialTexture(this, key, 0, it, null as IntArray?, null, null, null, null, null) }
        val relPath = texPath.dataString()
        if (relPath.isBlank()) return null
        val texFile = ctx.directory.resolve(relPath.replace('\\', '/'))
        if (texFile.exists() || ctx.embeddedTextures.containsKey(relPath)) {
            return textureCache.getOrPut(texFile.path) {
                ctx.embeddedTextures[relPath]?.let {
                    parseImage(it, isSRGB)
                } ?: run {
                    val bytes = texFile.readAllBytes()
                    val buffer = MemoryUtil.memAlloc(bytes.size)
                    try {
                        buffer.put(bytes)
                        buffer.flip()
                        parseImage(buffer, isSRGB)
                    } finally {
                        MemoryUtil.memFree(buffer)
                    }
                }
            }
        }
        println("WARNING: Texture $texFile does not exist")
        return null
    }

    private fun AIMaterial.getUVIndex(textureType: Int): Int {
        val pOut = IntArray(1)
        val res = aiGetMaterialIntegerArray(
            this,
            $$"$tex.uvwsrc",
            textureType,
            0,
            pOut,
            intArrayOf(1)
        )
        return if (res == aiReturn_SUCCESS) pOut[0] else 0
    }

    fun loadImage(resource: Resource, isSRGB: Boolean = true): TextureHandle? {
        return textureCache.getOrPut(resource.absolutePath) {
            val bytes = resource.readAllBytes()
            val buffer = MemoryUtil.memAlloc(bytes.size)

            try {
                buffer.put(bytes)
                buffer.flip()
                parseImage(buffer, isSRGB)
            } finally {
                MemoryUtil.memFree(buffer)
            }
        }
    }

    private fun parseImage(buffer: ByteBuffer, isSRGB: Boolean): TextureHandle? {
        val width = IntArray(1)
        val height = IntArray(1)
        val channels = IntArray(1)

        val pixelData = stbi_load_from_memory(buffer, width, height, channels, 4) ?: return null.also { println("Failed to load texture from memory") }
        return TextureHandle().also {
            it.setData(pixelData, width[0], height[0], isSRGB)
            stbi_image_free(pixelData)
        }
    }

    context(ctx: LoadContext)
    private fun parseMesh(aiMesh: AIMesh): ModelMesh {
        val name = aiMesh.mName().dataString()
        val vertexCount = aiMesh.mNumVertices()
        val numFields = 16
        val vertices = FloatArray(vertexCount * numFields)
        val indices = IntArray(aiMesh.mNumFaces() * 3)

        val boneIds = IntArray(vertexCount * 4) { -1 }
        val boneWeights = FloatArray(vertexCount * 4) { 0f }
        val boneCounter = IntArray(vertexCount)

        for (b in 0 until aiMesh.mNumBones()) {
            val aiBone = AIBone.create(aiMesh.mBones()!![b])
            val boneName = aiBone.mName().dataString()
            val boneId = if (ctx.boneOffsetsMap.containsKey(boneName)) {
                ctx.boneNameList.indexOf(boneName)
            } else {
                val id = ctx.boneNameList.size
                ctx.boneNameList.add(boneName)
                ctx.boneOffsetsMap[boneName] = aiBone.mOffsetMatrix().toMat4x4()
                id
            }

            val weights = aiBone.mWeights()
            for (w in 0 until aiBone.mNumWeights()) {
                val weight = weights.get(w)
                val vertexId = weight.mVertexId()
                val weightVal = weight.mWeight()
                val count = boneCounter[vertexId]
                if (count < 4) {
                    boneIds[vertexId * 4 + count] = boneId
                    boneWeights[vertexId * 4 + count] = weightVal
                    boneCounter[vertexId]++
                }
            }
        }

        val positions = aiMesh.mVertices()
        val normals = aiMesh.mNormals()!!
        val tangents = aiMesh.mTangents()
        val bitangents = aiMesh.mBitangents()
        val uv0 = aiMesh.mTextureCoords(0)
        val uv1 = aiMesh.mTextureCoords(1)

        if (uv0 == null && uv1 == null) {
            println("WARNING: $name did not have texture coords!")
        }

        for (i in 0 until vertexCount) {
            val pos = positions.get(i)
            vertices[i * numFields + 0] = pos.x()
            vertices[i * numFields + 1] = pos.y()
            vertices[i * numFields + 2] = pos.z()

            val norm = normals.get(i)
            vertices[i * numFields + 3] = norm.x()
            vertices[i * numFields + 4] = norm.y()
            vertices[i * numFields + 5] = norm.z()

            if (uv0 != null) {
                val tc = uv0.get(i)
                vertices[i * numFields + 6] = tc.x()
                vertices[i * numFields + 7] = tc.y()
            } else {
                vertices[i * numFields + 6] = 0.0f
                vertices[i * numFields + 7] = 0.0f
            }

            if (uv1 != null) {
                val tc = uv1.get(i)
                vertices[i * numFields + 8] = tc.x()
                vertices[i * numFields + 9] = tc.y()
            } else {
                vertices[i * numFields + 8] = 0.0f
                vertices[i * numFields + 9] = 0.0f
            }

            if (tangents != null) {
                val tan = tangents.get(i)
                vertices[i * numFields + 10] = tan.x()
                vertices[i * numFields + 11] = tan.y()
                vertices[i * numFields + 12] = tan.z()
            } else {
                vertices[i * numFields + 10] = 1.0f
                vertices[i * numFields + 11] = 0.0f
                vertices[i * numFields + 12] = 0.0f
            }

            if (bitangents != null) {
                val bitan = bitangents.get(i)
                vertices[i * numFields + 13] = bitan.x()
                vertices[i * numFields + 14] = bitan.y()
                vertices[i * numFields + 15] = bitan.z()
            } else {
                vertices[i * numFields + 13] = 1.0f
                vertices[i * numFields + 14] = 0.0f
                vertices[i * numFields + 15] = 0.0f
            }
        }

        val faces = aiMesh.mFaces()
        for (i in 0 until aiMesh.mNumFaces()) {
            val face = faces.get(i)
            indices[i * 3 + 0] = face.mIndices().get(0)
            indices[i * 3 + 1] = face.mIndices().get(1)
            indices[i * 3 + 2] = face.mIndices().get(2)
        }

        return ModelMesh(name, vertices, indices, boneIds, boneWeights)
    }

    private fun parseTree(node: AINode, allRoots: Array<Geometry>): ModelPart {
        val geoms = Array(node.mNumMeshes()) {
            val idx = node.mMeshes()!![it]
            allRoots[idx]
        }

        val children = Array(node.mNumChildren()) {
            val node = AINode.create(node.mChildren()!![it])
            parseTree(node, allRoots)
        }

        return ModelPart(
            node.mName().dataString(),
            node.getTransformation(),
            geoms,
            children,
        )
    }

    private fun AIMatrix4x4.toMat4x4(): Mat4x4 {
        return Mat4x4(Matrix4f(
            a1(), b1(), c1(), d1(),
            a2(), b2(), c2(), d2(),
            a3(), b3(), c3(), d3(),
            a4(), b4(), c4(), d4(),
        ))
    }

    private fun AINode.getTransformation(): Mat4x4 {
        return mTransformation().toMat4x4()
    }

    private fun parseAnimation(anim: AIAnimation): Animation {
        val channels = Array(anim.mNumChannels()) {
            val channel = AINodeAnim.create(anim.mChannels()!![it])
            val translations = Array(channel.mNumPositionKeys()) {
                val kf = channel.mPositionKeys()!![it]
                Keyframe(kf.mTime().toFloat(), kf.mValue().toVec3(), kf.mInterpolation())
            }
            val rotations = Array(channel.mNumRotationKeys()) {
                val kf = channel.mRotationKeys()!![it]
                Keyframe(kf.mTime().toFloat(), kf.mValue().toQuat(), kf.mInterpolation())
            }
            val scales = Array(channel.mNumScalingKeys()) {
                val kf = channel.mScalingKeys()!![it]
                Keyframe(kf.mTime().toFloat(), kf.mValue().toVec3(), kf.mInterpolation())
            }
            AnimationChannel(
                channel.mNodeName().dataString(),
                translations.sortedBy { it.time }.toTypedArray(),
                rotations.sortedBy { it.time }.toTypedArray(),
                scales.sortedBy { it.time }.toTypedArray(),
            )
        }
        return Animation(
            anim.mName().dataString(),
            anim.mDuration().toFloat(),
            anim.mTicksPerSecond().toFloat(),
            channels,
        )
    }

    private fun AIVector3D.toVec3() = Vec3(x(), y(), z())
    private fun AIQuaternion.toQuat() = Quat(x(), y(), z(), w())

    fun loadTextFile(resource: Resource): String {
        return resource.readAllBytes().decodeToString()
    }

    // TODO: Json definitions for sprites? What about sprite maps?
    fun loadSpritesheet(resource: Resource): Spritesheet? {
        return spritesheetCache.getOrPut(resource.absolutePath) {
            val bytes = resource.readAllBytes()
            val buffer = MemoryUtil.memAlloc(bytes.size)

            try {
                buffer.put(bytes)
                buffer.flip()
                val width = IntArray(1)
                val height = IntArray(1)
                val channels = IntArray(1)
                val pixelData = stbi_load_from_memory(buffer, width, height, channels, 4) ?: return null.also { println("Failed to load Sprite from memory") }
                stbi_image_free(pixelData)
                Spritesheet(resource, Vec2i(width[0], height[0]))
            } finally {
                MemoryUtil.memFree(buffer)
            }
        }
    }
}
