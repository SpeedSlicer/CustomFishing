package com.illuzionzstudios.mist.debug

import com.illuzionzstudios.mist.Logger

/**
 * Check how long a task takes to run
 */
class PerformanceTimer(val taskName: String) {

    private var startTime: Long = 0L
    var timeTaken = 0L

    init {
        this.startTime = System.currentTimeMillis()
    }

    /**
     * Ends timer
     */
    fun complete() {
        this.timeTaken = System.currentTimeMillis() - this.startTime
    }

    /**
     * Ends timer and prints debug to console
     */
    fun completeAndLog() {
        complete()
        Logger.debug("Performance Task '$taskName' completed in $timeTaken ms")
    }

}