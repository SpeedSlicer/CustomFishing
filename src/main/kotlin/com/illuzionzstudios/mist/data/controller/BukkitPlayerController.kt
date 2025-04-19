package com.illuzionzstudios.mist.data.controller

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.controller.PluginController
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.AbstractPlayerData
import com.illuzionzstudios.mist.data.player.BukkitPlayer
import com.illuzionzstudios.mist.data.player.OfflinePlayer
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*

/**
 * Controls all Bukkit players
 */
abstract class BukkitPlayerController<BP : BukkitPlayer?> : AbstractPlayerController<BP>(), Listener, PluginController {

    override fun initialize(plugin: SpigotPlugin) {
        INSTANCE = this
        PLUGIN = plugin
        Bukkit.getServer().pluginManager.registerEvents(this, plugin!!)
        MinecraftScheduler.get()!!.registerSynchronizationService(this)
    }

    override fun stop(plugin: SpigotPlugin) {
        // Save everyone
        for (player in ArrayList(players)) {
            try {
                unregister(player!!)
            } finally {
                try {
                    player!!.unsafeSave()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                players.remove(player)
            }
        }

        // Save offline players
        offlineCache.forEach { (_: UUID?, player: OfflinePlayer) -> player.unsafeSave() }

        // Now disconnect database
        PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
            ?.disconnect()
    }

    fun getPlayer(name: String?): BP? {
        return getPlayer { wp: BP? ->
            wp?.bukkitPlayer?.name.equals(name, ignoreCase = true)
        }.orElse(null)
    }

    fun getPlayer(player: Player): BP? {
        return getPlayer(player.uniqueId)
    }

    fun getPlayer(player: CommandSender): BP? {
        return getPlayer(player.name)
    }

    fun getPlayer(entity: LivingEntity): BP? {
        return getPlayer { wp: BP? ->
            wp?.bukkitPlayer?.entityId == entity.entityId
        }.orElse(null)
    }

    /**
     * Get all nearby players in radius
     *
     * @param location      From location
     * @param squaredRadius Radius
     */
    fun getNearbyPlayers(location: Location, squaredRadius: Int): List<BP> {
        val playersNearby: MutableList<BP> = ArrayList()
        players.forEach { bp ->
            if (bp?.location?.world == location.world && location.distanceSquared(bp?.location!!) <= squaredRadius * squaredRadius) {
                if (!playersNearby.contains(bp)) {
                    playersNearby.add(bp)
                }
            }
        }
        return playersNearby
    }

    /**
     * Pre process logging in
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLogin(event: AsyncPlayerPreLoginEvent) {
        if (!PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
                ?.isAlive!!
        ) {
            // Database down, disable plugin
            Logger.severe("Plugin disabling as could not establish connection to database")
            Bukkit.getPluginManager().disablePlugin(PLUGIN!!)
            return
        }
        val player = handleLogin(event.uniqueId, event.name)
    }

    /**
     * As a player logs in
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = getPlayer(event.player)
        if (event.result != PlayerLoginEvent.Result.ALLOWED) {
            player?.let { handleLogout(it) }
        }
    }

    /**
     * When a player logs off
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLeave(event: PlayerQuitEvent) {
        val player = getPlayer(event.player) ?: return
        handleLogout(player)
    }

    companion object {
        /**
         * Instances
         */
        var INSTANCE: BukkitPlayerController<*>? = null
        var PLUGIN: Plugin? = null
    }
}