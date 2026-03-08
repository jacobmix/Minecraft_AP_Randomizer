package gg.archipelago.aprandomizer.managers.itemmanager;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.itemmanager.powers.ExcavationPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber
public class TemporaryBonusManager {

    private static final Logger LOGGER = LogManager.getLogger();

    // Bonus types
    public static final String BONUS_HASTE = "haste_boost";
    public static final String BONUS_EXCAVATION = "excavation_boost";

    // Duration in seconds for each bonus
    public static final int BONUS_DURATION_SECONDS = 30;

    // Active bonuses for online players: UUID -> (BonusType -> remaining ticks)
    private static final HashMap<UUID, HashMap<String, Integer>> activeBonuses = new HashMap<>();

    // Original excavation level before boost (to restore after)
    private static int originalExcavationLevel = -1;
    private static int excavationBoostActivePlayers = 0;

    /**
     * Grant a temporary bonus to all players.
     * If a player is online, apply immediately.
     * If offline but known, store as pending bonus.
     */
    public static void grantBonusToAll(String bonusType) {
        int durationTicks = BONUS_DURATION_SECONDS * 20;

        // Apply to all online players
        if (APRandomizer.getServer() != null) {
            for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                addBonusToPlayer(player.getUUID(), bonusType, durationTicks);
                applyBonusEffect(player, bonusType);
            }
        }

        // Add pending bonus for offline known players (use WorldData directly)
        if (APRandomizer.worldData != null) {
            for (UUID uuid : APRandomizer.worldData.getKnownPlayers()) {
                if (!isPlayerOnline(uuid)) {
                    APRandomizer.worldData.addPendingBonus(uuid, bonusType, BONUS_DURATION_SECONDS);
                }
            }
        }

        String bonusName = bonusType.equals(BONUS_HASTE) ? "Haste Boost" : "Excavation Boost";

        // Check if any player already had this bonus active (means it was extended)
        boolean wasExtended = false;
        int maxRemainingSeconds = 0;
        for (ServerPlayer p : APRandomizer.getServer().getPlayerList().getPlayers()) {
            int remaining = getRemainingSeconds(p.getUUID(), bonusType);
            if (remaining > BONUS_DURATION_SECONDS) {
                wasExtended = true;
                maxRemainingSeconds = Math.max(maxRemainingSeconds, remaining);
            }
        }

        if (wasExtended) {
            Utils.sendMessageToAll("§6" + bonusName + " extended! §7(" + maxRemainingSeconds + "s remaining)");
        } else {
            Utils.sendMessageToAll("§aReceived " + bonusName + "! §7(" + BONUS_DURATION_SECONDS + " seconds)");
        }
    }

    /**
     * Add bonus time to a specific player (stacks with existing)
     */
    private static void addBonusToPlayer(UUID uuid, String bonusType, int ticks) {
        activeBonuses.computeIfAbsent(uuid, k -> new HashMap<>());
        HashMap<String, Integer> playerBonuses = activeBonuses.get(uuid);

        int currentTicks = playerBonuses.getOrDefault(bonusType, 0);
        playerBonuses.put(bonusType, currentTicks + ticks);
    }

    /**
     * Apply the actual effect to an online player
     */
    private static void applyBonusEffect(ServerPlayer player, String bonusType) {
        if (bonusType.equals(BONUS_HASTE)) {
            // Haste effect will be maintained by tick handler
            LOGGER.info("Applying Haste Boost to {}", player.getName().getString());
        } else if (bonusType.equals(BONUS_EXCAVATION)) {
            // Max out excavation level
            if (excavationBoostActivePlayers == 0) {
                originalExcavationLevel = ExcavationPower.level;
                ExcavationPower.level = 3; // Max level
                LOGGER.info("Excavation Boost activated! Level {} -> 3", originalExcavationLevel);
            }
            excavationBoostActivePlayers++;
        }
    }

    /**
     * Check if a player is currently online
     */
    private static boolean isPlayerOnline(UUID uuid) {
        if (APRandomizer.getServer() == null) return false;
        return APRandomizer.getServer().getPlayerList().getPlayer(uuid) != null;
    }

    /**
     * Called when a player joins - apply any pending bonuses
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();

        // Register as known player (use WorldData directly)
        if (APRandomizer.worldData != null) {
            APRandomizer.worldData.addKnownPlayer(uuid);
        }

        // Check for pending bonuses
        if (APRandomizer.worldData != null) {
            HashMap<String, Integer> pendingBonuses = APRandomizer.worldData.getPendingBonuses(uuid);

            if (pendingBonuses != null && !pendingBonuses.isEmpty()) {
                for (Map.Entry<String, Integer> entry : pendingBonuses.entrySet()) {
                    String bonusType = entry.getKey();
                    int seconds = entry.getValue();
                    int ticks = seconds * 20;

                    addBonusToPlayer(uuid, bonusType, ticks);
                    applyBonusEffect(player, bonusType);

                    String bonusName = bonusType.equals(BONUS_HASTE) ? "Haste Boost" : "Excavation Boost";
                    Utils.sendMessageToPlayer(player, "You received " + bonusName + " while offline! (" + seconds + " seconds)");
                }

                // Clear pending bonuses
                APRandomizer.worldData.clearPendingBonuses(uuid);
            }
        }
    }

    /**
     * Tick handler to manage active bonuses
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (APRandomizer.getServer() == null) return;

        // Track if any player still has excavation boost
        boolean anyExcavationBoost = false;

        // Process each player's active bonuses
        Iterator<Map.Entry<UUID, HashMap<String, Integer>>> playerIterator = activeBonuses.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, HashMap<String, Integer>> playerEntry = playerIterator.next();
            UUID uuid = playerEntry.getKey();
            HashMap<String, Integer> bonuses = playerEntry.getValue();

            ServerPlayer player = APRandomizer.getServer().getPlayerList().getPlayer(uuid);

            // Decrement all bonus timers
            Iterator<Map.Entry<String, Integer>> bonusIterator = bonuses.entrySet().iterator();
            while (bonusIterator.hasNext()) {
                Map.Entry<String, Integer> bonusEntry = bonusIterator.next();
                String bonusType = bonusEntry.getKey();
                int remainingTicks = bonusEntry.getValue() - 1;

                if (remainingTicks <= 0) {
                    // Bonus expired
                    bonusIterator.remove();

                    if (bonusType.equals(BONUS_EXCAVATION)) {
                        excavationBoostActivePlayers = Math.max(0, excavationBoostActivePlayers - 1);
                    }

                    if (player != null) {
                        String bonusName = bonusType.equals(BONUS_HASTE) ? "Haste Boost" : "Excavation Boost";
                        Utils.sendMessageToPlayer(player, bonusName + " has expired!");
                    }
                } else {
                    bonusEntry.setValue(remainingTicks);

                    // Apply ongoing effects
                    if (player != null) {
                        if (bonusType.equals(BONUS_HASTE)) {
                            // Apply haste effect (level 2 = Haste III)
                            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, true, true));
                        }
                    }

                    if (bonusType.equals(BONUS_EXCAVATION)) {
                        anyExcavationBoost = true;
                    }
                }
            }

            // Remove player entry if no active bonuses
            if (bonuses.isEmpty()) {
                playerIterator.remove();
            }
        }

        // Restore excavation level if no one has the boost anymore
        if (!anyExcavationBoost && originalExcavationLevel >= 0 && excavationBoostActivePlayers == 0) {
            ExcavationPower.level = originalExcavationLevel;
            LOGGER.info("Excavation Boost expired, level restored to {}", originalExcavationLevel);
            originalExcavationLevel = -1;
        }
    }

    /**
     * Check if a player has an active bonus
     */
    public static boolean hasActiveBonus(UUID uuid, String bonusType) {
        HashMap<String, Integer> bonuses = activeBonuses.get(uuid);
        return bonuses != null && bonuses.getOrDefault(bonusType, 0) > 0;
    }

    /**
     * Get remaining seconds for a bonus
     */
    public static int getRemainingSeconds(UUID uuid, String bonusType) {
        HashMap<String, Integer> bonuses = activeBonuses.get(uuid);
        if (bonuses == null) return 0;
        return bonuses.getOrDefault(bonusType, 0) / 20;
    }
}
