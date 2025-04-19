package com.illuzionzstudios.mist.config

import com.illuzionzstudios.mist.Mist
import com.illuzionzstudios.mist.plugin.SpigotPlugin

/**
 * An implementation of basic plugin settings. This handles settings that
 * all plugins with this library will have. For instance locale, main command etc.
 * Typically, used as the "config.yml"
 *
 *
 * This should be implemented by our [SpigotPlugin] and
 * define our own [ConfigSetting] specific to the plugin
 */
abstract class PluginSettings(plugin: SpigotPlugin) : YamlConfig(plugin, Mist.SETTINGS_NAME) {

    /**
     * Invoked to load all other custom settings that we implement
     * in our own [PluginSettings]
     */
    abstract fun loadSettings()

    companion object {

        /**
         * The current loaded [PluginSettings] instance
         */
        var SETTINGS_FILE: YamlConfig? = null

        @JvmStatic
        protected val GENERAL_GROUP = ConfigSettings()

        //  -------------------------------------------------------------------------
        //  Main config settings provided by default
        //  -------------------------------------------------------------------------

        /**
         * The locale type to use, for instance
         * "en_US"
         */
        var LOCALE = GENERAL_GROUP.create(
            "settings.locale", "en_US",
            "The language file to use for the plugin",
            "More language files (if available) can be found in the plugins locale folder."
        )

        /**
         * Load these [PluginSettings] into the server, setting values
         * if not there, or loading the values into memory
         *
         *
         * Call in the [SpigotPlugin.onPluginEnable] to load plugin settings
         *
         * @param settings The instance of [PluginSettings] to load
         */
        fun loadSettings(settings: PluginSettings) {
            SETTINGS_FILE = settings

            // Load settings file
            settings.load()
            GENERAL_GROUP.load()

            // Load our other custom settings
            settings.loadSettings()
            settings.saveChanges()
        }
    }
}