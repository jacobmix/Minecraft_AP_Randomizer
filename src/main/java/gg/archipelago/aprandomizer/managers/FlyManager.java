package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FlyManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String FLY_REMAINING = "fly_remaining";
    private static final String FLY_COOLDOWN = "fly_cooldown";

    // Duration of flight in ticks (1 minute = 1200 ticks)
    public static final int FLY_DURATION_TICKS = 1200;
    // Cooldown in ticks (10 minutes = 12000 ticks)
    public static final int FLY_COOLDOWN_TICKS = 12000;

    private static CompoundTag getData(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static boolean canFly(ServerPlayer player) {
        return getData(player).getInt(FLY_COOLDOWN) <= 0;
    }

    public static boolean isFlying(ServerPlayer player) {
        return getData(player).getInt(FLY_REMAINING) > 0;
    }

    public static int getCooldownRemainingSeconds(ServerPlayer player) {
        int ticks = getData(player).getInt(FLY_COOLDOWN);
        return ticks > 0 ? ticks / 20 : 0;
    }

    public static boolean grantFlight(ServerPlayer player) {
        if (!canFly(player)) return false;

        CompoundTag data = getData(player);
        data.putInt(FLY_REMAINING, FLY_DURATION_TICKS);
        data.putInt(FLY_COOLDOWN, FLY_COOLDOWN_TICKS);

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        Utils.sendMessageToPlayer(player, "§bFlight activated! §7(60 seconds)");
        LOGGER.info("Flight granted to {}", player.getName().getString());
        return true;
    }

    private static void revokeFlight(ServerPlayer player) {
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();

        Utils.sendMessageToPlayer(player, "§cFlight has expired!");
        LOGGER.info("Flight revoked from {}", player.getName().getString());
    }

    /**
     * Called every server tick. Decrements fly and cooldown timers on all online players.
     */
    public static void tick() {
        if (APRandomizer.getServer() == null) return;

        for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
            CompoundTag data = getData(player);

            int flyRemaining = data.getInt(FLY_REMAINING);
            if (flyRemaining > 0) {
                flyRemaining--;
                data.putInt(FLY_REMAINING, flyRemaining);
                if (flyRemaining <= 0) {
                    revokeFlight(player);
                }
            }

            int cooldown = data.getInt(FLY_COOLDOWN);
            if (cooldown > 0) {
                cooldown--;
                data.putInt(FLY_COOLDOWN, cooldown);
            }
        }
    }
}
