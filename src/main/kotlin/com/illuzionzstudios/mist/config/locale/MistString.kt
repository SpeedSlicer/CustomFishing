package com.illuzionzstudios.mist.config.locale

import com.illuzionzstudios.mist.compatibility.ServerVersion
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.TextUtil
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.title.Title
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.function.Consumer
import java.util.regex.Matcher

/**
 * This represents a custom translatable string that can be used in the plugin where we
 * would usually use a [String]. It is based of plugin translation files and includes
 * utils for formatting and replacing parts of the string. Can also represent text without
 * being bound to a key.
 *
 * Contains full text engine for manipulating text
 */
class MistString(
    /**
     * The key of this string for the locale
     */
    private val key: String,

    /**
     * The raw contents of this string
     */
    private var value: String
) {
    /**
     * A map of replacements to insert into localised object. The format is "{X}" is replaced by "Y"
     */
    private val replacements: MutableMap<String, Any> = HashMap()

    /**
     * Action to perform when text is clicked
     */
    private val clickEvent: ClickEvent? = null

    /**
     * The event to use when text is hovered over
     */
    private val hoverEvent: HoverEvent<*>? = null

    /**
     * Load a mist string with multiple lines
     */
    constructor(key: String, vararg def: String) : this(key, def.joinToString("\n"))

    /**
     * Used when we just want a string with content and not
     * to be localised
     *
     * @param def The value of the string
     */
    constructor(def: String) : this("", def)

    /**
     * @param other Create string from another
     */
    constructor(other: MistString) : this(other.key, other.value)

    //  -------------------------------------------------------------------
    //  Methods to turn the content into different objects
    //  -------------------------------------------------------------------

    /**
     * Turn final string object into normal [String]
     */
    override fun toString(): String {
        var baseMessage = TextUtil.formatText(PluginLocale.getMessage(key, value))

        // Placeholders
        for ((key1, value1) in replacements) {
            val toReplace = Matcher.quoteReplacement(key1)
            baseMessage = baseMessage.replace("{$toReplace}", Matcher.quoteReplacement(value1.toString()))
        }

        return baseMessage
    }

    /**
     * Returns a list of strings made from this string
     */
    fun toList(): List<String> {
        return listOf(*toString().split("\\r?\\n".toRegex()).toTypedArray())
    }

    /**
     * Turn this into a [Component] so it can be used for click events and such
     */
    fun toComponent(): Component {
        var component: Component = Component.text(toString())
        if (clickEvent != null) component = component.clickEvent(clickEvent)
        if (hoverEvent != null) component = component.hoverEvent(hoverEvent)
        return component
    }

    /**
     * Turn this into a list of components seperated by `"\n"`
     */
    fun toComponentList(): List<Component> {
        val components: MutableList<Component> = java.util.ArrayList()
        toList().forEach(Consumer { text: String? ->
            var component: Component = Component.text(
                text!!
            )
            if (clickEvent != null) component = component.clickEvent(clickEvent)
            if (hoverEvent != null) component = component.hoverEvent(hoverEvent)
            components.add(component)
        })
        return components
    }

    //  -------------------------------------------------------------------
    //  Methods to send content out to players
    //  -------------------------------------------------------------------

    /**
     * Format and send the held message to a player.
     * Detect if string is split and send multiple lines
     *
     * @param player player to send the message to
     */
    fun sendMessage(player: CommandSender?) {
        val audience: Audience? = SpigotPlugin.instance!!.audiences?.sender(player!!)
        toComponentList().forEach { audience?.sendMessage(it) }
    }

    /**
     * Format and send the held message to a player as a title message
     *
     * @param sender   command sender to send the message to
     * @param subtitle Subtitle to send
     */
    @JvmOverloads
    fun sendTitle(sender: CommandSender?, subtitle: MistString? = MistString("")) {
        if (sender is Player) {
            if (ServerVersion.atLeast(ServerVersion.V.v1_11)) {
                sender.sendTitle(toString(), subtitle.toString(), 10, 20, 10)
            } else {
                sender.sendTitle(toString(), subtitle.toString())
            }
        } else {
            sendMessage(sender)
        }
    }

    //  -------------------------------------------------------------------
    // Text Builders
    //  -------------------------------------------------------------------

    /**
     * Replace the provided placeholder with the provided object. <br></br>
     * Supports `{value}` placeholders
     *
     * @param placeholder the placeholder to replace
     * @param replacement the replacement object
     * @return the modified Message
     */
    fun toString(placeholder: String, replacement: Any?): MistString {
        this.replacements[placeholder] = replacement ?: "null"
        return this
    }

    /**
     * Replace everything in the string according to this replacement map.
     *
     * @param replacements The map of replacements
     * @return the modified Message
     */
    fun toString(replacements: MutableMap<String, Any>): MistString {
        this.replacements.putAll(replacements)
        return this
    }

    companion object {
        /**
         * Construct a [MistString] from single string.
         * Provides a way of making a nullable miststring where
         * if the string is null the MistString is null to avoid
         * init errors.
         *
         * Should only be used where the string might be nullable. Eg
         * getting from config or something
         */
        fun of(string: String?): MistString? {
            // Faster than iterate list of 1 item
            if (string == null) return null
            return MistString(string)
        }

        /**
         * Construct a [MistString] from multi strings
         */
        fun of(vararg strings: String?): MistString {
            return MistString(strings.joinToString("\n"))
        }

        /**
         * Converts a list of strings to one mist string
         *
         * @param list The list of strings to convert
         * @return One [MistString]
         */
        fun of(list: List<String>): MistString {
            return of(*list.toTypedArray())
        }

        /**
         * Easily turn a list of strings into a list of [MistString]
         *
         * @param list The list to convert
         * @return The list of [MistString] with the original list's values
         */
        fun fromStringList(list: List<String>?): List<MistString> {
            val strings = ArrayList<MistString>()
            list?.forEach(Consumer { string: String -> strings.add(MistString(string)) })
            return strings
        }

        /**
         * Easily turn a list of [MistString] into a list of strings
         *
         * @param list The list to convert
         * @return The list of string with the original list's values
         */
        fun fromList(list: List<MistString>): List<String> {
            val strings = ArrayList<String>()
            list.forEach(Consumer { string: MistString -> strings.add(string.toString()) })
            return strings
        }
    }
}

/**
 * Convert any string to a mist string by
 * invoking "mist" on the end of it
 */
val String.mist: MistString
    get() = MistString(this)