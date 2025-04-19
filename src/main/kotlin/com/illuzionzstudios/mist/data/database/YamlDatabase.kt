package com.illuzionzstudios.mist.data.database

import com.illuzionzstudios.mist.config.YamlConfig
import com.illuzionzstudios.mist.data.controller.BukkitPlayerController
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.OfflinePlayer
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.UUIDFetcher.Companion.getName
import java.io.File
import java.util.*
import java.util.function.Consumer

/**
 * Database stored in flat files
 */
class YamlDatabase : Database {

    override fun getFields(player: AbstractPlayer): HashMap<String, Any?> {
        // Local data file
        val dataConfig = YamlConfig(SpigotPlugin.instance!!, "data", player.uuid.toString() + ".yml")
        dataConfig.load()
        val cache = HashMap<String, Any?>()

        // Get keys and load if found value
        dataConfig.defaultSection!!.getKeys(true).forEach(Consumer { path: String ->
            // Check if not null value
            if (dataConfig[path] != null) {
                // Add to cache
                cache[path] = dataConfig[path]
            }
        })
        return cache
    }

    override fun getFieldValue(player: AbstractPlayer, queryingField: String): Any? {
        // Local data file
        val dataConfig = YamlConfig(SpigotPlugin.instance!!, "data", player.uuid.toString() + ".yml")
        dataConfig.load()
        return dataConfig[queryingField]
    }

    override fun setFieldValue(player: AbstractPlayer, queryingField: String, value: Any?) {
        // Local data file
        val dataConfig = YamlConfig(SpigotPlugin.instance!!, "data", player.uuid.toString() + ".yml")
        dataConfig.load()
        dataConfig[queryingField] = value
        dataConfig.saveChanges()
    }

    override val savedPlayers: List<OfflinePlayer>
        get() {
            val savedPlayers: MutableList<OfflinePlayer> = ArrayList()
            val dir = File(SpigotPlugin.instance!!.dataFolder.path + File.separator + "data")
            val files: Array<File> = dir.listFiles() ?: return savedPlayers

            // Can't find players if can't find directory

            // Go through files
            for (file in files) {
                // Get name without extension
                val uuid = file.name.split("\\.".toRegex()).toTypedArray()[0]
                val name = getName(UUID.fromString(uuid))

                // Get offline player
                val player: OfflinePlayer =
                    BukkitPlayerController.Companion.INSTANCE!!.getOfflinePlayer(UUID.fromString(uuid), name)!!

                // Add to cache
                savedPlayers.add(player)
            }
            return savedPlayers
        }

    override fun connect(): Boolean {
        return true
    }

    override fun disconnect(): Boolean {
        return true
    }

    override val isAlive: Boolean
        get() = true
}