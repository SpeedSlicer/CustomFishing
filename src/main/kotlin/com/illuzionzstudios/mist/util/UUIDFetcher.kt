package com.illuzionzstudios.mist.util

import com.google.gson.GsonBuilder
import com.illuzionzstudios.mist.util.UUIDFetcher
import com.mojang.util.UUIDTypeAdapter
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Consumer

/**
 * External UUID library included for our plugin. Runs Async
 */
class UUIDFetcher(
    /**
     * Cached name
     */
    private val name: String? = null,

    /**
     * Cached [UUID]
     */
    private val id: UUID? = null
) {

    companion object {
        private val gson = GsonBuilder().registerTypeAdapter(UUID::class.java, UUIDTypeAdapter()).create()
        private const val UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d"
        private const val NAME_URL = "https://api.mojang.com/user/profiles/%s/names"
        private val uuidCache: MutableMap<String, UUID?> = HashMap()
        private val nameCache: MutableMap<UUID?, String?> = HashMap()
        private val pool = Executors.newCachedThreadPool()

        /**
         * Fetches the uuid asynchronously and passes it to the consumer
         *
         * @param name   The name
         * @param action Do what you want to do with the uuid her
         */
        fun getUUID(name: String, action: Consumer<UUID?>) {
            pool.execute { action.accept(getUUID(name)) }
        }

        /**
         * Fetches the uuid synchronously and returns it
         *
         * @param name The name
         * @return The uuid
         */
        fun getUUID(name: String): UUID? {
            return getUUIDAt(name, System.currentTimeMillis())
        }

        /**
         * Fetches the uuid synchronously for a specified name and time and passes the result to the consumer
         *
         * @param name      The name
         * @param timestamp Time when the player had this name in milliseconds
         * @param action    Do what you want to do with the uuid her
         */
        fun getUUIDAt(name: String, timestamp: Long, action: Consumer<UUID?>) {
            pool.execute { action.accept(getUUIDAt(name, timestamp)) }
        }

        /**
         * Fetches the uuid synchronously for a specified name and time
         *
         * @param name      The name
         * @param timestamp Time when the player had this name in milliseconds
         */
        fun getUUIDAt(name: String, timestamp: Long): UUID? {
            var name = name
            name = name.lowercase(Locale.getDefault())
            if (uuidCache.containsKey(name)) {
                return uuidCache[name]
            }
            try {
                val connection =
                    URL(String.format(UUID_URL, name, timestamp / 1000)).openConnection() as HttpURLConnection
                connection.readTimeout = 5000
                val data =
                    gson.fromJson(
                        BufferedReader(InputStreamReader(connection.inputStream)),
                        UUIDFetcher::class.java
                    )
                uuidCache[name] = data.id
                nameCache[data.id] = data.name
                return data.id
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * Fetches the name asynchronously and passes it to the consumer
         *
         * @param uuid   The uuid
         * @param action Do what you want to do with the name her
         */
        fun getName(uuid: UUID?, action: Consumer<String?>) {
            pool.execute { action.accept(getName(uuid)) }
        }

        /**
         * Fetches the name synchronously and returns it
         *
         * @param uuid The uuid
         * @return The name
         */
        @JvmStatic
        fun getName(uuid: UUID?): String? {
            if (nameCache.containsKey(uuid)) {
                return nameCache[uuid]
            }
            try {
                val connection =
                    URL(
                        String.format(
                            NAME_URL,
                            UUIDTypeAdapter.fromUUID(uuid)
                        )
                    ).openConnection() as HttpURLConnection
                connection.readTimeout = 5000
                val nameHistory = gson.fromJson(
                    BufferedReader(InputStreamReader(connection.inputStream)),
                    Array<UUIDFetcher>::class.java
                )
                val currentNameData = nameHistory[nameHistory.size - 1]
                uuidCache[currentNameData.name!!.lowercase(Locale.getDefault())] = uuid
                nameCache[uuid] = currentNameData.name
                return currentNameData.name
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}