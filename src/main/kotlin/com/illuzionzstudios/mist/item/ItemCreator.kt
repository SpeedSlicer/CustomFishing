package com.illuzionzstudios.mist.item

import com.cryptomorin.xseries.XEnchantment
import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.compatibility.ServerVersion
import com.illuzionzstudios.mist.compatibility.ServerVersion.V
import com.illuzionzstudios.mist.compatibility.XItemFlag
import com.illuzionzstudios.mist.compatibility.XProperty
import com.illuzionzstudios.mist.util.TextUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import java.util.function.Consumer

/**
 * Utility class to easily build custom items. We can set flags,
 * names and lore, and enchantments. Provides a way to
 * just easily construct an item
 */
class ItemCreator(
    /**
     * The actual item stack this represents
     */
    val item: ItemStack? = null,

    /**
     * The [XMaterial] of the item
     */
    val material: XMaterial,

    /**
     * The amount of items in the stack
     */
    val amount: Int = 1,

    /**
     * Data for older versions
     */
    val data: Short = 0,

    /**
     * Damage to the item for setting durability
     */
    val damage: Int = -1,

    /**
     * Custom model data
     */
    val customModelData: Int = 0,

    /**
     * The display name of the item
     */
    val name: String? = null,

    /**
     * The lore strings to display
     */

    val lores: List<String?>? = null,

    /**
     * The enchants applied for the item mapped by level
     */
    val enchants: Map<XEnchantment, Int>? = null,

    /**
     * The item flags
     */
    val flags: MutableList<XItemFlag> = ArrayList(),

    /**
     * The actual metadata of the item stack
     */
    val meta: ItemMeta? = null,

    /**
     * If the [ItemStack] has the unbreakable flag
     */
    var unbreakable: Boolean = false,

    /**
     * Should we hide all tags from the item (enchants, etc.)?
     */
    var hideTags: Boolean = false,

    /**
     * Should we add glow to the item? (adds a fake enchant and uses
     * item flags to hide it)
     *
     *
     * The enchant is visible on older MC versions.
     */
    val glow: Boolean = false
) {

    /**
     * @return This item suitable for a [com.illuzionzstudios.mist.ui.UserInterface]
     */
    fun makeUIItem(): ItemStack {
        unbreakable = true
        hideTags = true
        return make()
    }

    /**
     * Finally construct the [ItemStack] from all parameters
     *
     * @return The built item
     */
    fun make(): ItemStack {
        // Make sure base item and material are set
        Valid.checkBoolean(
            material.parseMaterial() != null || item != null,
            "Material or item must be set!"
        )

        // Actual item we're building on
        val stack = item?.clone() ?: material.parseItem()!!
        val stackMeta = meta?.clone() ?: stack.itemMeta
        Valid.checkNotNull(stackMeta, "Item metadata was somehow null")

        // Skip if trying to build on air
        if (material == XMaterial.AIR) return stack

        // Set item amount
        stack.amount = this.amount

        // Set damage
        if (damage != -1) {
            try {
                // Old versions
                stack.durability = damage.toShort()
            } catch (ignored: Throwable) {
            }
            try {
                if (stackMeta is Damageable) {
                    stackMeta.damage = damage
                }
            } catch (ignored: Throwable) {
            }
        }

        // Custom model data only in 1.14+
        if (ServerVersion.atLeast(V.v1_14) && customModelData != 0) stackMeta?.setCustomModelData(customModelData)

        // Glow
        if (glow) {
            stackMeta?.addEnchant(Enchantment.DURABILITY, 1, true)
            flags.add(XItemFlag.HIDE_ENCHANTS)
        }

        // Enchantments
        if (enchants != null) {
            for (ench in enchants.keys) {
                stackMeta?.addEnchant(ench.enchant!!, enchants[ench]!!, true)
            }
        }

        // Name and lore
        if (name != null) {
            stackMeta?.setDisplayName(TextUtil.formatText("&r$name"))
        }
        if (lores != null && lores.isNotEmpty()) {
            val coloredLores: MutableList<String> = ArrayList()
            lores.forEach(Consumer { line: String? ->
                // Colour and split by \n
                val lines: Array<String>? = line?.split("\\r?\\n".toRegex())?.toTypedArray()
                // Append '&7' before every line instead of ugly purple italics
                lines?.forEach { line2: String? -> coloredLores.add(TextUtil.formatText(ChatColor.GRAY.toString() + line2)) }
            })
            stackMeta?.lore = coloredLores
        }

        // Unbreakable
        if (unbreakable) {
            if (ServerVersion.olderThan(V.v1_12)) {
                try {
                    val spigot = stackMeta?.javaClass?.getMethod("spigot")?.invoke(stackMeta)
                    spigot?.javaClass?.getMethod("setUnbreakable", Boolean::class.javaPrimitiveType)?.invoke(spigot, true)
                } catch (ignored: Throwable) {
                    // Probably 1.7.10, tough luck
                }
            } else {
                XProperty.UNBREAKABLE.apply(stackMeta!!, true)
            }
        }

        // Hide flags
        if (hideTags) {
            for (f in XItemFlag.values()) {
                if (!flags.contains(f)) {
                    flags.add(f)
                }
            }
        }

        // Apply flags
        for (flag in flags) {
            try {
                stackMeta?.addItemFlags(ItemFlag.valueOf(flag.toString()))
            } catch (ignored: Throwable) {
            }
        }

        // Finally apply metadata
        stack.itemMeta = stackMeta
        return stack
    }

    companion object {
        fun builder(): ItemCreatorBuilder = ItemCreatorBuilder()
    }

    data class ItemCreatorBuilder(
        /**
         * The actual item stack this represents
         */
        var item: ItemStack? = null,

        /**
         * The [XMaterial] of the item
         */
        var material: XMaterial = XMaterial.STONE,

        /**
         * The amount of items in the stack
         */
        var amount: Int = 1,

        /**
         * Data for older versions
         */
        var data: Short = 0,

        /**
         * Damage to the item for setting custom metadata
         */
        var damage: Int = -1,

        /**
         * Custom model data
         */
        var customModelData: Int = 0,

        /**
         * The display name of the item
         */
        var name: String? = null,

        /**
         * The lore strings to display
         */

        var lores: List<String?>? = null,

        /**
         * The enchants applied for the item mapped by level
         */
        var enchants: Map<XEnchantment, Int>? = null,

        /**
         * The item flags
         */
        var flags: MutableList<XItemFlag> = ArrayList(),

        /**
         * The actual metadata of the item stack
         */
        var meta: ItemMeta? = null,

        /**
         * If the [ItemStack] has the unbreakable flag
         */
        var unbreakable: Boolean = false,

        /**
         * Should we hide all tags from the item (enchants, etc.)?
         */
        var hideTags: Boolean = false,

        /**
         * Should we add glow to the item? (adds a fake enchant and uses
         * item flags to hide it)
         *
         * The enchant is visible on older MC versions.
         */
        var glow: Boolean = false
    ) {
        fun item(item: ItemStack) = apply { this.item = item }
        fun material(item: XMaterial) = apply { this.material = item }
        fun amount(item: Int) = apply { this.amount = item }
        fun damage(item: Int) = apply { this.damage = item }
        fun data(item: Short) = apply { this.data = item }
        fun customModelData(item: Int) = apply { this.customModelData = item }
        fun name(item: String) = apply { this.name = item }
        fun lores(item: List<String>) = apply { this.lores = item }
        fun lore(item: String) = apply { this.lores = listOf(item) }
        fun enchants(item: Map<XEnchantment, Int>) = apply { this.enchants = item }
        fun flags(item: MutableList<XItemFlag>) = apply { this.flags = item }
        fun meta(item: ItemMeta) = apply { this.meta = item }
        fun unbreakable(item: Boolean) = apply { this.unbreakable = item }
        fun hideTags(item: Boolean) = apply { this.hideTags = item }
        fun glow(item: Boolean) = apply { this.glow = item }
        fun build() = ItemCreator(
            item,
            material,
            amount,
            data,
            damage,
            customModelData,
            name,
            lores,
            enchants,
            flags,
            meta,
            unbreakable,
            hideTags,
            glow
        )
    }
}