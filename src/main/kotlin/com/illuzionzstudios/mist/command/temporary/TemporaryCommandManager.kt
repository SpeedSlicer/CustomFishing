package com.illuzionzstudios.mist.command.temporary

import com.illuzionzstudios.mist.command.SpigotCommand
import com.illuzionzstudios.mist.command.response.ReturnType
import com.illuzionzstudios.mist.controller.PluginController
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.scheduler.rate.Rate
import com.illuzionzstudios.mist.scheduler.rate.Sync

object TemporaryCommandManager: PluginController {

    private val TEMPORARY_COMMAND_CAPACITY = 500

    val registeredTemp: LinkedHashMap<String?, TemporaryCommand?> =
        object : LinkedHashMap<String?, TemporaryCommand?>(TEMPORARY_COMMAND_CAPACITY) {
            override fun removeEldestEntry(eldest: Map.Entry<String?, TemporaryCommand?>): Boolean {
                return size > TEMPORARY_COMMAND_CAPACITY
            }
        }

    override fun initialize(plugin: SpigotPlugin) {
        MinecraftScheduler.get()?.registerSynchronizationService(this)
        plugin.reloadables.registerCommand(TemporaryPlayerCommand())
    }

    override fun stop(plugin: SpigotPlugin) {
        MinecraftScheduler.get()?.dismissSynchronizationService(this)
        registeredTemp.clear()
    }

    @Sync(rate = Rate.MIN_01)
    fun cleanRegistered() {
        if (registeredTemp.size > 1000) {
            registeredTemp.clear()
        }
    }

    class TemporaryPlayerCommand : SpigotCommand("misttemp") {
        override fun onCommand(): ReturnType {
            try {
                val command: TemporaryCommand? = registeredTemp[args[0]]

                if (command != null && player != null) {
                    command.run(player!!)
                    MinecraftScheduler.get()!!.synchronize { registeredTemp.remove(command.label) }
                }
            } catch (error: Exception) {}

            return ReturnType.SUCCESS
        }
    }
}