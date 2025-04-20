package com.illuzionzstudios.mist.config.locale

import com.illuzionzstudios.mist.config.PluginSettings
import com.illuzionzstudios.mist.config.YamlConfig
import com.illuzionzstudios.mist.plugin.SpigotPlugin

/**
 * Loaded file that handles all game language
 * Also supports different languages apart from English
 * Implement our own instance in our plugin with the different values
 *
 * We provide some common messages but can implement your own
 */
abstract class PluginLocale(
    plugin: SpigotPlugin,
) : YamlConfig(plugin, "locales", PluginSettings.LOCALE.string + ".lang") {
    companion object {
        /**
         * Messages loaded on startup
         */
        @JvmStatic
        protected val STARTUP_GROUP = MistStringGroup()

        /**
         * This is a cache of all loaded translations for a key. If we go to get a value by
         * a key it will first check here. If not found it will look through the file and if found
         * update it here. If not found anywhere it will simply return the default.
         */
        private val localeCache = HashMap<String, String?>()

        /**
         * The current loaded [PluginLocale] instance
         */
        private var LOCALE_FILE: YamlConfig? = null

        //  -------------------------------------------------------------------------
        //  Main messages provided by default
        //  If these are found in the locale, we use those, otherwise use these
        //  defaults
        //  -------------------------------------------------------------------------

        /**
         * The prefix to use before certain messages
         */
        var GENERAL_PLUGIN_PREFIX = createString("general.prefix", "&d&lMist Plugin &8\\u00BB&7")

        /**
         * Message sent when reloading the plugin. Used in [com.illuzionzstudios.mist.command.type.ReloadCommand]
         */
        var GENERAL_PLUGIN_RELOAD = createString("general.reload", "&7Reloaded the plugin (Configuration files & controllers)")

        /**
         * If a command sender that isn't a player tries to execute a command
         */
        var COMMAND_PLAYER_ONLY = createString("command.player-only", "&cYou must be a player to execute this command.")

        /**
         * If the player doesn't have a required permission
         */
        var COMMAND_NO_PERMISSION = createString("command.no-permission", "&cYou must have the permission {permission} to do this.")

        /**
         * Sent when the executor provides too little arguments
         */
        var COMMAND_INVALID_USAGE = createString("command.invalid-usage", "&cInvalid usage. Try /{label} {args}")

        /**
         * If they try use a sub command that doesn't exist
         */
        var COMMAND_INVALID_SUB = createString("command.invalid-sub", "&cThat command doesn't exist. Try /{label} help")

        /**
         * The optional arguments label
         */
        var COMMAND_LABEL_OPTIONAL_ARGS = createString("command.label-optional-args", "optional arguments")

        /**
         * The required arguments label
         */
        var COMMAND_LABEL_REQUIRED_ARGS = createString("command.label-required-args", "required arguments")

        /**
         * The keys below are shown as hover tooltip on /command help menu.
         */
        var HELP_TOOLTIP_DESCRIPTION = createString("command.help.description", "&7Description: &f{description}")
        var HELP_TOOLTIP_PERMISSION = createString("command.help.permission", "&7Permission: &f{permission}")
        var HELP_TOOLTIP_USAGE = createString("command.help.usage", "&7Usage: &f")

        /**
         * Message sent prompting to enter a value
         */
        var CONFIG_ENTER_VALUE = createString("config.enter-value", "&7Enter a new value to set (Type 'cancel' to cancel)")

        /**
         * Name of the confirm icon in the confirm inventory
         */
        var INTERFACE_CONFIRM_CONFIRM_NAME = createString("interface.confirm.confirm.name", "&a&lConfirm")

        /**
         * Lore of the confirm icon in the confirm inventory
         */
        var INTERFACE_CONFIRM_CONFIRM_LORE = createString("interface.confirm.confirm.lore", "&7&o(Click to confirm)")

        /**
         * Name of the deny icon in the confirm inventory
         */
        var INTERFACE_CONFIRM_DENY_NAME = createString("interface.confirm.deny.name", "&c&lDeny")

        /**
         * Lore of the deny icon in the confirm inventory
         */
        var INTERFACE_CONFIRM_DENY_LORE = createString("interface.confirm.deny.lore", "&7&o(Click to deny)")

        /**
         * The message if a new version is found
         */
        var UPDATE_AVAILABLE = createString(
            "update.available", """
     &2You're on an &a{status}&2 version of &a{plugin_name}&2.
     &2Current version: &a{current}&2; Latest version: &a{new}
     &2URL: &ahttps://spigotmc.org/resources/{resource_id}/.
     """.trimIndent()
        )
        
        fun createString(node: String, vararg defaults: String): MistString {
            return STARTUP_GROUP.create(node, *defaults)
        }
        
        /**
         * Load the [PluginLocale] into the server, setting values
         * if not there, or loading the values into memory
         *
         *
         * Call in the [SpigotPlugin.onPluginEnable] to load plugin locale
         *
         * @param settings The instance of [PluginLocale] to load
         */
        fun loadLocale(settings: PluginLocale) {
            // Set instance
            LOCALE_FILE = settings

            // Load settings loadLocale
            settings.load()
            // Load locale groups
            STARTUP_GROUP.load()

            // Load our other custom settings
            settings.saveChanges()

            // Reset cache
            invalidateCache()
        }

        /**
         * Retrieve a message from the locale
         *
         * @param key Node key to search for
         * @return The found message
         */
        @Deprecated("Should always provide a default value for redundancy")
        fun getMessage(key: String): String? {
            return getMessage(key, key)
        }

        /**
         * Retrieve a message from the locale. Will first look in the cache to see if
         * they key is there. If not found it will look for it in the file and then update
         * the cache. If couldn't be found anywhere will return {@param def}. Should
         * be defined in lang file manually anyway.
         *
         * @param key Node key to search for
         * @param def The default message to use if not found
         * @return The found message or default
         */
        fun getMessage(key: String, def: String?): String? {
            // If no key just return default so doesn't get set in file.
            if (key.equals("", ignoreCase = true)) return def

            // Try find in cache
            if (localeCache.containsKey(key)) return localeCache[key]

            // Try find in locale file
            if (LOCALE_FILE!!.isSet(key)) {
                val foundValue = LOCALE_FILE!!.getString(key)
                // Update cache
                localeCache[key] = foundValue
                return foundValue
            }

            // Not found so add default
            LOCALE_FILE?.addDefault(key, def)
            LOCALE_FILE?.saveChanges()

            // Return default
            return def
        }

        /**
         * Clear the locale cache. Usually good for a plugin reload
         */
        private fun invalidateCache() {
            localeCache.clear()
        }
    }
}