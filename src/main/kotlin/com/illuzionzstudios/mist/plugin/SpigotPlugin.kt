package com.illuzionzstudios.mist.plugin

import com.illuzionzstudios.mist.Logger.Companion.displayError
import com.illuzionzstudios.mist.Logger.Companion.info
import com.illuzionzstudios.mist.Logger.Companion.warn
import com.illuzionzstudios.mist.Mist
import com.illuzionzstudios.mist.command.SpigotCommand
import com.illuzionzstudios.mist.command.SpigotCommandGroup
import com.illuzionzstudios.mist.command.temporary.TemporaryCommandManager
import com.illuzionzstudios.mist.config.PluginSettings
import com.illuzionzstudios.mist.config.locale.PluginLocale
import com.illuzionzstudios.mist.data.controller.BukkitPlayerController
import com.illuzionzstudios.mist.data.controller.PlayerDataController
import com.illuzionzstudios.mist.data.database.Database
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.AbstractPlayerData
import com.illuzionzstudios.mist.data.player.BukkitPlayer
import com.illuzionzstudios.mist.model.UpdateChecker
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.scheduler.bukkit.BukkitScheduler
import com.illuzionzstudios.mist.ui.InterfaceController
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

/**
 * Represents an instance of a custom spigot plugin with our
 * "Mist" functionality
 *
 * The plugin is only designed to work on versions
 * {@version 1.8.8} to {@version 1.18.2}
 */
abstract class SpigotPlugin(var metricsId: Int = 0) : JavaPlugin(), Listener {

    /**
     * An easy way to handle listeners for reloading
     */
    val reloadables = Reloadables()

    var audiences: BukkitAudiences? = null
    private set

    /**
     * If to check for plugin updates on load
     */
    var isCheckUpdates = false

    /**
     * The main command for this plugin, can be `null`
     */
    var mainCommand: SpigotCommandGroup? = null

    /**
     * Our player controller for our player data objects
     * Also used as a flag if we want to use player data
     */
    private var playerController: BukkitPlayerController<out BukkitPlayer>? = null

    /**
     * Called when the plugin is loaded into the server
     */
    abstract fun onPluginLoad()

    /**
     * Called before the actual plugin is enabled
     */
    abstract fun onPluginPreEnable()

    /**
     * Called when the plugin is finally being enabled
     */
    abstract fun onPluginEnable()

    /**
     * Called when the plugin is being disabled
     */
    abstract fun onPluginDisable()

    /**
     * Called before the plugin is reloaded
     * This means config reloads, /reload, or if the plugin is
     * disabled and then enabled while the server is running
     */
    abstract fun onPluginPreReload()

    /**
     * Called after the plugin is reloaded
     * This means config reloads, /reload, or if the plugin is
     * disabled and then enabled while the server is running
     */
    abstract fun onPluginReload()

    /**
     * This method is called when registering things like listeners.
     *
     *
     * In your plugin use it to register commands, events etc.
     */
    abstract fun onRegisterReloadables()

    override fun onLoad() {
        try {
            // Try set instance
            instance
        } catch (ex: Throwable) {
            // If can't set manually
            instance = this
            throw ex
        }
        onPluginLoad()
    }

    override fun onEnable() {
        if (!isEnabled) return

        // Pre start the plugin
        onPluginPreEnable()

        // Return if plugin pre start indicated a fatal problem
        if (!isEnabled) return

        // Main enabled
        try {
            // Startup logo
            if (startupLogo != null) Arrays.stream(startupLogo).sequential().forEach { info(it) }

            // Track metrics
            if (this.metricsId != 0)
                Metrics(this, metricsId)

            // Audiences
            this.audiences = BukkitAudiences.create(this)

            // Load settings and locale
            // Try save config if found
            PluginSettings.loadSettings(pluginSettings)
            PluginLocale.loadLocale(pluginLocale)

            // Enable our scheduler
            BukkitScheduler(this).initialize()
            reloadables.registerController(InterfaceController)
            reloadables.registerController(TemporaryCommandManager)
            reloadables.registerController(Hooks)
            onRegisterReloadables()

            // Check update
            UpdateChecker.checkVersion(Bukkit.getServer().consoleSender)
            onPluginEnable()

            // Register main events
            registerListener(this)
            // Start reloadables
            reloadables.start()

            // Connect to database and allow player data
            if (playerController != null) {
                playerController!!.initialize(this)
            }
        } catch (ex: Throwable) {
            displayError(ex, "Error enabling plugin")

            // Errors on startup could break the plugin,
            // so just kill it
            this.isEnabled = false
            isEnabled = false
        }
    }

    override fun onDisable() {
        // Don't shutdown if wasn't enabled
        if (!isEnabled) return
        try {
            onPluginDisable()
        } catch (t: Throwable) {
            warn("Plugin might not shut down property. Got " + t.javaClass.simpleName + ": " + t.message)
            t.printStackTrace()
        }

        unregisterReloadables()
        this.audiences?.close()

        // Try save all player data
        if (playerController != null) {
            playerController!!.stop(this)
        }
        Objects.requireNonNull(instance, "Plugin $name has already been shutdown!")
        instance = null
    }

    /**
     * Attempt to reload the plugin
     */
    fun reload() {
        info("Reloading plugin $pluginName v$pluginVersion")
        isReloading = true
        try {
            unregisterReloadables()
            onPluginPreReload()

            // Load settings and locale
            // Try save config if found
            PluginSettings.loadSettings(pluginSettings)
            PluginLocale.loadLocale(pluginLocale)

            // Restart tickers
            MinecraftScheduler.get()!!.initialize()

            // Reload controllers etc
            reloadables.registerController(InterfaceController)
            reloadables.registerController(TemporaryCommandManager)
            reloadables.registerController(Hooks)
            onPluginReload()
            onRegisterReloadables()

            // Register main events
            registerListener(this)
            reloadables.start()
            if (playerController != null) {
                playerController!!.initialize(this)
            }
        } catch (ex: Throwable) {
            displayError(ex, "Error reloading plugin")
        } finally {
            isReloading = false
        }
    }

    /**
     * Un register stuff when reloading
     */
    private fun unregisterReloadables() {
        // Stop ticking all tasks
        MinecraftScheduler.get()!!.stopInvocation()
        mainCommand = null
        reloadables.shutdown()
    }

    //  -------------------------------------------------------------------------
    //  Additional features of our main plugin
    //  -------------------------------------------------------------------------

    /**
     * @param listener Register a listener for this plugin
     */
    protected fun registerListener(listener: Listener?) {
        reloadables.registerEvent(listener!!)
    }

    /**
     * The start-up fancy logo
     *
     * @return null by default
     */
    val startupLogo: Array<String>?
        get() = null

    /**
     * Opt in to using custom player data.
     *
     * This is optional because we don't want to load and save
     * data if we don't need it
     *
     * @param playerClass      The class for our custom player data
     * @param database         The type of database to use to save data
     * @param playerController Our custom player controller for operations
     */
    protected fun <BP : BukkitPlayer> initializePlayerData(
        playerClass: Class<out BukkitPlayer?>?, database: Database?,
        playerController: BukkitPlayerController<BP>?
    ) {
        this.playerController = playerController
        PlayerDataController<AbstractPlayer, AbstractPlayerData<*>>().initialize(playerClass, database)
    }

    /**
     * @return Our custom implementation of [PluginSettings]
     */
    abstract val pluginSettings: PluginSettings

    /**
     * @return Get the [PluginLocale] instance being used for this plugin
     */
    abstract val pluginLocale: PluginLocale

    /**
     * @return Plugin's id for update checking on spigot
     */
    abstract val pluginId: Int

    /**
     * The main color for our plugin
     */
    abstract val pluginColor: ChatColor

    /**
     * @param command Register a [SpigotCommand] for this plugin
     */
    protected fun registerCommand(command: SpigotCommand?) {
        reloadables.registerCommand(command!!)
    }

    /**
     * @param command Register a [SpigotCommand] for this plugin
     */
    protected fun registerCommand(command: SpigotCommandGroup?, vararg labels: String) {
        reloadables.registerCommand(command!!, *labels)
    }

    /**
     * @param command Register a new [SpigotCommandGroup]
     */
    protected fun registerMainCommand(command: SpigotCommandGroup?, vararg labels: String) {
        this.mainCommand = command
        reloadables.registerCommand(command!!, *labels)
    }

    companion object {
        /**
         * If our [SpigotPlugin] is currently reloading
         */
        @Volatile
        private var isReloading = false

        /**
         * Return our instance of the [SpigotPlugin]
         *
         * Should be overridden in your own [SpigotPlugin] class
         * as a way to implement your own methods per plugin
         *
         * @return This instance of the plugin
         */
        @Volatile
        var instance: SpigotPlugin? = null
            get() {
                // Assign if null
                if (field == null) {
                    field = getPlugin(SpigotPlugin::class.java)
                    Objects.requireNonNull(field, "Cannot create instance of plugin. Did you reload?")
                }
                return field
            }

        /**
         * Get if the instance that is used across the library has been set. Normally it
         * is always set, except for testing.
         *
         * @return if the instance has been set.
         */
        fun hasInstance(): Boolean {
            return instance != null
        }

        /**
         * @return The name of the [SpigotPlugin] from the plugin description
         */
        val pluginName: String
            get() = instance!!.description.name

        /**
         * @return The version of the [SpigotPlugin] from the plugin description
         */
        val pluginVersion: String
            get() = instance!!.description.version

        /**
         * Shortcut for getFile()
         *
         * @return plugin's jar file
         */
        val source: File
            get() = instance!!.file

        /**
         * Check if a given label is for the main plugin command
         *
         * @param label Label to check
         * @return If it's an aliases for the main command
         */
        fun isMainCommand(label: String?): Boolean {
            return instance!!.mainCommand != null && instance!!.mainCommand?.label
                .equals(label, ignoreCase = true)
        }
    }
}