package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class onPlayerClone {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Delay teleport by 1 tick so it happens AFTER vanilla respawn positioning
        APRandomizer.getServer().execute(() -> {
            BlockPos pos = player.getLevel().getSharedSpawnPos();
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            LOGGER.debug("Respawned {} at spawn platform ({}, {}, {})", player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
        });
    }
}
