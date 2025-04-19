package com.illuzionzstudios.mist.data.player

import com.illuzionzstudios.mist.data.PlayerData
import com.illuzionzstudios.mist.data.controller.PlayerDataController
import java.util.*

/**
 * A player that we don't know if is offline or online
 */
class OfflinePlayer(uuid: UUID, name: String?) : AbstractPlayer(uuid, name) {
    /*
     * Automatically creates a new abstract player data object
     * if not already applied by default.
     */
    override fun <T : PlayerData<*>?> get(type: Class<T>): T? {
        var data = super.get(type)
        if (data != null) {
            return data
        }
        data = PlayerDataController.INSTANCE!!.getDefaultData<T>(this, type)
        return data
    }
}