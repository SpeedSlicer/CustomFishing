package com.illuzionzstudios.mist.data.player

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.data.PlayerData
import com.illuzionzstudios.mist.data.controller.PlayerDataController
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import lombok.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Player abstraction for data loading/saving
 */
@Getter
abstract class AbstractPlayer(
    /**
     * Identifier of the player
     */
    val uuid: UUID,
    /**
     * Cached name of the player
     */
    val name: String?
) {

    /**
     * Keys in the data to always be replaced
     */
    val keyMetadata = HashMap<String, String>()

    /**
     * Keys in the data that have been modified
     * Used for tracking whether to bother setting data
     */
    val modifiedKeys = CopyOnWriteArrayList<String>()

    /**
     * Local data stored before being saved
     */
    val cachedData = HashMap<String, Any?>()

    /**
     * Player data associated with this player
     */
    val data = ArrayList<AbstractPlayerData<*>>()

    /**
     * If the player data has been loaded into the cache
     */
    private val loaded = AtomicBoolean(false)

    /**
     * Get player data from a class
     *
     * @param type The class type
     * @param <T>  Player data type to return
    </T> */
    open operator fun <T : PlayerData<*>?> get(type: Class<T>): T? {
        for (info in data) {
            if (info.javaClass == type || type.isAssignableFrom(info.javaClass)) {
                return info as T
            }
        }
        return PlayerDataController.Companion.INSTANCE!!.getDefaultData<T>(type)
    }

    /**
     * Set that a key has been modified in data
     *
     * @param key Key to set modified
     */
    fun modifyKey(key: String) {
        if (!modifiedKeys.contains(key)) {
            modifiedKeys.add(key)
        }
    }

    /**
     * Reset any keys we modified
     */
    fun resetModifiedKeys() {
        modifiedKeys.clear()
    }

    /**
     * Called when loading into server
     */
    fun load() {
        // Shouldn't try to load twice
        if (loaded.get()) return

        // Loading stored data into cache
        // Simply insert into cached data
        MinecraftScheduler.Companion.get()!!.desynchronize(Runnable {

            // Async fetch data
            PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
                ?.getFields(this)?.let {
                    cachedData.putAll(
                        it
                    )
                }

            // If stored data is empty, try upload cached data first
            if (PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
                    ?.getFields(this)?.isEmpty()!!
            ) {
                upload()
            }
        }, 0)
        loaded.set(true)
        PlayerDataController.Companion.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()!!
            .applyDefaultData(this)
    }
    /**
     * Save cached data to the database
     *
     *
     * CAN BE PERFORMED ON MAIN THREAD
     *
     * @param doAfter Perform an action afterwards
     */
    /**
     * Shorthand for just saving
     */
    @JvmOverloads
    fun save(doAfter: Consumer<Boolean?>? = null) {
        prepareSaveData()

        // Saving data async
        MinecraftScheduler.Companion.get()!!
            .desynchronize<Boolean>(Callable { upload() }, Consumer { consumer: Future<Boolean> ->
                try {
                    val insert = consumer.get()
                    doAfter?.accept(insert)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                }
            })
    }

    /**
     * Quick save
     */
    fun unsafeSave() {
        prepareSaveData()
        upload()
    }

    /**
     * BE VERY CAREFUL USING THIS
     *
     *
     * This will wipe all data for this current user
     */
    fun clearAllData() {
        // Clear loaded/cached data
        cachedData.forEach { (key: String?, _: Any?) ->
            PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
                ?.setFieldValue(this, key, null)
        }

        // Now clear cached data as not to save it
        cachedData.clear()

        // Don't save any data trying to be saved
        modifiedKeys.clear()
    }

    /**
     * Upload cached data into database
     *
     *
     * NEVER SERVER THREAD SAFE
     */
    fun upload(): Boolean {
        // Upload modified data
        for (key in modifiedKeys) {
            val value = cachedData.getOrDefault(key, null) ?: continue

            // Don't save if nothing to save

            // Set the field in the database
            PlayerDataController.get<AbstractPlayer, AbstractPlayerData<AbstractPlayer>>()?.database
                ?.setFieldValue(this, key, value)
        }
        resetModifiedKeys()
        return true
    }

    /**
     * Get data ready to save
     */
    fun prepareSaveData() {
        data.forEach(Consumer { data: AbstractPlayerData<*> ->
            try {
                data.onSave()
            } catch (e: Exception) {
                Logger.severe("Error occurred while preparing save data")
                e.printStackTrace()
            }
        })
    }
}