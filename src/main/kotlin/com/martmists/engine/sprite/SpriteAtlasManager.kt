package com.martmists.engine.sprite

object SpriteAtlasManager {
    private val atlases = mutableListOf<SpriteAtlas>()
    private val spriteMap = mutableMapOf<Sprite, SpriteAtlas>()

    fun registerSprite(sprite: Sprite): SpriteAtlas.Entry {
        require(sprite.size.x < 2048 && sprite.size.y < 2048) { println("Sprite is too large for a Sprite Atlas")}
        val atlas = spriteMap[sprite]
        if (atlas != null) {
            val pos = atlas.spriteMap[sprite]!!
            return SpriteAtlas.Entry(
                atlas,
                sprite,
                pos.size,
                pos.offset,
            )
        } else {

            for (atlas in atlases) {
                val res = atlas.addSprite(sprite) ?: continue
                spriteMap[sprite] = atlas
                return res
            }

            // Failed to fit existing maps, generate new
            val atl = SpriteAtlas()
            atlases.add(atl)
            spriteMap[sprite] = atl
            return atl.addSprite(sprite)!!
        }
    }
}
