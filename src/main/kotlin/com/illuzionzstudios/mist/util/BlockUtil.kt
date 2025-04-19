package com.illuzionzstudios.mist.util

import org.bukkit.Location
import org.bukkit.block.Block

/**
 * Util class for editing and getting blocks in the world
 */
class BlockUtil {

    companion object {

        /**
         * Get nearby blocks in a radius of a location. (Cuboid)
         *
         * @param location Center location
         * @param radius   Radius in blocks
         * @return List of all blocks
         */
        fun getNearbyBlocks(location: Location, radius: Int): List<Block> {
            val blocks: MutableList<Block> = ArrayList()
            for (x in location.blockX - radius..location.blockX + radius) {
                for (y in location.blockY - radius..location.blockY + radius) {
                    for (z in location.blockZ - radius..location.blockZ + radius) {
                        if (location.world == null) continue
                        blocks.add(location.world!!.getBlockAt(x, y, z))
                    }
                }
            }
            return blocks
        }
    }
}