package com.illuzionzstudios.mist.item

import com.cryptomorin.xseries.XEnchantment
import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.config.locale.MistString
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * A custom item is a custom item that is loaded from the config. This
 * allows us to customise things like display name and lore from a [com.illuzionzstudios.mist.config.ConfigSection]
 * and turn it into an item. Can be implemented to have custom items that are configured
 * by the config extending on the base functionality.
 *
 * This differs from [ItemCreator] as that is strictly for creating an item
 * stack where this is to create an item stack with a bit more functionality and
 * have our own sort of data attached to it.
 *
 * Is not a builder as we should set everything manually.
 *
 * Contains methods for manipulating item
 */
open class CustomItem(

    /**
     * The identifier of this custom item. Usually so we can
     * include this in a map if need be
     */
    var identifier: String = "null",

    /**
     * Data for older versions
     */
    var data: Short = 0,

    /**
     * Damage to the item for setting custom metadata
     */
    var damage: Int = -1,

    /**
     * Actual item stack constructed to perform more operations on
     */
    var item: ItemStack? = null,

    /**
     * The material of this item
     */
    var material: XMaterial = XMaterial.AIR,

    /**
     * Custom name of the item
     */
    var customName: MistString? = null,

    /**
     * Custom lore of the item
     */
    var lore: List<MistString>? = null,

    /**
     * Amount of the item
     */
    var amount: Int = 1,

    /**
     * Item custom model data
     */
    var customModelData: Int = 0,

    /**
     * Map of all enchantments
     */
    var enchants: Map<XEnchantment, Int>? = null,

    /**
     * If the item is glowing
     */
    var glowing: Boolean = false,

    /**
     * If the item is unbreakable
     */
    var unbreakable: Boolean = false,

    /**
     * If to hide all flags
     */
    var hideFlags: Boolean = false,
) {

    /**
     * Actually build the item. Must be called before any kind of
     * registering or giving of the item is done. If is overriden must
     * call this super method
     */
    fun buildItem(): ItemStack {
        val creator: ItemCreator.ItemCreatorBuilder = ItemCreator.builder().material(material)
        if (customName != null && customName.toString().trim { it <= ' ' }
                .isNotEmpty()) creator.name(customName.toString())
        if (lore != null) creator.lores(MistString.fromList(lore!!))
        creator.amount(amount)
        creator.data(data)
        creator.damage(damage)
        creator.customModelData(customModelData)
        creator.enchants(enchants!!)
        creator.glow(glowing)
        creator.unbreakable(unbreakable)
        creator.hideTags(hideFlags)
        item = creator.build().make()
        customiseItem()
        return item!!.clone()
    }

    /**
     * Do custom things to the item with extra loaded stuff right after [.item] instance
     * is set.
     */
    open fun customiseItem() {}

    /**
     * Makes sure item is set. Run where we need to make sure it isn't null
     */
    private fun checkBuilt() {
        if (item == null) buildItem()
    }

    /**
     * Give this item to a player
     *
     * @param player Player receiving item
     */
    fun givePlayer(player: Player) {
        checkBuilt()
        player.inventory.addItem(item)
    }
}