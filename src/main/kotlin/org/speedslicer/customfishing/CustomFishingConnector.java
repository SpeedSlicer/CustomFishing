package org.speedslicer.customfishing;

import com.illuzionzstudios.customfishing.reward.RewardController;
import com.illuzionzstudios.customfishing.reward.fishing.FishingReward;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomFishingConnector {
    private static HashMap<String, FishingReward> fishingRewards = new HashMap<>();
    private static ArrayList<String> fishingNameAutoComplete = new ArrayList<>();
    public static RewardController rewardController;

    public static void addReward(String rewardName, FishingReward fishingReward) {
        String rewardNameFinal = rewardName.replace(" ", "");
        fishingRewards.put(rewardNameFinal, fishingReward);
        fishingNameAutoComplete.add(rewardName);
    }

    public static boolean itemExists(String key) {
        return fishingRewards.containsKey(key);
    }

    public static FishingReward getReward(String key) {
        return fishingRewards.get(key);
    }

    public static String getFish() {
        String fish = "Fishes:";
        for (String fishes : fishingNameAutoComplete) {
            fish = fish + " " + fishes + ",";
        }
        return fish;
    }
}
