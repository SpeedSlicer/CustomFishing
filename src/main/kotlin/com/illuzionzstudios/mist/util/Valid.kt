package com.illuzionzstudios.mist.util

import com.illuzionzstudios.mist.exception.PluginException
import java.util.regex.Pattern

/**
 * Util class to check if things are valid. This means checking nulls,
 * empties, if an item is air etc
 */
class Valid {

    companion object {

        /**
         * Matching valid integers
         */
        private val PATTERN_INTEGER = Pattern.compile("-?\\d+")

        /**
         * Matching valid whole numbers
         */
        private val PATTERN_DECIMAL = Pattern.compile("-?\\d+.\\d+")

        /**
         * Throws an error if the given object is null
         *
         * @param toCheck Object to check if is null
         */
        fun checkNotNull(toCheck: Any?) {
            if (toCheck == null) throw PluginException("null")
        }

        /**
         * Throws an error with a custom message if the given object is null
         *
         * @param toCheck      Object to check if is null
         * @param falseMessage Message explaining why it may have been null
         */
        fun checkNotNull(toCheck: Any?, falseMessage: String?) {
            if (toCheck == null) throw PluginException(falseMessage)
        }

        /**
         * Throws an error if the given expression is false
         *
         * @param expression Boolean expression to check
         */
        fun checkBoolean(expression: Boolean) {
            if (!expression) throw PluginException("null")
        }

        /**
         * Throws an error with a custom message if the given expression is false
         *
         * @param expression   Boolean expression to check
         * @param falseMessage Message explaining why it may have been false
         */
        fun checkBoolean(expression: Boolean, falseMessage: String?) {
            if (!expression) throw PluginException(falseMessage)
        }
        // ------------------------------------------------------------------------------------------------------------
        // Checking for true without throwing errors
        // ------------------------------------------------------------------------------------------------------------
        /**
         * Returns true if the given string is a valid integer
         */
        fun isInteger(raw: String?): Boolean {
            return PATTERN_INTEGER.matcher(raw).find()
        }

        /**
         * Returns true if the given string is a valid whole number
         */
        fun isDecimal(raw: String?): Boolean {
            return PATTERN_DECIMAL.matcher(raw).find()
        }

        /**
         * Parse a string into a boolean
         *
         * @param check The string to check
         * @return True if parsed correctly otherwise false
         */
        fun parseBoolean(check: String?): Boolean {
            return Pattern.compile("true|yes|1").matcher(check).find()
        }
    }
}