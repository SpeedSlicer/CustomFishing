package com.illuzionzstudios.mist.scheduler.timer

import lombok.Getter
import lombok.Setter

/**
 * A pre-set cool down that can be started when we wish
 */
@Getter
@Setter
class PresetCooldown(
    /**
     * The amount of ticks to wait
     */
    val wait: Int
) : Cooldown() {
    /**
     * Start the timer
     */
    fun go() {
        super.setWait(wait)
    }

    init {
        go()
    }
}