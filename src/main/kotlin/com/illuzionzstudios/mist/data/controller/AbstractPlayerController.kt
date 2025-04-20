package com.illuzionzstudios.mist.data.controller

import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.OfflinePlayer
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.scheduler.rate.Rate
import com.illuzionzstudios.mist.scheduler.rate.Sync
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Handles all players on the server
 */
abstract class AbstractPlayerController<P : AbstractPlayer?> {

    /**
     * Cache of loaded players
     */
    protected var players: MutableList<P?> = ArrayList()

    /**
     * Cache of offline players
     */
    protected var offlineCache: MutableMap<UUID?, OfflinePlayer> = ConcurrentHashMap()

    /**
     * Perform an action on all players
     *
     * @param action Consumer to apply
     */
    fun each(action: Consumer<in P?>?) {
        players.forEach(action)
    }

    /**
     * Try find a player from custom filter
     *
     * @param context Predicate to use
     */
    fun getPlayer(context: Predicate<in P?>?): Optional<P?> {
        return players.stream().filter(context).findAny()
    }

    /**
     * Try find player from UUID
     *
     * @param uuid The UUID of player
     */
    fun getPlayer(uuid: UUID): P? {
        return getPlayer { player: P? -> player?.uuid == uuid }.orElse(null)
    }

    /**
     * Create a new instance of a player
     *
     * @param uuid Player's UUID
     * @param name Player's Name
     */
    protected abstract fun newInstance(uuid: UUID, name: String?): P

    /**
     * Autosave all player data
     */
    @Sync(rate = Rate.MIN_04)
    fun autosave() {
        for (player in players) {
            player!!.save()
        }
    }

    /**
     * Login a player to the server and prepare data
     *
     * @param uuid UUID of player
     * @param name Name of player
     * @return The loaded player object
     */
    protected fun handleLogin(uuid: UUID, name: String?): P {
//        Logger.info("Loading %s's player data from database onto server.", name);

        // Load a player's data if set from offline player
        if (offlineCache.containsKey(uuid)) {
            val player = offlineCache[uuid]
            player!!.unsafeSave()
            try {
                for (info in player.data) {
                    info.unregister()
                }
            } finally {
                MinecraftScheduler.Companion.get()!!.dismissSynchronizationService(player)
                offlineCache.remove(player.uuid)
            }
        }
        val player = newInstance(uuid, name)
        player!!.load()
        players.add(player)
        return player
    }

    /**
     * Get an offline player from UUID and name
     *
     * @param uuid UUID of player
     * @param name Name of player
     * @return New OfflinePlayer AbstractPlayer
     */
    fun getOfflinePlayer(uuid: UUID, name: String?): OfflinePlayer? {
        if (offlineCache.containsKey(uuid)) {
            return offlineCache[uuid]
        }
        val player = OfflinePlayer(uuid, name)
        player.load()
        offlineCache[uuid] = player
        return player
    }

    /**
     * Remove an offline player
     *
     * @param uuid UUID of player
     */
    fun removePlayer(uuid: UUID?) {
        offlineCache.remove(uuid)
    }

    /**
     * Log out the player and save all data
     *
     * @param player The player to logout
     */
    protected fun handleLogout(player: P) {
        unregister(player)
        player!!.save()
    }

    /**
     * Unregister the player and get ready for logout
     *
     * @param player AbstractPlayer instance
     */
    fun unregister(player: P) {
        try {
            for (info in player?.data!!) {
                info.unregister()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            MinecraftScheduler.Companion.get()!!.dismissSynchronizationService(player)
            players.remove(player)
        }
    }
}