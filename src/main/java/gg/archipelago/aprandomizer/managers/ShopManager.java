package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ShopManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Category names
    public static final String CATEGORY_TOOLS = "tools";
    public static final String CATEGORY_HASTE = "haste";
    public static final String CATEGORY_EXCAVATION = "excavation";
    public static final String CATEGORY_REACH = "reach";
    public static final String CATEGORY_EFFICIENCY = "efficiency";

    // Tier names for display
    private static final Map<String, String[]> TIER_NAMES = new HashMap<>() {{
        put(CATEGORY_TOOLS, new String[] {
            "Wooden Tools",     // Tier 0 (starting)
            "Stone Tools",      // Tier 1
            "Iron Tools",       // Tier 2
            "Diamond Tools",    // Tier 3
            "Netherite Tools"   // Tier 4
        });
        put(CATEGORY_HASTE, new String[] {
            "No Haste",   // Tier 0 (starting)
            "Haste I"     // Tier 1
        });
        put(CATEGORY_EXCAVATION, new String[] {
            "No Excavation",   // Tier 0 (starting)
            "Excavation I",    // Tier 1
            "Excavation II",   // Tier 2
            "Excavation III"   // Tier 3
        });
        put(CATEGORY_REACH, new String[] {
            "No Reach Bonus",  // Tier 0 (starting)
            "Reach I (+1)"     // Tier 1
        });
        put(CATEGORY_EFFICIENCY, new String[] {
            "No Efficiency",   // Tier 0 (starting)
            "Efficiency I",    // Tier 1
            "Efficiency II",   // Tier 2
            "Efficiency III"   // Tier 3
        });
    }};

    // Prices for each tier (index = tier number)
    private static final Map<String, int[]> TIER_PRICES = new HashMap<>() {{
        put(CATEGORY_TOOLS, new int[] {
            0,   // Tier 0 - starting (free)
            5,   // Tier 1 - Stone Tools
            10,  // Tier 2 - Iron Tools
            20,  // Tier 3 - Diamond Tools
            35   // Tier 4 - Netherite Tools
        });
        put(CATEGORY_HASTE, new int[] {
            0,   // Tier 0 - starting (free)
            15   // Tier 1 - Haste I
        });
        put(CATEGORY_EXCAVATION, new int[] {
            0,   // Tier 0 - starting (free)
            20,  // Tier 1 - Excavation I
            50,  // Tier 2 - Excavation II
            70  // Tier 3 - Excavation III
        });
        put(CATEGORY_REACH, new int[] {
            0,   // Tier 0 - starting (free)
            25   // Tier 1 - Reach I
        });
        put(CATEGORY_EFFICIENCY, new int[] {
            0,   // Tier 0 - starting (free)
            15,  // Tier 1 - Efficiency I
            35,  // Tier 2 - Efficiency II
            50   // Tier 3 - Efficiency III
        });
    }};

    /**
     * Get the display name for a category/tier
     */
    public static String getTierName(String category, int tier) {
        String[] names = TIER_NAMES.get(category);
        if (names != null && tier >= 0 && tier < names.length) {
            return names[tier];
        }
        return category + " Tier " + tier;
    }

    /**
     * Get the price for a tier
     */
    public static int getTierPrice(String category, int tier) {
        int[] prices = TIER_PRICES.get(category);
        if (prices != null && tier >= 0 && tier < prices.length) {
            return prices[tier];
        }
        return Integer.MAX_VALUE; // Unknown tier = impossible price
    }

    /**
     * Get the max tier for a category
     */
    public static int getMaxTier(String category) {
        String[] names = TIER_NAMES.get(category);
        return names != null ? names.length - 1 : 0;
    }

    /**
     * Check if a tier can be purchased
     */
    public static boolean canPurchase(String category, int tier) {
        if (APRandomizer.worldData == null) return false;

        // 1. Is the tier unlocked by AP?
        String tierKey = category + "_" + tier;
        if (!APRandomizer.worldData.isTierUnlocked(tierKey)) {
            return false;
        }

        // 2. Is it the next sequential tier?
        int currentTier = APRandomizer.worldData.getPurchasedTier(category);
        if (tier != currentTier + 1) {
            return false;
        }

        // 3. Enough fossils?
        int price = getTierPrice(category, tier);
        return APRandomizer.worldData.getFossilBalance() >= price;
    }

    /**
     * Get the reason why a tier cannot be purchased
     */
    public static String getCannotPurchaseReason(String category, int tier) {
        if (APRandomizer.worldData == null) return "World not loaded";

        String tierKey = category + "_" + tier;
        if (!APRandomizer.worldData.isTierUnlocked(tierKey)) {
            return "Locked - Needs unlock from Archipelago";
        }

        int currentTier = APRandomizer.worldData.getPurchasedTier(category);
        if (tier != currentTier + 1) {
            if (tier <= currentTier) {
                return "Already purchased";
            } else {
                return "Must purchase tier " + (currentTier + 1) + " first";
            }
        }

        int price = getTierPrice(category, tier);
        int balance = APRandomizer.worldData.getFossilBalance();
        if (balance < price) {
            return "Need " + (price - balance) + " more fossils";
        }

        return ""; // Can purchase
    }

    /**
     * Attempt to purchase a tier
     * @return true if purchase was successful
     */
    public static boolean purchase(ServerPlayer player, String category, int tier) {
        if (!canPurchase(category, tier)) {
            String reason = getCannotPurchaseReason(category, tier);
            Utils.sendMessageToPlayer(player, "§cCannot purchase: " + reason);
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return false;
        }

        int price = getTierPrice(category, tier);
        String tierName = getTierName(category, tier);

        // Spend fossils
        if (!APRandomizer.worldData.spendFossils(price)) {
            return false;
        }

        // Update purchased tier
        APRandomizer.worldData.setPurchasedTier(category, tier);

        // Apply the upgrade via ItemManager
        APRandomizer.getItemManager().applyShopUpgrade(category, tier);

        // Feedback
        Utils.sendMessageToAll("§a" + player.getName().getString() + " purchased §e" + tierName + "§a!");
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

        LOGGER.info("{} purchased {} (tier {}). Remaining balance: {}",
            player.getName().getString(), tierName, tier, APRandomizer.worldData.getFossilBalance());

        // Save
        APRandomizer.getServer().execute(() -> {
            APRandomizer.getServer().saveEverything(true, true, true);
        });

        return true;
    }

    /**
     * Unlock a tier (called when receiving item from AP)
     */
    public static void unlockTier(String category, int tier) {
        if (APRandomizer.worldData == null) return;

        String tierKey = category + "_" + tier;

        // Check if already unlocked
        if (APRandomizer.worldData.isTierUnlocked(tierKey)) {
            return;
        }

        APRandomizer.worldData.unlockTier(tierKey);
        String tierName = getTierName(category, tier);

        Utils.sendMessageToAll("§eUnlocked in shop: §f" + tierName);

        LOGGER.info("Unlocked tier: {} ({})", tierName, tierKey);

        // Save
        APRandomizer.getServer().execute(() -> {
            APRandomizer.getServer().saveEverything(true, true, true);
        });
    }

    /**
     * Get the next tier that can be purchased for a category
     * @return the next tier number, or -1 if maxed out
     */
    public static int getNextPurchasableTier(String category) {
        if (APRandomizer.worldData == null) return -1;

        int currentTier = APRandomizer.worldData.getPurchasedTier(category);
        int nextTier = currentTier + 1;

        if (nextTier > getMaxTier(category)) {
            return -1; // Maxed out
        }

        return nextTier;
    }

    /**
     * Calculate total cost of all upgrades across all categories
     */
    public static int getTotalUpgradeCost() {
        int total = 0;
        for (int[] prices : TIER_PRICES.values()) {
            for (int price : prices) {
                total += price;
            }
        }
        return total;
    }

    /**
     * Check if the next tier is unlocked
     */
    public static boolean isNextTierUnlocked(String category) {
        int nextTier = getNextPurchasableTier(category);
        if (nextTier < 0) return false;

        String tierKey = category + "_" + nextTier;
        return APRandomizer.worldData.isTierUnlocked(tierKey);
    }
}
