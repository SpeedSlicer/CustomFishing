package com.illuzionzstudios.mist.command.temporary

import net.md_5.bungee.api.chat.ClickEvent
import org.apache.commons.lang.RandomStringUtils
import org.bukkit.entity.Player

/**
 * A temporary command is a command create for a single use. Useful for
 * click events in chat so we can perform any action
 */
abstract class TemporaryCommand(
    /**
     * Player executing the action
     */
    val player: Player,

    /**
     * Label to call for this action
     */
    val label: String = RandomStringUtils.randomAlphabetic(6)
) {

    init {
        TemporaryCommandManager.registeredTemp[label] = this
    }

    /**
     * Run the action
     */
    abstract fun run(player: Player)

    /**
     * Get click event to be used in chat
     */
    fun getClickEvent(): ClickEvent {
        return ClickEvent(ClickEvent.Action.RUN_COMMAND, "/misttemp $label")
    }

}