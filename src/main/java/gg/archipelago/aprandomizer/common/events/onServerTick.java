package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.FlyManager;
import gg.archipelago.aprandomizer.managers.FossilManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class onServerTick {

    static double count = 0;

    @SubscribeEvent
    static public void serverTickEvent(TickEvent.ServerTickEvent event) {
        // Only run at END phase (after entity ticking) to avoid ConcurrentModificationException
        if (event.phase != TickEvent.Phase.END) return;

        // Tick X-ray sessions
        FossilManager.tickXraySessions();

        // Tick async fossil generation (one chunk per tick)
        FossilManager.tickGeneration();

        // Check for fossils destroyed by fire, pistons, etc.
        FossilManager.tickOrphanFossilCheck();

        // Clean up X-ray shulkers that are no longer inside a solid block, just to be safe
        FossilManager.tickStaleShulkerCleanup();

        // Tick temporary flight timers
        FlyManager.tick();

        if(APRandomizer.isJailPlayers())
            return;

        // Check for players at Y=-100 or below (every tick for responsiveness)
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.getY() <= -100) {
                // Teleport to spawn platform — use full teleportTo to avoid safe-position search
                ServerLevel level = player.getLevel();
                player.teleportTo(level, -2.5, 129.0, -2.5, player.getYRot(), player.getXRot());
            }
        }

        if(++count < 20)
            return;
        count = 0;
        int side = APRandomizer.getChunkSide();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            int y = (int) Math.floor(player.getY());
            int chunkX = (int) Math.floor(player.getX() / 16.0);
            int chunkZ = (int) Math.floor(player.getZ() / 16.0);

            // Calculate chunk index (same formula as LayerManager: cz * side + cx)
            if (chunkX >= 0 && chunkX < side && chunkZ >= 0 && chunkZ < side) {
                int chunkIndex = chunkZ * side + chunkX;
                Utils.sendActionBarToPlayer(player, "Chunk " + chunkIndex + "  |  Y level: " + y);
            } else {
                Utils.sendActionBarToPlayer(player, "Y level: " + y);
            }
        }
    }
}
