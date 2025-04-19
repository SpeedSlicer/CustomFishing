package com.illuzionzstudios.mist.data

import com.illuzionzstudios.mist.data.controller.PlayerDataController
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.AbstractPlayerData
import org.apache.commons.lang.StringUtils
import java.util.*

/**
 * A field in a players data
 */
class DataField<T>(
    /**
     * Data level this field is apart of
     */
    val playerData: AbstractPlayerData<*>,
    /**
     * Point in file where data is stored
     */
    val field: String,

    val defaultData: T
) {
    /**
     * Local instance of our controller
     */
    private val controller: PlayerDataController<*, *>? =
        PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()

    /**
     * Stored data of this field
     */
    private var localValue: T? = null
        get() {
            if (field == null) {
                localValue = get()
            }
            return field
        }

    /**
     * If the actual field is set
     */
    val isSet: Boolean
        get() {

            // Check value is actually set
            return controller?.database?.getCachedValue(playerData.player!!, queryingField) != null
        }

    /**
     * Get the actual value stored in data
     */
    fun get(): T? {
        val fetch: T? = controller?.database?.getCachedValue(playerData.player!!, queryingField) as T?

        // If not set or null, set default
        if (fetch == null) {
            set(defaultData)

            // Default is null so ultimately return null
            return if (defaultData == null) null else get()

            // Rerun as is set
        }

        // Make sure we update local
        localValue = fetch

        // Finally return found value
        return fetch
    }

    /**
     * Set a value in the field
     *
     * @param value The value of type T to set
     */
    fun set(value: T?) {
        // Current set value if any
        val current: T? = controller?.database?.getCachedValue(playerData.player!!, queryingField) as T?

        // Clear any existing modifications queries if new value is null
        if (value == null) {
            playerData.player?.modifiedKeys?.removeIf { s: String ->
                s.contains(
                    queryingField
                )
            }
        }

        // New value is not the current so we can modify it
        if (current == null || current != value) {
            playerData.player?.modifyKey(queryingField)
        }

        // Update local value
        localValue = value
        controller?.database?.setCachedValue(playerData.player!!, queryingField, value)
    }

    /**
     * Used to update the current querying field ready for upload
     *
     *
     * Used when the get() value modified something (i.e a list from get() was modified)... Basically only used for key tracking
     */
    fun set() {
        val queryingField = queryingField
        playerData.player?.modifyKey(queryingField)
    }

    /**
     * This will grab the final field we use for querying based
     * on local keys to replace
     */
    private val queryingField: String = ""
        get() {
            var queryingField = field

            // Replace necessary metadata
            val globalKeyMetadata = playerData.player?.keyMetadata
            val localKeyMetadata = playerData.localKeys
            val localIter: Iterator<Map.Entry<String, String>> = globalKeyMetadata?.entries?.iterator()!!
            val globalIter: Iterator<Map.Entry<String?, String?>> = localKeyMetadata.entries.iterator()
            while (localIter.hasNext()) {
                val (key, value) = localIter.next()
                if (contains(field, key)) {
                    queryingField = StringUtils.replace(queryingField, key, value)
                }
            }
            while (globalIter.hasNext()) {
                val (key, value) = globalIter.next()
                if (contains(field, key)) {
                    queryingField = StringUtils.replace(queryingField, key, value)
                }
            }
            if (contains(queryingField, "{")) {
                throw RuntimeException("Tried to access player metadata before prepared... " + field + " for " + playerData.player?.name)
            }
            return queryingField
        }

    private fun contains(queryingField: String, s: String?): Boolean {
        return queryingField.lowercase(Locale.getDefault()).contains(s!!.lowercase(Locale.getDefault()))
    }
}