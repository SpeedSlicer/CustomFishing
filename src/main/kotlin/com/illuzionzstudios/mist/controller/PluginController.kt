package com.illuzionzstudios.mist.controller

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import org.bukkit.event.Listener

/**
 * A tag that indicates a controller. A controller is class that handles things
 * of a certain type. Has a start and stop method. Also, a good place to contain
 * listeners for certain functionalities
 *
 *
 * These can usually be an [Enum] object that has a single
 * member, INSTANCE. That way you can simply call Controller.INSTANCE.<method>.
 * Can easily be implemented in Kotlin by using an object
 *
 * @param </method><P> The instance of the plugin this controller is for
 */
interface PluginController : Listener {
    /**
     * Starts up our controller
     *
     * @param plugin The plugin starting the controller
     */
    fun initialize(plugin: SpigotPlugin)

    /**
     * Stops our controller
     *
     * @param plugin The plugin stopping the controller
     */
    fun stop(plugin: SpigotPlugin)
}