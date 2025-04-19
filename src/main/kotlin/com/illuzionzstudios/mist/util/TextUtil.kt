package com.illuzionzstudios.mist.util

import com.illuzionzstudios.mist.compatibility.ServerVersion
import net.md_5.bungee.api.ChatColor
import org.apache.commons.lang.StringEscapeUtils
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * Util methods to help parsing text
 */
class TextUtil {

    companion object {

        /**
         * Returns a long ------ smooth console line
         */
        @JvmField
        val SMOOTH_LINE = ChatColor.STRIKETHROUGH.toString() + "                                                               "

        /**
         * Master method to format text. This will run formatting like
         * capitalizing, translating colour codes, etc.
         *
         * @param text [String] to format
         * @return The formatted [String]
         */
        @JvmOverloads
        fun formatText(text: String?, capitalize: Boolean = false): String {
            var text = text
            if (text == null || text == "") return ""
            if (capitalize) text = text.substring(0, 1).uppercase(Locale.getDefault()) + text.substring(1)

            // Parse unicode
            text = StringEscapeUtils.unescapeJava(text)

            // Parse hexadecimal as &#FAFAFA<message>
            if (ServerVersion.atLeast(ServerVersion.V.v1_16)) {
                val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
                var matcher = pattern.matcher(text)

                while (matcher.find()) {
                    val color: String = text?.substring(matcher.start(), matcher.end())!!
                    val chatColor = ChatColor.of(color.substring(1, color.length))
                    text = text.replace(color, chatColor.toString())
                    matcher = pattern.matcher(text)
                }
            }
            text = ChatColor.translateAlternateColorCodes('&', text)
            return text
        }

        /**
         * Format a list of [String]
         *
         * @see [formatText]
         */
        fun formatText(text: List<String?>): List<String> {
            val formatted: MutableList<String> = ArrayList()
            text.forEach(Consumer { str: String? -> formatted.add(formatText(str)) })
            return formatted
        }

        /**
         * This will turn camelCase into PascalCase.
         * For instance vanillaRewards becomes Vanilla Rewards
         *
         * @param text camelCase text
         * @return PascalCase text
         */
        fun convertCamelCase(text: String): String {
            return formatText(
                text.replace(
                    String.format(
                        "%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                    ).toRegex(),
                    " "
                ), true
            )
        }

        /**
         * Util method to get amount of ' ' chars in [String] before the first non-space char
         */
        fun getOffset(s: String): Int {
            val chars = s.toCharArray()
            for (i in chars.indices) {
                if (chars[i] != ' ') {
                    return i
                }
            }
            return -1
        }

        /**
         * Replace last text occurrence with another string of text
         */
        fun replaceLast(string: String, toReplace: String, replacement: String): String {
            val index = string.lastIndexOf(toReplace)
            return if (index == -1) {
                string
            } else string.substring(0, index) + replacement + string.substring(index + toReplace.length)
        }

        /**
         * Format a millisecond duration into human-readable time
         *
         * @param millis Total milliseconds
         * @param verbose If to use full descriptors (days vs d)
         */
        fun getFormattedTime(millis: Long, verbose: Boolean): String {
            var millis = millis
            require(millis >= 0) { "Duration must be greater than zero!" }
            val daysLeft = TimeUnit.MILLISECONDS.toDays(millis)
            millis -= TimeUnit.DAYS.toMillis(daysLeft)
            val hoursLeft = TimeUnit.MILLISECONDS.toHours(millis)
            millis -= TimeUnit.HOURS.toMillis(hoursLeft)
            val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(millis)
            millis -= TimeUnit.MINUTES.toMillis(minutesLeft)
            val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millis)
            val message = StringBuilder()
            if (daysLeft != 0L) {
                message.append(daysLeft)
                message.append(if (verbose) " days" else "d")
            }
            if (hoursLeft != 0L) {
                if (message.isNotEmpty()) {
                    message.append(if (verbose) ", " else " ")
                }
                message.append(hoursLeft)
                message.append(if (verbose) " hour" else "h")
                if (verbose && hoursLeft > 1) {
                    message.append("s")
                }
            }
            if (minutesLeft != 0L) {
                if (message.isNotEmpty()) {
                    message.append(if (verbose) ", " else " ")
                }
                message.append(minutesLeft)
                message.append(if (verbose) " minute" else "m")
                if (verbose && minutesLeft > 1) {
                    message.append("s")
                }
            }

            // Only display seconds if waittime is <1 hr
            if (secondsLeft != 0L && hoursLeft == 0L && daysLeft == 0L) {
                if (message.isNotEmpty()) {
                    message.append(if (verbose) ", " else " ")
                }
                message.append(secondsLeft)
                message.append(if (verbose) " second" else "s")
                if (verbose && secondsLeft > 1) {
                    message.append("s")
                }
            }

            // Only display seconds if waittime is <1 sec
            if (hoursLeft == 0L && minutesLeft == 0L && secondsLeft == 0L && millis > 0) {
                if (message.isNotEmpty()) {
                    message.append(if (verbose) ", " else " ")
                }
                message.append(millis)
                message.append(if (verbose) " millis" else "ms")
            }
            var formatted = message.toString()
            if (verbose) {
                formatted = replaceLast(formatted, ", ", " and ")
            }
            return formatted
        }

        /**
         * Makes the string centered with padding based on the max width. Default is max width for chat
         */
        @JvmOverloads
        fun getCenteredString(message: String?, maxWidth: Int = 77): String {
            var message = message
            if (message == null || message == "") return ""

            message = formatText(message)
            var messagePxSize = 0
            var previousCode = false
            var isBold = false
            for (c in message.toCharArray()) {
                if (c == '§') {
                    previousCode = true
                    continue
                } else if (previousCode) {
                    previousCode = false
                    if (c == 'l' || c == 'L') {
                        isBold = true
                        continue
                    } else isBold = false
                } else {
                    val dFI = DefaultFontInfo.getDefaultFontInfo(c)
                    messagePxSize += if (isBold) dFI.getBoldLength() else dFI.length
                    messagePxSize++
                }
            }
            val halvedMessageSize = messagePxSize / 2
            val toCompensate: Int = (maxWidth * 2) - halvedMessageSize
            val spaceLength = DefaultFontInfo.SPACE.length + 1
            var compensated = 0
            val sb = StringBuilder()
            while (compensated < toCompensate) {
                sb.append(" ")
                compensated += spaceLength
            }
            return sb.toString() + message
        }
    }
}