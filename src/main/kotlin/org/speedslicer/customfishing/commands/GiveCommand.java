package org.speedslicer.customfishing.commands;

import com.illuzionzstudios.customfishing.reward.fishing.FishingReward;
import com.illuzionzstudios.customfishing.reward.fishing.item.FishingItem;
import com.illuzionzstudios.mist.command.SpigotSubCommand;
import com.illuzionzstudios.mist.command.response.ReturnType;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.speedslicer.customfishing.CustomFishingConnector;

import java.util.*;

public class GiveCommand extends SpigotSubCommand {

    public GiveCommand() {

        super("give", "g");
    }

    @Override
    public ReturnType onCommand() {
        if (getArgs().length == 0) {
            getSender().sendMessage("§cUsage: /fishing give <itemKey>");
            return ReturnType.UNKNOWN_ERROR;
        }

        if (!getSender().hasPermission("customfishing.giveitem")) {
            getSender().sendMessage("&5Needs permission: customfishing.giveitem");
            return ReturnType.NO_PERMISSION;
        }
        String key = getArgs()[0];

        if (CustomFishingConnector.itemExists(key)) {
            getSender().sendMessage("§aGave rewards for: " + key);
            for (FishingItem item : Objects.requireNonNull(CustomFishingConnector.getReward(key).getItems())) {
                item.givePlayer(getPlayer());
            }
            return ReturnType.SUCCESS;
        } else {
            getSender().sendMessage("§cItem not found: " + key);
            return ReturnType.UNKNOWN_ERROR;
        }
    }
}
