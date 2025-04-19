package com.illuzionzstudios.mist

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import java.util.logging.Level

/**
 * [SpigotPlugin] logger to handle logging
 */
class Logger {

    companion object {

        //  -------------------------------------------------------------------------
        //  Main logging functions
        //  -------------------------------------------------------------------------

        /**
         * Debug an object as output
         *
         * @param message Prints the string version of on object as debug
         */
        @JvmStatic
        fun debug(message: Any) {
            log(Level.WARNING, "[DEBUG] $message")
        }

        /**
         * Print debug messages to the console. Warns as their
         * easier to see
         *
         * @param message    Message to send
         * @param parameters Formatting parameters
         */
        @JvmStatic
        fun debug(message: String, vararg parameters: Any?) {
            log(Level.WARNING, "[DEBUG] $message", parameters)
        }

        /**
         * Send a warning to the console
         *
         * @param message    Message to send
         * @param parameters Formatting parameters
         */
        @JvmStatic
        fun warn(message: String, vararg parameters: Any?) {
            log(Level.WARNING, "[WARN] $message", parameters)
        }

        /**
         * Log a error to the console
         *
         * @param message    Message to send
         * @param parameters Formatting parameters
         */
        @JvmStatic
        fun severe(message: String, vararg parameters: Any?) {
            log(Level.SEVERE, "[SEVERE] $message", parameters)
        }

        /**
         * Basic method to report information to the console
         *
         * @param message    Message to send
         * @param parameters Formatting parameters
         */
        @JvmStatic
        fun info(message: String, vararg parameters: Any?) {
            log(Level.INFO, message, parameters)
        }

        /**
         * Base method to log output to the console at a certain logging level
         *
         * @param level      Logging level
         * @param message    The object/message to log
         * @param parameters Formatting parameters
         */
        @JvmStatic
        private fun log(level: Level, message: Any, vararg parameters: Any) {
            // Make sure has instance
            if (!SpigotPlugin.hasInstance()) {
                println("[Unloaded Plugin] " + String.format(message.toString(), *parameters))
                return
            }
            SpigotPlugin.instance?.logger?.log(level, String.format(message.toString(), *parameters))
        }

        //  -------------------------------------------------------------------------
        //  Error handling
        //  -------------------------------------------------------------------------

        /**
         * Nicely display an error to console
         *
         * @param throwable    The error to display
         * @param errorMessage The error message/cause for this error
         */
        @JvmStatic
        fun displayError(throwable: Throwable, errorMessage: String) {
            severe("An error occurred for " + SpigotPlugin.pluginName + " v" + SpigotPlugin.pluginVersion + ": " + errorMessage)
            severe("Report the following error:")
            throwable.printStackTrace()
        }
    }

}