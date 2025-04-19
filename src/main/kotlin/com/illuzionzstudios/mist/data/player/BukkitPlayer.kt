package com.illuzionzstudios.mist.data.player

import com.illuzionzstudios.mist.scheduler.Tickable
import lombok.Getter
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import java.util.*

/**
 * Player that contains Bukkit API methods
 */
@Getter
open class BukkitPlayer(uuid: UUID, name: String?) : AbstractPlayer(uuid, name), Tickable {
    /**
     * To be overridden
     */
    override fun tick() {}
    val bukkitPlayer: Player?
        get() = Bukkit.getPlayer(uuid)

    /**
     * Send a message to the player translating color code.
     *
     * @param message The target message.
     */
    fun sendRawMessage(message: String?) {
        bukkitPlayer!!.sendMessage(ChatColor.translateAlternateColorCodes('&', message!!))
    }

    /**
     * Kick the player from the server with target message.
     *
     * @param message Kick Message.
     */
    fun kick(message: String?) {
        bukkitPlayer!!.kickPlayer(ChatColor.translateAlternateColorCodes('&', message!!))
    }

    /**
     * Teleport The the player to a location.
     *
     * @param location [Location]
     */
    fun teleport(location: Location?) {
        bukkitPlayer!!.teleport(location!!)
    }

    /**
     * If another entity is in range
     *
     * @param entity       Entity
     * @param rangeSquared The blocks to check
     */
    fun isInRange(entity: Entity, rangeSquared: Double): Boolean {
        return isInRange(entity.location, rangeSquared)
    }

    /**
     * If another location is in range
     *
     * @param location     location
     * @param rangeSquared The blocks to check
     */
    fun isInRange(location: Location, rangeSquared: Double): Boolean {
        return if (bukkitPlayer == null) {
            false
        } else location.world == bukkitPlayer!!.world && location.distanceSquared(bukkitPlayer!!.location) < rangeSquared
    }

    val location: Location
        get() = bukkitPlayer!!.location
    val inventory: PlayerInventory
        get() = bukkitPlayer!!.inventory
}