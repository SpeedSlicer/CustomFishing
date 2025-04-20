package com.illuzionzstudios.mist.command

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.util.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.SimpleCommandMap

/**
 * Util class for registering commands into the server's command map.
 * Not in main util package as internal util
 */
class CommandUtil {

    companion object {

        /**
         * Injects an existing command into the command map
         *
         * @param command The [Command] instance to register
         */
        fun registerCommand(command: Command) {
            val commandMap = commandMap ?: return
            commandMap.register(command.label, command)
            Valid.checkBoolean(
                command.isRegistered,
                "Command /" + command.label + " could not have been registered properly!"
            )
        }
        /**
         * Removes a command by its label from command map, optionally can also remove
         * aliases
         *
         * @param label         the label
         * @param removeAliases also remove aliases?
         */
        @JvmOverloads
        fun unregisterCommand(label: String, removeAliases: Boolean = true) {
            try {
                // Unregister the commandMap from the command itself.
                val command = Bukkit.getPluginCommand(label)
                if (command != null) {
                    val commandField = Command::class.java.getDeclaredField("commandMap")
                    commandField.isAccessible = true
                    if (command.isRegistered) command.unregister((commandField[command] as CommandMap))
                }

                // Delete command + aliases from server's command map.
                val f = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                f.isAccessible = true
                val cmdMap = f[commandMap] as MutableMap<*, *>
                cmdMap.remove(label)
                if (command != null && removeAliases) for (alias in command.aliases) cmdMap.remove(alias)
            } catch (ex: ReflectiveOperationException) {
                Logger.displayError(ex, "Failed to unregister command /$label")
            }
        }

        /**
         * @return Server's command map
         */
        private val commandMap: SimpleCommandMap?
            get() {
                try {
                    return ReflectionUtil.getOBCClass("CraftServer").getDeclaredMethod("getCommandMap")
                        .invoke(Bukkit.getServer()) as SimpleCommandMap?
                } catch (ex: ReflectiveOperationException) {
                    Logger.displayError(ex, "Couldn't load server command map")
                }
                return null
            }
    }
}