package com.illuzionzstudios.mist.ui.render

import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * This instance handles rendering an actual [com.illuzionzstudios.mist.ui.UserInterface]
 * It contains the methods like setting title, setting all items, etc
 */
class InterfaceDrawer private constructor(
    /**
     * Amount of slots in the inventory
     */
    val size: Int,
    /**
     * The title for the interface
     *
     *
     * Updating does not update interface, you have
     * to manually redraw it
     */
    var title: String
) {
    /**
     * The items (or content) inside the inventory
     */
    private val content: Array<ItemStack?> = arrayOfNulls(size)

    /**
     * Push an item onto the stack. This means we set the next available slot,
     * or [org.bukkit.Material.AIR] to this item. If there are no free slots,
     * the last slot in the inventory is set
     *
     * @param item The item to push
     */
    fun pushItem(item: ItemStack?) {
        // If we found a slot and pushed
        var added = false
        for (i in content.indices) {
            val currentItem = content[i]
            if (currentItem == null) {
                content[i] = item
                added = true
                break
            }
        }
        if (!added) content[size - 1] = item
    }

    /**
     * Simple check for if a slot is not null
     *
     * @param slot Slot to check
     * @return If slot exists
     */
    fun isSet(slot: Int): Boolean {
        return getItem(slot) != null
    }

    /**
     * Retrieve the [ItemStack] for a certain slot
     *
     * @param slot The slot to get the item by
     * @return Found [ItemStack] otherwise `null` if slot is outside
     * total slots
     */
    fun getItem(slot: Int): ItemStack? {
        return if (slot < content.size) content[slot] else null
    }

    /**
     * Shorthand way to set an item in a slot
     *
     * @param slot The slot to set the item in
     * @param item The [to set][ItemStack]
     */
    fun setItem(slot: Int, item: ItemStack?) {
        // Don't set out of bounds
        if (slot >= 0) content[slot] = item
    }

    /**
     * Set the full content of this inventory
     *
     * If the given content is shorter, all additional inventory slots are replaced with air
     *
     * @param newContent the new content
     */
    fun setContent(newContent: Array<ItemStack?>) {
        for (i in content.indices) content[i] = if (i < newContent.size) newContent[i] else ItemStack(
            XMaterial.AIR.parseMaterial()!!
        )
    }

    /**
     * Draw the interface for a player with all elements.
     * Closes other inventories when opening this one
     *
     * @param player The player to draw to
     */
    fun display(player: Player) {
        // Create our inventory instance
        val inventory = Bukkit.createInventory(player, size, TextUtil.formatText("&7$title"))
        inventory.contents = content

        // Clear inventories and open
        player.closeInventory()
        player.openInventory(inventory)
    }

    companion object {
        /**
         * Shorthand constructor. See [.InterfaceDrawer]
         */
        fun of(size: Int, title: String): InterfaceDrawer {
            return InterfaceDrawer(size, title)
        }
    }

}