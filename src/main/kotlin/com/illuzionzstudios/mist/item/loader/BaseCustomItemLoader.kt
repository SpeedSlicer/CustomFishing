package com.illuzionzstudios.mist.item.loader

import com.cryptomorin.xseries.XEnchantment
import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.config.ConfigSection
import com.illuzionzstudios.mist.config.locale.MistString
import com.illuzionzstudios.mist.config.serialization.loader.type.YamlSectionLoader
import com.illuzionzstudios.mist.exception.PluginException
import com.illuzionzstudios.mist.item.CustomItem
import java.util.*

/**
 * A loader for a [CustomItem] from a YAML config section. Can be implemented
 * with another custom item by overriding [.returnImplementedObject] and creating
 * new instance of custom item with loading already done for that item.
 */
abstract class BaseCustomItemLoader<T : CustomItem?>(section: ConfigSection) : YamlSectionLoader<T>(section) {

    override fun save(): Boolean {
        // TODO: Implement saving
        return true
    }

    override fun loadObject(file: ConfigSection?): T {
        // Lets try build the item
        val item = returnImplementedObject(file)

        // Set base options
        item?.identifier = loader?.nodeKey!!
        item?.customName = MistString.of(loader?.getString("item-name"))
        item?.lore = MistString.fromStringList(loader?.getStringList("lore")?.toMutableList())
        item?.amount = loader?.getInt("amount") ?: 1
        item?.material = XMaterial.matchXMaterial(loader?.getString("material") ?: "AIR").get()
        item?.damage = loader?.getInt("damage") ?: 0
        item?.data = (loader?.getInt("data") ?: 0).toShort()
        item?.customModelData = loader?.getInt("model-data") ?: 0

        val enchants: MutableMap<XEnchantment, Int> = EnumMap(XEnchantment::class.java)
        if (loader?.getStringList("enchants") != null) {
            for (toParse in loader?.getStringList("enchants")!!) {
                val tokens = toParse.split(":".toRegex()).toTypedArray()
                enchants[XEnchantment.matchXEnchantment(tokens[0])
                    .orElseThrow { PluginException("Enchant " + tokens[0] + " does not exist") }] = tokens[1].toInt()
            }
        }

        item?.enchants = enchants
        item?.glowing = loader?.getBoolean("glowing")!!
        item?.unbreakable = loader?.getBoolean("unbreakable")!!
        item?.hideFlags = loader?.getBoolean("hide-flags")!!
        return item
    }

    /**
     * Can be overridden to return the new custom item with the loading done
     * for that item.
     */
    protected abstract fun returnImplementedObject(configSection: ConfigSection?): T
}