package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class onServerTick {

    static double count = 0;
    @SubscribeEvent
    static public void serverTickEvent(TickEvent.ServerTickEvent event) {
        if(APRandomizer.isJailPlayers())
            return;

        // Check for players at Y=-100 or below (every tick for responsiveness)
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.getY() <= -100) {
                // Teleport to spawn platform (always accessible)
                // This prevents softlock when falling into unlocked chunk areas
                player.teleportTo(-3.0, 130.0, -3.0);
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
