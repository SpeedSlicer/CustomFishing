package com.illuzionzstudios.mist.data.database

import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.OfflinePlayer

/**
 * A database where data can be stored
 */
interface Database {

    /**
     * Get a cached value
     *
     * @param player        The player whos data to get
     * @param queryingField The field we're trying to access
     * @return The value as an [Object] to be cast
     */
    fun getCachedValue(player: AbstractPlayer, queryingField: String): Any? {
        return player.cachedData.getOrDefault(queryingField, null)
    }

    /**
     * Set a cached value
     *
     * @param player        The player whos data to set
     * @param queryingField The field we're trying to access
     * @param value         The value we're setting in the database
     */
    fun setCachedValue(player: AbstractPlayer, queryingField: String, value: Any?) {
        player.cachedData[queryingField] = value
    }

    /**
     * Gets all stored fields for a player
     *
     * @return Field, Value pairs
     */
    fun getFields(player: AbstractPlayer): HashMap<String, Any?>

    /**
     * Get a value from the database
     *
     * @param player        The player whos data to get
     * @param queryingField The field we're trying to access
     * @return The value as an [Object] to be cast
     */
    fun getFieldValue(player: AbstractPlayer, queryingField: String): Any?

    /**
     * Set a value in the database
     *
     * @param player        The player whos data to set
     * @param queryingField The field we're trying to access
     * @param value         The value we're setting in the database
     */
    fun setFieldValue(player: AbstractPlayer, queryingField: String, value: Any?)

    /**
     * Return a list of all saved players in the database
     * Returned as offline player as may not be online
     */
    val savedPlayers: List<OfflinePlayer>?

    /**
     * Open connection to database if necessary
     *
     * @return Connected successfully
     */
    fun connect(): Boolean

    /**
     * Dispose database if needed
     *
     * @return Disconnected successfully
     */
    fun disconnect(): Boolean

    /**
     * Check if the database is open and running
     *
     * @return If database can set and retrieve data
     */
    val isAlive: Boolean
}