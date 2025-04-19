package com.illuzionzstudios.mist.util

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import org.apache.commons.lang3.ArrayUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility method for dealing with players
 */
class PlayerUtil {

    companion object {

        /**
         * Stores a list of currently pending title animation tasks to restore the tile to its original one
         */
        private val titleRestoreTasks: Map<UUID, BukkitTask> = ConcurrentHashMap()

        /**
         * Return if the given sender has a certain permission
         */
        fun hasPerm(sender: Permissible?, permission: String?, ignoreOps: Boolean = false): Boolean {
            Valid.checkNotNull(sender, "cannot call hasPerm for null sender!")

            if (permission == null) return true
            if (permission.trim().isEmpty()) return true

            Valid.checkBoolean(
                !permission.contains("{plugin_name}") && !permission.contains("{plugin_name_lower}"),
                "Found {plugin_name} variable calling hasPerm(" + sender + ", " + permission + ")." + "This is now disallowed, contact plugin authors to put " + SpigotPlugin.pluginName
                    .lowercase(Locale.getDefault()) + " in their permission."
            )
            val perm = Permission(permission, if (ignoreOps) PermissionDefault.FALSE else null)
            return sender?.hasPermission(perm) ?: false
        }

        /**
         * Sets pretty much every flag the player can have such as
         * flying etc, back to normal
         *
         * Also sets gamemode to survival
         *
         * Typical usage: Minigame plugins - call this before joining the player to an arena
         *
         * Even disables Essentials god mode.
         *
         * @param removeVanish should we remove vanish from players? most vanish plugins are supported
         */
        @JvmOverloads
        fun normalize(player: Player, cleanInventory: Boolean, removeVanish: Boolean = true) {
            synchronized(titleRestoreTasks) {
                player.gameMode = GameMode.SURVIVAL
                if (cleanInventory) {
                    cleanInventoryAndFood(player)
                    player.resetMaxHealth()
                    try {
                        player.health = 20.0
                    } catch (ignored: Throwable) {
                    }
                    player.isHealthScaled = false
                    for (potion in player.activePotionEffects) player.removePotionEffect(potion.type)
                }
                player.totalExperience = 0
                player.level = 0
                player.exp = 0f
                player.resetPlayerTime()
                player.resetPlayerWeather()
                player.fallDistance = 0f
                try {
                    player.isGlowing = false
                    player.isSilent = false
                } catch (ignored: NoSuchMethodError) {
                }
                player.allowFlight = false
                player.isFlying = false
                player.flySpeed = 0.2f
                player.walkSpeed = 0.2f
                player.canPickupItems = true
                player.velocity = Vector(0, 0, 0)
                player.eject()
                if (player.isInsideVehicle) player.vehicle!!.remove()
                try {
                    for (passenger in player.passengers) player.removePassenger(passenger!!)
                } catch (err: NoSuchMethodError) {
                    /* old MC */
                }
                if (removeVanish) try {
                    if (player.hasMetadata("vanished")) {
                        val plugin = player.getMetadata("vanished")[0].owningPlugin
                        player.removeMetadata("vanished", plugin!!)
                    }
                    for (other in Bukkit.getOnlinePlayers()) if (other.name != player.name && !other.canSee(player)) other.showPlayer(
                        player
                    )
                } catch (err: NoSuchMethodError) {
                    /* old MC */
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        /**
         * Cleans players inventory and restores food levels
         */
        private fun cleanInventoryAndFood(player: Player) {
            player.inventory.setArmorContents(null)
            player.inventory.contents = arrayOfNulls(player.inventory.contents.size)
            try {
                player.inventory.setExtraContents(arrayOfNulls(player.inventory.extraContents.size))
            } catch (err: NoSuchMethodError) {
                /* old MC */
            }
            player.fireTicks = 0
            player.foodLevel = 20
            player.exhaustion = 0f
            player.saturation = 10f
            player.velocity = Vector(0, 0, 0)
        }

        /**
         * Returns true if the player has empty both normal and armor inventory
         */
        fun hasEmptyInventory(player: Player): Boolean {
            val inv = player.inventory.contents
            val armor = player.inventory.armorContents
            val everything = ArrayUtils.addAll(inv, *armor)
            for (i in everything) if (i != null && i.type != Material.AIR) return false
            return true
        }

        /**
         * Return if the player is vanished, see [.isVanished] or if the other player can see him
         */
        fun isVanished(player: Player, otherPlayer: Player?): Boolean {
            return if (otherPlayer != null && !otherPlayer.canSee(player)) true else isVanished(player)
        }

        /**
         * Return true if the player is vanished. We check for Essentials and CMI vanish and also "vanished"
         * metadata value which is supported by most plugins
         */
        fun isVanished(player: Player): Boolean {
            val list = player.getMetadata("vanished")
            for (meta in list) if (meta.asBoolean()) return true
            return false
        }
    }
}