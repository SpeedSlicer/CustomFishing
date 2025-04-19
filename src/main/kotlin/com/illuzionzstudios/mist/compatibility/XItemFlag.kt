package com.illuzionzstudios.mist.compatibility

import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/**
 * Wrapper for cross-version for [org.bukkit.inventory.ItemFlag]
 */
enum class XItemFlag {
    /**
     * Setting to show/hide enchants
     */
    HIDE_ENCHANTS,

    /**
     * Setting to show/hide Attributes like Damage
     */
    HIDE_ATTRIBUTES,

    /**
     * Setting to show/hide the unbreakable State
     */
    HIDE_UNBREAKABLE,

    /**
     * Setting to show/hide what the ItemStack can break/destroy
     */
    HIDE_DESTROYS,

    /**
     * Setting to show/hide where this ItemStack can be build/placed on
     */
    HIDE_PLACED_ON,

    /**
     * Setting to show/hide potion effects on this ItemStack
     */
    HIDE_POTION_EFFECTS, HIDE_DYE;

    /**
     * Tries to apply this item flag to the given item, fails silently
     */
    fun applyTo(item: ItemStack) {
        try {
            val meta = item.itemMeta
            val bukkitFlag = ItemFlag.valueOf(toString())
            meta?.addItemFlags(bukkitFlag)
            item.itemMeta = meta
        } catch (ignored: Throwable) {
            // Unsupported MC version
        }
    }
}