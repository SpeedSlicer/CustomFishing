package com.illuzionzstudios.mist.util

import java.text.DecimalFormat
import java.util.*

/**
 * Utility class for mathematical operations.
 */
class MathUtil {
    companion object {

        /**
         * Formatter that transforms whole numbers into whole decimals with 1 decimal point
         */
        private val oneDigitFormat = DecimalFormat("#.#")

        /**
         * Formatter that transforms whole numbers into whole decimals with 2 decimal points
         */
        private val twoDigitsFormat = DecimalFormat("#.##")

        /**
         * Formatter that transforms whole numbers into whole decimals with 3 decimal points
         */
        private val threeDigitsFormat = DecimalFormat("#.###")

        /**
         * Formatter that transforms whole numbers into whole decimals with 5 decimal points
         */
        private val fiveDigitsFormat = DecimalFormat("#.#####")

        /**
         * Holds all valid roman numbers
         */
        private val romanNumbers: NavigableMap<Int, String> = TreeMap()


        // Load the roman numbers
        init {
            romanNumbers.put(1000, "M")
            romanNumbers.put(900, "CM")
            romanNumbers.put(500, "D")
            romanNumbers.put(400, "CD")
            romanNumbers.put(100, "C")
            romanNumbers.put(90, "XC")
            romanNumbers.put(50, "L")
            romanNumbers.put(40, "XL")
            romanNumbers.put(10, "X")
            romanNumbers.put(9, "IX")
            romanNumbers.put(5, "V")
            romanNumbers.put(4, "IV")
            romanNumbers.put(1, "I")
        }

        // ----------------------------------------------------------------------------------------------------
        // Number manipulation
        // ----------------------------------------------------------------------------------------------------
        /**
         * Return a roman number representation of the given number
         */
        fun toRoman(number: Int): String? {
            if (number == 0) return "0" // Actually, Romans did not know zero lol
            val literal = romanNumbers.floorKey(number)
            return if (number == literal) romanNumbers[number] else romanNumbers[literal]
                .toString() + toRoman(number - literal)
        }

        /**
         * Return the highest integer in the given number array
         */
        fun max(vararg numbers: Int): Int {
            return Arrays.stream(numbers).max().asInt
        }

        /**
         * See [Math.floor]
         */
        fun floor(d1: Double): Int {
            val i = d1.toInt()
            return if (d1 >= i) i else i - 1
        }

        /**
         * See [Math.ceil]
         */
        fun ceiling(f1: Double): Int {
            val i = f1.toInt()
            return if (f1 >= i) i else i - 1
        }

        /**
         * See [.range]
         *
         * @param value the real value
         * @param min   the min limit
         * @param max   the max limit
         * @return the value in range
         */
        fun range(value: Double, min: Double, max: Double): Double {
            return Math.min(Math.max(value, min), max)
        }

        /**
         * Get a value in range. If the value is < min, returns min, if it is > max, returns max.
         *
         * @param value the real value
         * @param min   the min limit
         * @param max   the max limit
         * @return the value in range
         */
        fun range(value: Int, min: Int, max: Int): Int {
            return Math.min(Math.max(value, min), max)
        }

        /**
         * Increase the given number by given percents (from 0 to 100)
         */
        fun increase(number: Int, percent: Double): Int {
            val percentage = number.toDouble() / 100 * percent
            return Math.round(number.toDouble() + percentage).toInt()
        }

        /**
         * Increase the given number by given percents (from 0 to 100)
         */
        fun increase(number: Double, percent: Double): Double {
            val percentage = number / 100 * percent
            return number + percentage
        }

        /**
         * Calculates the percentage (completion) of the given number from the maximum
         * in 0 till 100
         *
         * @return 0 to 100 of the given number portion of the maximum
         */
        fun percent(number: Double, maximum: Double): Int {
            return (number / maximum * 100).toInt()
        }

        /**
         * Return the average double of the given values
         */
        fun average(values: Collection<Double>): Double {
            return values.toTypedArray().average()
        }

        /**
         * Return the average double of the given values
         */
        fun average(vararg values: Double): Double {
            var sum = 0.0
            for (`val` in values) sum += `val`
            return formatTwoDigitsD(sum / values.size)
        }

        /**
         * Calculate if a given chance as decimal value is passed
         * out of 100
         *
         * @param percent Chance as whole number to 100
         * @return If the odds were in it's favour
         */
        fun chance(percent: Double): Boolean {
            return Random().nextInt(100) <= percent
        }
        // ----------------------------------------------------------------------------------------------------
        // Formatting
        // ----------------------------------------------------------------------------------------------------
        /**
         * Formats the given number into one digit
         */
        fun formatOneDigit(value: Double): String {
            return oneDigitFormat.format(value).replace(",", ".")
        }

        /**
         * Formats the given number into one digit
         */
        fun formatOneDigitD(value: Double): Double {
            Valid.checkBoolean(!java.lang.Double.isNaN(value), "Value must not be NaN")
            return oneDigitFormat.format(value).replace(",", ".").toDouble()
        }

        /**
         * Formats the given number into two digits
         */
        fun formatTwoDigits(value: Double): String {
            return twoDigitsFormat.format(value).replace(",", ".")
        }

        /**
         * Formats the given number into two digits
         */
        fun formatTwoDigitsD(value: Double): Double {
            Valid.checkBoolean(!java.lang.Double.isNaN(value), "Value must not be NaN")
            return twoDigitsFormat.format(value).replace(",", ".").toDouble()
        }

        /**
         * Formats the given number into three digits
         */
        fun formatThreeDigits(value: Double): String {
            return threeDigitsFormat.format(value).replace(",", ".")
        }

        /**
         * Formats the given number into three digits
         */
        fun formatThreeDigitsD(value: Double): Double {
            Valid.checkBoolean(!java.lang.Double.isNaN(value), "Value must not be NaN")
            return threeDigitsFormat.format(value).replace(",", ".").toDouble()
        }

        /**
         * Formats the given number into five digits
         */
        fun formatFiveDigits(value: Double): String {
            return fiveDigitsFormat.format(value).replace(",", ".")
        }

        /**
         * Formats the given number into five digits
         */
        fun formatFiveDigitsD(value: Double): Double {
            Valid.checkBoolean(!java.lang.Double.isNaN(value), "Value must not be NaN")
            return fiveDigitsFormat.format(value).replace(",", ".").toDouble()
        }
        // ----------------------------------------------------------------------------------------------------
        // Calculating
        // ----------------------------------------------------------------------------------------------------
        /**
         * Evaluate the given expression, e.g. 5*(4-2) returns... let me check!
         */
        fun calculate(expression: String): Double {
            class Parser {
                var pos = -1
                var c = 0
                fun eatChar() {
                    c = if (++pos < expression.length) expression[pos].code else -1
                }

                fun eatSpace() {
                    while (Character.isWhitespace(c)) eatChar()
                }

                fun parse(): Double {
                    eatChar()
                    val v = parseExpression()
                    if (c != -1) throw CalculatorException("Unexpected: " + c.toChar())
                    return v
                }

                // Grammar:
                // expression = term | expression `+` term | expression `-` term
                // term = factor | term `*` factor | term `/` factor | term brackets
                // factor = brackets | number | factor `^` factor
                // brackets = `(` expression `)`
                fun parseExpression(): Double {
                    var v = parseTerm()
                    while (true) {
                        eatSpace()
                        if (c == '+'.code) { // addition
                            eatChar()
                            v += parseTerm()
                        } else if (c == '-'.code) { // subtraction
                            eatChar()
                            v -= parseTerm()
                        } else return v
                    }
                }

                fun parseTerm(): Double {
                    var v = parseFactor()
                    while (true) {
                        eatSpace()
                        if (c == '/'.code) { // division
                            eatChar()
                            v /= parseFactor()
                        } else if (c == '*'.code || c == '('.code) { // multiplication
                            if (c == '*'.code) eatChar()
                            v *= parseFactor()
                        } else return v
                    }
                }

                fun parseFactor(): Double {
                    var v: Double
                    var negate = false
                    eatSpace()
                    if (c == '+'.code || c == '-'.code) { // unary plus & minus
                        negate = c == '-'.code
                        eatChar()
                        eatSpace()
                    }
                    if (c == '('.code) { // brackets
                        eatChar()
                        v = parseExpression()
                        if (c == ')'.code) eatChar()
                    } else { // numbers
                        val sb = StringBuilder()
                        while (c >= '0'.code && c <= '9'.code || c == '.'.code) {
                            sb.append(c.toChar())
                            eatChar()
                        }
                        if (sb.length == 0) throw CalculatorException("Unexpected: " + c.toChar())
                        v = sb.toString().toDouble()
                    }
                    eatSpace()
                    if (c == '^'.code) { // exponentiation
                        eatChar()
                        v = Math.pow(v, parseFactor())
                    }
                    if (negate) v = -v // unary minus is applied after exponentiation; e.g. -3^2=-9
                    return v
                }
            }
            return Parser().parse()
        }

        /**
         * An exception thrown when calculating wrong numbers (i.e. 0 division)
         *
         *
         * See [MathUtil.calculate]
         */
        class CalculatorException(message: String?) : RuntimeException(message) {
            companion object {
                private const val serialVersionUID = 1L
            }
        }
    }
}