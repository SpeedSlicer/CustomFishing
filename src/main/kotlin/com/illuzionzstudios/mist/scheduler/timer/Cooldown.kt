package com.illuzionzstudios.mist.scheduler.timer

import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.util.TextUtil

/**
 * A set cooldown that checks if a set amount of time has passed
 */
open class Cooldown(ticks: Int = 0) {
    /**
     * Represents the time (in ticks) when the time will expire
     */
    private var expireTicks: Long = 0

    /**
     * Represents the time (in mills) when the time will expire
     */
    private var expireTime: Long = 0

    /**
     * @param ticks Set in x ticks after current time, [.isReady]
     */
    open fun setWait(ticks: Int) {
        expireTicks = MinecraftScheduler.currentTick + ticks
        expireTime = System.currentTimeMillis() + ticks * 50L
    }

    /**
     * Reset the timer to check for time again
     */
    fun reset() {
        expireTicks = 0
    }

    /**
     * @return If the set time has passed as ticks
     */
    val isReady: Boolean
        get() = tickLeft <= 0

    /**
     * @return If set millis time has passed
     */
    val isReadyRealTime: Boolean
        get() = millisecondsLeft <= 0

    /**
     * @return Get millis left before expire time
     */
    private val millisecondsLeft: Long
        get() = expireTime - System.currentTimeMillis()

    /**
     * @return Get ticks left before expire time
     */
    val tickLeft: Long
        get() = expireTicks - MinecraftScheduler.currentTick

    /**
     * Formatted version of [.getMillisecondsLeft]
     *
     * @param verbose If to set full name for scale, eg if true
     * "days" over "d"
     * @return The formatted time as a [String]
     */
    fun getFormattedTimeLeft(verbose: Boolean): String {
        return TextUtil.getFormattedTime(millisecondsLeft, verbose)
    }

    /**
     * @param ticks How many ticks from current time will expire
     */
    init {
        setWait(ticks)
    }
}