package com.illuzionzstudios.mist.config.locale

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import java.util.function.Consumer

/**
 * Represents a group of strings for a section. Eg strings to do with interface or the player.
 * The way this works is we create strings via this group and then load this group after
 * the locale is loaded. This way the [MistString]'s get loaded from locale without null
 * errors. Only one loader is needed for all strings. Only group if they need to be loaded in
 * different stages
 *
 * Should be loaded in [SpigotPlugin.onReloadablesStart]
 */
class MistStringGroup {
    /**
     * List of strings in group
     */
    private val strings: MutableList<MistString> = ArrayList()

    /**
     * Create a new string and add it to our group
     *
     * @param key Key from config
     * @param def Default value
     * @return Created [MistString]
     */
    fun create(key: String, vararg def: String): MistString {
        val string = MistString(key, *def)
        strings.add(string)
        return string
    }

    /**
     * Load all strings into cache and locale
     */
    fun load() {
        strings.forEach(Consumer { obj: MistString -> obj.toString() })
    }
}