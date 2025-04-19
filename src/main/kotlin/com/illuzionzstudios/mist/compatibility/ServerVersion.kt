package com.illuzionzstudios.mist.compatibility

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.exception.PluginException
import com.illuzionzstudios.mist.util.Valid
import lombok.Getter
import org.bukkit.Bukkit

/**
 * Represents the current Minecraft version the plugin loaded on
 */
object ServerVersion {
    /**
     * The string representation of the version, for example V1_13
     */
    private var serverVersion: String? = null

    /**
     * The wrapper representation of the version
     */
    @Getter
    private var current: V? = null

    /**
     * Does the current Minecraft version equal the given version?
     */
    fun equals(version: V): Boolean {
        return compareWith(version) == 0
    }

    /**
     * Is the current Minecraft version older than the given version?
     */
    fun olderThan(version: V): Boolean {
        return compareWith(version) < 0
    }

    /**
     * Is the current Minecraft version newer than the given version?
     */
    fun newerThan(version: V): Boolean {
        return compareWith(version) > 0
    }

    /**
     * Is the current Minecraft version at equals or newer than the given version?
     */
    fun atLeast(version: V): Boolean {
        return equals(version) || newerThan(version)
    }

    // Compares two versions by the number
    private fun compareWith(version: V): Int {
        return try {
            current?.ver?.minus(version.ver)!!
        } catch (t: Throwable) {
            t.printStackTrace()
            0
        }
    }

    /**
     * Return the class versioning such as v1_14_R1
     */
    fun getServerVersion(): String {
        return if (serverVersion == "craftbukkit") "" else serverVersion!!
    }

    /**
     * The version wrapper
     */
    enum class V(
        /**
         * The numeric version (the second part of the 1.x number)
         */
        val ver: Int,
        /**
         * Is this library tested with this Minecraft version?
         */
        @field:Getter private val tested: Boolean = true
    ) {
        v1_19(19), v1_18(18), v1_17(17), v1_16(16), v1_15(15), v1_14(14), v1_13(13), v1_12(12), v1_11(11), v1_10(10), v1_9(9), v1_8(
            8
        ),
        v1_7(7, false), v1_6(6, false), v1_5(5, false), v1_4(4, false), v1_3_AND_BELOW(3, false);

        companion object {
            /**
             * Attempts to get the version from number
             *
             * @throws RuntimeException if number not found
             */
            fun parse(number: Int): V {
                for (v in values()) if (v.ver == number) return v
                throw PluginException("Invalid version number: $number")
            }
        }
        /**
         * Creates new enum for a MC version
         */
    }

    // Initialize the version
    init {
        try {
            val packageName = Bukkit.getServer().javaClass.getPackage().name
            val curr: String = packageName.substring(
                packageName.lastIndexOf('.') + 1
            )
            val hasGatekeeper = "craftbukkit" != curr
            serverVersion = curr
            if (hasGatekeeper) {
                var pos = 0
                for (ch in curr.toCharArray()) {
                    pos++
                    if (pos > 2 && ch == 'R') break
                }
                val numericVersion: String = curr.substring(
                    1,
                    pos - 2
                ).replace("_", ".")
                var found = 0
                for (ch in numericVersion.toCharArray()) if (ch == '.') found++
                Valid.checkBoolean(
                    found == 1,
                    "Minecraft Version checker malfunction. Could not detect your server version. Detected: $numericVersion Current: $curr"
                )
                current = V.parse(
                    numericVersion.split("\\.".toRegex()).toTypedArray()[1]
                        .toInt()
                )
            } else current = V.v1_3_AND_BELOW
        } catch (t: Throwable) {
            Logger.displayError(t, "Error detecting your Minecraft version. Check your server compatibility.")
        }
    }
}