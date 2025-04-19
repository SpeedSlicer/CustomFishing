package com.illuzionzstudios.mist.random

import com.illuzionzstudios.mist.model.Pair
import java.util.*

/**
 * Contains items that can be picked based on weights
 */
class LootTable<T> {
    /**
     * Stored rewards in our table
     */
    private val lootTable: MutableList<Pair<T, Double>> = LinkedList()

    /**
     * Total weight of rewards used for picking
     */
    private var totalWeight = 0.0

    /**
     * Add new loot with weight
     *
     * @param type   The type of loot based on LootTable
     * @param weight The weight as a double
     */
    fun addLoot(type: T, weight: Double) {
        lootTable.add(Pair(type, weight))
        totalWeight += weight
    }

    /**
     * Pick random item from loot table based on weight
     */
    fun pick(): T? {
        if (lootTable.isEmpty()) return null

        var currentItemUpperBound = 0.0
        val random = Random()
        val nextValue = (totalWeight - 0) * random.nextDouble()
        for (itemAndWeight in lootTable) {
            currentItemUpperBound += itemAndWeight.value
            if (nextValue < currentItemUpperBound) return itemAndWeight.key
        }
        return lootTable[lootTable.size - 1].key
    }

    /**
     * Clears all current loot in the table
     */
    fun clear() {
        lootTable.clear()
    }
}