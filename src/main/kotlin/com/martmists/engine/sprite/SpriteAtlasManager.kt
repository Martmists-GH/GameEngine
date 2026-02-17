package com.martmists.engine.sprite

object SpriteAtlasManager {
    private val atlases = mutableListOf<SpriteAtlas>()

    fun registerSprite(sprite: Sprite): SpriteAtlas.Entry {
        require(sprite.size.x < 2048 && sprite.size.y < 2048) { println("Sprite is too large for a Sprite Atlas")}

        for (atlas in atlases) {
            val res = atlas.addSprite(sprite) ?: continue
            return res
        }

        // Failed to fit existing maps, generate new
        val atl = SpriteAtlas()
        atlases.add(atl)
        return atl.addSprite(sprite)!!
    }
}
