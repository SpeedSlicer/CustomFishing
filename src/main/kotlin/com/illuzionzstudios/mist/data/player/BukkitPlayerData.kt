package com.illuzionzstudios.mist.data.player

import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/*
 * Player data which can be a bukkit listener
 */
abstract class BukkitPlayerData<BP : BukkitPlayer?>(player: BP) : AbstractPlayerData<BP>(player), Listener {
    private var eventsRegistered = false
    override fun unregister() {
        super.unregister()
        if (eventsRegistered) {
            HandlerList.unregisterAll(this)
        }
    }

    protected fun registerEvents(plugin: Plugin?) {
        Bukkit.getServer().pluginManager.registerEvents(this, plugin!!)
        eventsRegistered = true
    }
}