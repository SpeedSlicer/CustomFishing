package com.illuzionzstudios.mist.exception

/**
 * Custom exception in our plugin which allows us
 * to easily debug problems
 */
class PluginException : RuntimeException {
    /**
     * Create a new exception and logs it
     *
     * @param t The error thrown
     */
    constructor(t: Throwable?) : super(t)

    /**
     * Create a new exception and logs it
     *
     * @param message The cause of the error
     */
    constructor(message: String?) : super(message)

    /**
     * Create a new exception and logs it
     *
     * @param message The cause of the error
     * @param t       The error thrown
     */
    constructor(t: Throwable?, message: String?) : super(message, t)

    companion object {
        private const val serialVersionUID = 1L
    }
}