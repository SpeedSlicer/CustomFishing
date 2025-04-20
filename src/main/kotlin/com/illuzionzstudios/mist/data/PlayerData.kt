package com.illuzionzstudios.mist.data

import com.illuzionzstudios.mist.data.player.AbstractPlayer

/**
 * Basis of player data
 */
interface PlayerData<P : AbstractPlayer?> {
    /**
     * Gets the player associated with the data
     */
    val player: P
}