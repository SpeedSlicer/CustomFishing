package com.illuzionzstudios.mist.command.response

/**
 * This is the response type back from the command which lets us handle custom
 * callbacks. This makes it so we don't have to repeat things like insufficient permission
 * or player only etc.
 */
enum class ReturnType {
    /**
     * The command executed successfully
     */
    SUCCESS,

    /**
     * Only the player can execute this command (not console)
     */
    PLAYER_ONLY,

    /**
     * Lacking a permission to do something within the command
     */
    NO_PERMISSION,

    /**
     * Unexpected error occurred here
     */
    UNKNOWN_ERROR
}