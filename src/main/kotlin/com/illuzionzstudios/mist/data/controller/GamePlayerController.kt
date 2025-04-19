package com.illuzionzstudios.mist.data.controller

import com.illuzionzstudios.mist.data.player.GamePlayer
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import java.util.*

/**
 * Default game player controllers
 */
class GamePlayerController : BukkitPlayerController<GamePlayer?>() {
    override fun newInstance(uuid: UUID, s: String?): GamePlayer {
        return GamePlayer(uuid, s)
    }

    override fun initialize(plugin: SpigotPlugin) {
        super.initialize(plugin)
        INSTANCE = this
    }

    companion object {
        var INSTANCE: GamePlayerController? = null
    }
}