package com.illuzionzstudios.customfishing.command

import com.illuzionzstudios.mist.command.SpigotCommandGroup
import com.illuzionzstudios.mist.command.type.ReloadCommand
import org.speedslicer.customfishing.commands.GiveCommand
import org.speedslicer.customfishing.commands.ListFishCommand

class CustomFishingCommand : SpigotCommandGroup() {

    override fun registerSubcommands() {
        registerSubCommand(ReloadCommand())
        registerSubCommand(GiveCommand())
        registerSubCommand(ListFishCommand());
    }
}