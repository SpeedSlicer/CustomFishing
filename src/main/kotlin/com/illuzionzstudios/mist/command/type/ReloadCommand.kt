package com.illuzionzstudios.mist.command.type

import com.illuzionzstudios.mist.command.SpigotSubCommand
import com.illuzionzstudios.mist.command.response.ReturnType
import com.illuzionzstudios.mist.config.locale.PluginLocale
import com.illuzionzstudios.mist.config.locale.mist
import com.illuzionzstudios.mist.plugin.SpigotPlugin

/**
 * Skeleton command that you can add to your [com.illuzionzstudios.mist.command.SpigotCommandGroup]
 * Simply reloads the plugin with all files etc, implemented in [SpigotPlugin.onPluginReload].
 * Invokes [SpigotPlugin.reload]. Should only be implemented in whole plugin main command not
 * per main command group
 *
 * {@permission {plugin.name}.command.reload}
 */
class ReloadCommand : SpigotSubCommand("reload", "rl") {
    override fun onCommand(): ReturnType {
        // Just call this method to reload
        SpigotPlugin.instance!!.reload()

        // Inform
        (PluginLocale.GENERAL_PLUGIN_PREFIX.toString() + PluginLocale.GENERAL_PLUGIN_RELOAD.toString()).mist.sendMessage(sender)
        return ReturnType.SUCCESS
    }

    init {
        setDescription("Reload the plugin configurations")
    }
}