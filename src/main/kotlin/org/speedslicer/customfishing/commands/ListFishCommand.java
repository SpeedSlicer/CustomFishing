package org.speedslicer.customfishing.commands;

import com.illuzionzstudios.mist.command.SpigotSubCommand;
import com.illuzionzstudios.mist.command.response.ReturnType;
import org.speedslicer.customfishing.CustomFishingConnector;

public class ListFishCommand extends SpigotSubCommand {
    public ListFishCommand() {
        super("listFish");
        setDescription("Lists all loaded fish");
    }

    @Override
    public ReturnType onCommand() {
        if (!getSender().hasPermission("customfishing.listfish")) {
            getSender().sendMessage("&5Needs permission: customfishing.listfish");
            return ReturnType.NO_PERMISSION;
        }
        getSender().sendMessage(CustomFishingConnector.getFish());
        return ReturnType.SUCCESS;
    }

}
