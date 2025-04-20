package com.illuzionzstudios.mist.plugin

import com.illuzionzstudios.mist.command.SpigotCommand
import com.illuzionzstudios.mist.command.SpigotCommandGroup
import com.illuzionzstudios.mist.controller.PluginController
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

class Reloadables {

    /**
     * A list of currently registered listeners for this [com.illuzionzstudios.mist.plugin.SpigotPlugin]
     * Stored in a hashset so we don't double register a listener
     */
    private val listeners = HashSet<Listener>()

    /**
     * A list of registered command groups
     */
    private val commandGroups = HashMap<SpigotCommandGroup, Array<out String>>()

    /**
     * Base plugin commands
     */
    private val commands = HashSet<SpigotCommand>()

    /**
     * Plugin controllers
     */
    private val controllers = HashSet<PluginController>()

    /**
     * Startup all our reloadables
     */
    fun start() {
        controllers.forEach { controller ->
            Bukkit.getServer().pluginManager.registerEvents(controller, SpigotPlugin.instance!!)
            controller.initialize(SpigotPlugin.instance!!)
        }
        listeners.forEach { listener ->
            Bukkit.getServer().pluginManager.registerEvents(
                listener, SpigotPlugin.instance!!
            )
        }
        commandGroups.forEach { (obj, labels) ->
            obj.register(*labels)
        }
        commands.forEach { obj -> obj.register() }
    }

    /**
     * Shutdown all reloadable tasks
     */
    fun shutdown() {
        for (listener in listeners) HandlerList.unregisterAll(listener)
        listeners.clear()
        for (commandGroup in commandGroups.keys) commandGroup.unregister()
        commandGroups.clear()
        for (command in commands) command.unregister()
        commands.clear()
        for (controller in controllers) {
            HandlerList.unregisterAll(controller)
            controller.stop(SpigotPlugin.instance!!)
        }
        controllers.clear()
    }

    /**
     * Register events to Bukkit
     */
    fun registerEvent(listener: Listener) {
        listeners.add(listener)
    }

    /**
     * Register the given command group
     */
    fun registerCommand(group: SpigotCommandGroup, vararg labels: String) {
        commandGroups[group] = labels
    }

    /**
     * Register a base spigot command
     */
    fun registerCommand(command: SpigotCommand) {
        commands.add(command)
    }

    /**
     * Register a controller for startup
     */
    fun registerController(controller: PluginController) {
        controllers.add(controller)
    }

}