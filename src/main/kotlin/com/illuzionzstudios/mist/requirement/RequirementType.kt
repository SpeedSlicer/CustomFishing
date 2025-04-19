package com.illuzionzstudios.mist.requirement

/**
 * Types of filters to filter players by
 *
 * @param matcher The string in config that will use this type
 */
enum class RequirementType(private val matcher: String, val inverted: Boolean = false) {

    /**
     * Checks if the player has a permission
     */
    PERMISSION("permission"),

    /**
     * Check if the player is in a region
     */
    REGION("region"),

    /**
     * Check if has at least a certain amount of experience points
     */
    EXPERIENCE("exp"),

    /**
     * Check if is near a certain location
     */
    NEAR("near"),

    /**
     * In a certain world
     */
    WORLD("world"),

    /**
     * Check if a string is equal to a value
     */
    STRING_EQUALS("string equals"),

    /**
     * Check if a string is equal to a value (ignore case)
     */
    STRING_EQUALS_IGNORECASE("string equals ignorecase"),

    /**
     * Check if a string is contained in the input
     */
    STRING_CONTAINS("string contains"),

    /**
     * Check if regex matches the given input
     */
    REGEX("regex"),

    /**
     * Operators, converts to int values
     */
    EQUAL("=="),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    NOT_EQUAL("!="),
    GREATER_THAN(">"),
    LESS_THAN("<");

    companion object {

        /**
         * Get filter from the matcher
         */
        fun getFilter(matcher: String): RequirementType {
            for (type in values()) {
                if (matcher.equals(type.matcher, true)) return type
            }

            return PERMISSION
        }
    }

}