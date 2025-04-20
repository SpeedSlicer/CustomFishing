package com.illuzionzstudios.mist.random

import java.util.*

/**
 * A util class for randomly generating numbers with an upper and lower limit
 * Provides additional functionality on top of [java.util.Random]
 *
 * Generates from lower (inclusive) to upper (inclusive)
 */
class RandomNumberGenerator
/**
 * Set lower and upper limits for generation
 */(
    /**
     * The lower bound for a generated number
     */
    private val lower: Double,
    /**
     * The upper bound for a generated number
     */
    private val upper: Double
) {
    constructor(upper: Double) : this(0.0, upper)

    /**
     * @return A randomly generated number in our range
     */
    fun generate(): Double {
        // Give new serial for generation
        val random = Random()

        // Precise value to add on to generated value
        val precision = random.nextDouble()
        return random.nextInt(upper.toInt() - lower.toInt() + 1) + lower + precision
    }

    companion object {
        /**
         * Parse a string into a [RandomNumberGenerator].
         * Syntax is "{lower}to{upper}". If just one number is provided, only generates
         * that single number.
         *
         * @param string String as "{lower}to{upper}"
         * @return [RandomNumberGenerator] with those bounds
         */
        fun parse(string: String?): RandomNumberGenerator {
            var string = string ?: return RandomNumberGenerator(0.0)

            // Remove whitespace
            string = string.replace("\\s+".toRegex(), "")
            // Create tokens
            val tokens: Array<String?> = string.split("to".toRegex()).toTypedArray()

            // Else use first element as upper
            return if (tokens[0] != null && tokens[1] != null) RandomNumberGenerator(
                tokens[0]!!.toDouble(),
                tokens[1]!!.toDouble()
            ) else if (tokens[0] != null) RandomNumberGenerator(
                tokens[0]!!.toDouble(), tokens[0]!!.toDouble()
            ) else RandomNumberGenerator(0.0)
        }
    }
}