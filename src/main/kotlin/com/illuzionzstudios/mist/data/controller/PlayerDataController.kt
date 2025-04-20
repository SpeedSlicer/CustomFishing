package com.illuzionzstudios.mist.data.controller

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.data.PlayerData
import com.illuzionzstudios.mist.data.database.Database
import com.illuzionzstudios.mist.data.player.AbstractPlayer
import com.illuzionzstudios.mist.data.player.AbstractPlayerData
import com.illuzionzstudios.mist.util.ReflectionUtil
import org.apache.commons.lang.reflect.ConstructorUtils
import java.lang.reflect.InvocationTargetException

/**
 * Master class for handling all data between players
 */
class PlayerDataController<P : AbstractPlayer?, PD : AbstractPlayerData<*>?> {

    /**
     * Registered default data
     */
    private val defaultData = ArrayList<Class<out AbstractPlayerData<*>>>()

    /**
     * The database to use for player data
     */
    var database: Database? = null

    /**
     * The player class we use for our operations
     */
    private var playerClass: Class<out AbstractPlayer>? = null

    /**
     * Register default player data
     *
     * @param data The data class to register
     */
    fun registerDefaultData(data: Class<out AbstractPlayerData<*>>) {
        defaultData.add(data)
    }

    /**
     * Let our data controller be used
     *
     * @param playerClass The player class registered for data
     * @param database    The database to use for data
     */
    fun initialize(playerClass: Class<out AbstractPlayer>?, database: Database?) {
        INSTANCE = this
        this.playerClass = playerClass
        this.database = database

        // Connect now
        if (this.database!!.connect()) {
            Logger.info("Connected to database successfully")
        }
    }

    /**
     * Try and find default data for a player
     *
     * @param player The player to find
     * @param clazz  The class for the data
     * @param <T>    Return the player data
    </T> */
    fun <T : PlayerData<*>?> getDefaultData(player: AbstractPlayer, clazz: Class<T>?): T? {
        try {
            val data = ConstructorUtils.getMatchingAccessibleConstructor(
                clazz, arrayOf<Class<*>>(
                    AbstractPlayer::class.java
                )
            ).newInstance(player) as T
            player.data.add(data as AbstractPlayerData<*>)
            return data
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * Apply default data to a player
     *
     * @param player The player to set default data for
     */
    fun applyDefaultData(player: AbstractPlayer) {
        addInfo@ for (pi in defaultData) {
            try {
                for (data in player.data) {
                    if (data.javaClass.isAssignableFrom(pi)) {
                        continue@addInfo
                    }
                }
                val constructor = ConstructorUtils.getMatchingAccessibleConstructor(pi, arrayOf<Class<*>?>(playerClass))
                    ?: continue
                player.data.add(ReflectionUtil.instantiate(constructor, player) as AbstractPlayerData<*>)
            } catch (e: Exception) {
                if (!(e is IllegalArgumentException || e is IllegalAccessException || e is InstantiationException || e is InvocationTargetException)) {
                }
            }
        }
    }

    /**
     * Get the default data of a data class
     *
     * @param type The class of the player data
     * @param <T>  Return a player data type
    </T> */
    fun <T : PlayerData<*>?> getDefaultData(type: Class<T>): T? {
        for (pi in defaultData) {
            if (pi == type) {
                try {
                    return pi.getDeclaredConstructor().newInstance() as T
                } catch (ignored: InstantiationException) {
                } catch (ignored: IllegalAccessException) {
                } catch (ignored: InvocationTargetException) {
                } catch (ignored: NoSuchMethodException) {
                }
            }
        }
        return null
    }

    companion object {
        /**
         * Instance of the data controller
         */
        var INSTANCE: PlayerDataController<*, *>? = null

        /**
         * Get an instance of our controller
         */
        fun <P : AbstractPlayer?, PD : AbstractPlayerData<P>?> get(): PlayerDataController<P, PD>? {
            return INSTANCE as PlayerDataController<P, PD>?
        }
    }
}