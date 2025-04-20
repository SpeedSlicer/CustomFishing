package com.illuzionzstudios.mist.scheduler

/**
 * Represents an object that can be ticked. Errors and everything
 * else will be handled in the ticker
 */
interface Tickable {
    /**
     * Calls tick operation
     * Should be ran safely!
     */
    @Throws(Exception::class)
    fun tick()
}