package com.illuzionzstudios.mist.scheduler.rate

/**
 * Sets a defined rate or interval between ticking
 */
enum class Rate(
    /**
     * The amount of ticks between each tick of the object
     */
    val time: Long
) {
    MIN_64(3840000L),
    MIN_32(1920000L),
    MIN_16(960000L),
    MIN_08(480000L),
    MIN_04(240000L),
    MIN_02(120000L),
    MIN_01(60000L),
    SLOWEST(32000L),
    SLOWER(16000L),
    SEC_10(10000L),
    SEC_8(8000L),
    SEC_6(6000L),
    SEC_4(4000L),
    SEC_2(2000L),
    SEC(1000L),
    FAST(500L),
    FASTER(250L),
    FASTEST(125L),
    TICK(50L),
    INSTANT(0L);

    /**
     * The last amount of ticks passed
     */
    @Volatile
    private var last: Long

    /**
     * Total milliseconds spent ticking
     */
    @Volatile
    private var timeSpent: Long = 0

    /**
     * Log the start time
     */
    @Volatile
    private var timeCount: Long = 0

    /**
     * @return If total time to fully tick has passed
     */
    @Synchronized
    fun hasElapsed(): Boolean {
        if (elapsed(last, time)) {
            last = System.currentTimeMillis()
            return true
        }
        return false
    }

    /**
     * Start the time
     */
    fun startTime() {
        timeCount = System.currentTimeMillis()
    }

    /**
     * Stop the time
     */
    fun stopTime() {
        timeSpent += System.currentTimeMillis() - timeCount
    }

    /**
     * Log and reset the time
     */
    fun printAndResetTime() {
        println("$name in a second: $timeSpent")
        timeSpent = 0L
    }

    companion object {
        /**
         * If the current time minus a time is greater than another
         *
         * @param from     Starting time
         * @param required The time that must be met
         * @return If time is met
         */
        @Synchronized
        fun elapsed(from: Long, required: Long): Boolean {
            return System.currentTimeMillis() - from > required
        }
    }

    init {
        last = System.currentTimeMillis()
    }
}