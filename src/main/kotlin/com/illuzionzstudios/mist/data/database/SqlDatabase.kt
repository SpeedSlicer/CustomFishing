package com.illuzionzstudios.mist.data.database

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.data.controller.BukkitPlayerController
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.OfflinePlayer
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.util.UUIDFetcher.Companion.getName
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Use MySql or any Sql database for our data
 *
 *
 * TODO: Make this mess work
 */
class SqlDatabase(
    /**
     * Host server of the database
     */
    private val host: String? = null,

    /**
     * Port to connect to
     */
    private val port: Int = 0,

    /**
     * Database to use
     */
    private val database: String? = null,

    /**
     * Connection username
     */
    private val username: String? = null,

    /**
     * Connection password
     */
    private val password: String? = null,

    /**
     * Table to store our player data
     */
    private val tableName: String = SpigotPlugin.pluginName + "_PlayerData"
) : Database {


    /**
     * Our connection to handle SQL operations
     */
    protected var connection: Connection? = null

    /**
     * If to implement as a sqlite local db
     */
    private val useSqlite = false
    override fun getFields(player: AbstractPlayer): HashMap<String, Any?> {
        val data = HashMap<String, Any?>()
        try {
            val statement = connection!!.prepareStatement("SELECT * FROM $tableName WHERE uuid = ?")
            statement.setString(1, player.uuid.toString())
            val set = statement.executeQuery()
            val meta = set.metaData
            set.next()
            var columnIndex = 0
            while (true) {
                try {
                    columnIndex++
                    // Get field value
                    val queryingField = meta.getColumnName(columnIndex)
                    Logger.debug("Getting all fields: $queryingField")
                    data[queryingField] = set.getObject(queryingField)
                } catch (ex: Exception) {
                    break
                }
            }
        } catch (ex: Exception) {
            Logger.displayError(ex, "Error preforming SQL operation")
        }
        return data
    }

    override fun getFieldValue(player: AbstractPlayer, queryingField: String): Any? {
        try {
            val statement = connection!!.prepareStatement("SELECT ? FROM $tableName WHERE uuid = ?")
            statement.setString(1, queryingField)
            statement.setString(2, player.uuid.toString())
            val set = statement.executeQuery()
            set.next()
            return set.getObject(queryingField)
        } catch (ex: Exception) {
            Logger.displayError(ex, "Error preforming SQL operation")
        }
        return null
    }

    override fun setFieldValue(player: AbstractPlayer, queryingField: String, value: Any?) {
        try {
            // Try create column if doesn't exist
            val createColumn = connection!!.prepareStatement(
                "IF COL_LENGTH(?, ?) IS NULL" +
                        " BEGIN" +
                        " ALTER TABLE " + tableName +
                        " ADD ? varchar(255)" +
                        " END"
            )
            createColumn.setString(1, tableName)
            createColumn.setString(2, queryingField)
            createColumn.setString(3, queryingField)

            // Attempt to set value
            val statement = connection!!.prepareStatement(
                "IF EXISTS (SELECT * FROM " + tableName + " WHERE uuid = ?)" +
                        " BEGIN" +
                        " UPDATE " + tableName + " SET ? = ? WHERE uuid = ?" +
                        " END" +
                        " ELSE" +
                        " BEGIN" +
                        " INSERT INTO " + tableName + " (uuid, ?) VALUES (?, ?)" +
                        " END"
            )
            statement.setString(1, player.uuid.toString())
            statement.setString(2, queryingField)
            statement.setObject(3, value)
            statement.setString(4, player.uuid.toString())
            statement.setString(5, queryingField)
            statement.setString(6, player.uuid.toString())
            statement.setObject(7, value)
            statement.executeUpdate()
            createColumn.executeUpdate()
        } catch (ex: Exception) {
            Logger.displayError(ex, "Error preforming SQL operation")
        }
    }

    // Go through and construct from ids
    override val savedPlayers: List<OfflinePlayer>?
        get() {
            try {
                val players: MutableList<OfflinePlayer> = ArrayList()
                val statement = connection!!.prepareStatement("SELECT * FROM $tableName")
                val set = statement.executeQuery()

                // Go through and construct from ids
                while (set.next()) {
                    val uuidString = set.getString("uuid")
                    val player: OfflinePlayer = BukkitPlayerController.Companion.INSTANCE!!.getOfflinePlayer(
                        UUID.fromString(uuidString),
                        getName(UUID.fromString(uuidString))
                    )!!
                    players.add(player)
                }
                return players
            } catch (ex: Exception) {
                Logger.displayError(ex, "Error preforming SQL operation")
            }
            return null
        }

    override fun connect(): Boolean {
        val status = AtomicBoolean(false)

        // Connect async
        MinecraftScheduler.get()!!.desynchronize({
            try {
                // Don't connect if already connected
                if (connection != null && isAlive) {
                    status.set(false)
                }
                synchronized(this) {
                    if (connection != null && isAlive) {
                        status.set(false)
                    }
                    if (!useSqlite) {
                        // MySQL
                        Class.forName("com.mysql.jdbc.Driver")
                        connection = DriverManager.getConnection(
                            "jdbc:mysql://$host:$port/$database", username, password
                                    + "?useSSL=false"
                        )
                        status.set(true)
                    } else {
                        val dataFolder = File(SpigotPlugin.instance!!.dataFolder, database + ".db")
                        if (!dataFolder.exists()) {
                            try {
                                dataFolder.createNewFile()
                            } catch (ex: Throwable) {
                                Logger.displayError(ex, "Could not create SQLite database")
                            }
                        }

                        // SQLite
                        Class.forName("org.sqlite.JDBC")
                        connection = DriverManager.getConnection("jdbc:sqlite:$dataFolder")
                        status.set(true)
                    }
                }
            } catch (ex: Exception) {
                Logger.displayError(ex, "Couldn't connect to database")
                status.set(false)
            }
            if (isAlive) {
                Logger.debug("Creating Table")
                try {
                    val statement = connection!!.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                                " `uuid` varchar(255) NOT NULL," +
                                "PRIMARY KEY  (`uuid`))"
                    )
                    statement.executeUpdate()
                } catch (ex: Exception) {
                    Logger.displayError(ex, "Error preforming SQL operation")
                }
            }
        }, 0)
        return status.get()
    }

    override fun disconnect(): Boolean {
        return try {
            connection!!.close()
            true
        } catch (ex: Exception) {
            false
        }
    }

    // Check async
    override val isAlive: Boolean
        get() {
            val status = AtomicBoolean(false)

            // Check async
            MinecraftScheduler.get()!!.desynchronize({
                try {
                    status.set(!connection!!.isClosed)
                } catch (ex: Exception) {
                    status.set(false)
                }
            }, 0)
            return status.get()
        }
}