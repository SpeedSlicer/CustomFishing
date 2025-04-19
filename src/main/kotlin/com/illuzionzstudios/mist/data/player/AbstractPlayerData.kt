package com.illuzionzstudios.mist.data.player

import com.illuzionzstudios.mist.data.PlayerData
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler

/**
 * Registered player data
 */
abstract class AbstractPlayerData<P : AbstractPlayer?>(
    /**
     * The player that owns this data
     */
    override var player: P
) : PlayerData<P> {
    /**
     * Keys to replace when querying
     */
    var localKeys = HashMap<String, String>()

    /**
     * If the scheduler is registered
     */
    private var schedulerRegistered = false

    /**
     * Run when attempting to save data
     */
    fun onSave() {}
    open fun unregister() {
        if (schedulerRegistered) {
            MinecraftScheduler.get()!!.dismissSynchronizationService(this)
        }
    }

    protected fun registerScheduler() {
        MinecraftScheduler.get()!!.registerSynchronizationService(this)
        schedulerRegistered = true
    }
}