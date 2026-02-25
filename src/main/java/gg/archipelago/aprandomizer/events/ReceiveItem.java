package gg.archipelago.aprandomizer.events;

import gg.archipelago.aprandomizer.APRandomizer;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ReceiveItemEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ReceiveItem {

    @ArchipelagoEventListener
    public static void onReceiveItem(ReceiveItemEvent event) {
        APRandomizer.getItemManager().giveItemToAll(event.getItemID(), event.getIndex());

        // Spawn particles around all players when receiving an item
        APRandomizer.getServer().execute(() -> {
            for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                ServerLevel level = player.getLevel();

                // Spawn happy villager particles (green sparkles) around the player
                level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    15,  // count
                    0.5, // xDist
                    0.5, // yDist
                    0.5, // zDist
                    0.1  // speed
                );

                // Also spawn some totem particles for extra effect
                level.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    10,  // count
                    0.3, // xDist
                    0.5, // yDist
                    0.3, // zDist
                    0.2  // speed
                );

                // Play a sound effect
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        });
    }
}
