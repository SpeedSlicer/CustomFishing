package com.illuzionzstudios.mist

import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.util.TextUtil
import org.bukkit.conversations.Conversable
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.stream.Collectors
import java.util.stream.StreamSupport

class Mist {

    companion object {
        /**
         * The name (with extension) for the main config file
         */
        const val SETTINGS_NAME: String = "config.yml"

        /**
         * Amount of ticks for an invocation to pause before warning
         */
        const val TIME_WARNING_THRESHOLD: Long = 100

        /**
         * Sends the conversable a message later
         */
        @JvmStatic
        fun tellLaterConversing(conversable: Conversable, message: String?, delayTicks: Int) {
            MinecraftScheduler.get()?.synchronize({ tellConversing(conversable, message) }, delayTicks.toLong())
        }

        /**
         * Sends the conversable player a colorized message
         */
        @JvmStatic
        fun tellConversing(conversable: Conversable, message: String?) {
            conversable.sendRawMessage(TextUtil.formatText(message).trim { it <= ' ' })
        }

        /**
         * Convert [Iterable] to [List]
         *
         * @param iterable The iterable to convert
         * @param <T>      Type of object
         * @return As a collection
         */
        @JvmStatic
        fun <T> toList(iterable: Iterable<T>): List<T>? {
            return StreamSupport.stream(iterable.spliterator(), false)
                .collect(Collectors.toList())
        }
    }
}