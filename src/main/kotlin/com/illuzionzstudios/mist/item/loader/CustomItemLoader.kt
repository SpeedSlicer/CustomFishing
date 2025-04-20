package com.illuzionzstudios.mist.item.loader

import com.illuzionzstudios.mist.config.ConfigSection
import com.illuzionzstudios.mist.item.CustomItem

/**
 * Custom item loader for normal implementation
 */
class CustomItemLoader(section: ConfigSection) : BaseCustomItemLoader<CustomItem>(section) {
    override fun returnImplementedObject(configSection: ConfigSection?): CustomItem {
        return CustomItem()
    }
}