package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber
public class IncreasedGravityTrap implements Trap {

    private static final int DURATION_TICKS = 200;
    private static final double GRAVITY_FORCE = -0.15; // Extra downward force (on top of normal gravity)
    private static final int EXPLOSION_FALL_THRESHOLD = 5; // Blocks fallen to trigger explosion

    private static final List<GravityTracker> activeTrackers = new ArrayList<>();

    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.getServer().execute(() -> {
            boolean extended = false;
            for (ServerPlayer p : APRandomizer.getServer().getPlayerList().getPlayers()) {
                p.level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 0.3f);

                // Check if player already has an active tracker
                GravityTracker existing = findTracker(p);
                if (existing != null) {
                    existing.addTime(DURATION_TICKS);
                    extended = true;
                } else {
                    activeTrackers.add(new GravityTracker(p));
                }
            }
            if (extended) {
                int totalSeconds = findAnyTracker().ticksRemaining / 20;
                Utils.sendMessageToAll("§4§l⚠ Gravity extended! ⚠ §7(" + totalSeconds + "s remaining)");
            } else {
                Utils.sendMessageToAll("§4§l⚠ Gravity increased! ⚠");
            }
        });
    }

    private static GravityTracker findTracker(ServerPlayer player) {
        for (GravityTracker tracker : activeTrackers) {
            if (tracker.player.getUUID().equals(player.getUUID())) {
                return tracker;
            }
        }
        return null;
    }

    private static GravityTracker findAnyTracker() {
        return activeTrackers.isEmpty() ? null : activeTrackers.get(0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (activeTrackers.isEmpty()) return;

        Iterator<GravityTracker> it = activeTrackers.iterator();
        while (it.hasNext()) {
            GravityTracker tracker = it.next();
            if (tracker.tick()) {
                it.remove();
            }
        }
    }

    private static class GravityTracker {
        private final ServerPlayer player;
        private int ticksRemaining;
        private boolean isFalling = false;
        private double fallStartY = 0;
        private double highestY = 0;

        GravityTracker(ServerPlayer player) {
            this.player = player;
            this.ticksRemaining = DURATION_TICKS;
            this.highestY = player.getY();
        }

        void addTime(int ticks) {
            this.ticksRemaining += ticks;
        }

        // Returns true when done
        boolean tick() {
            if (player.isRemoved() || !player.isAlive()) return true;

            ticksRemaining--;
            if (ticksRemaining <= 0) return true;

            double currentY = player.getY();
            Vec3 vel = player.getDeltaMovement();

            // Track highest point (for calculating fall distance)
            if (currentY > highestY) {
                highestY = currentY;
            }

            // Detect when player starts descending (vel.y < 0)
            if (vel.y < -0.1 && !player.isOnGround()) {
                if (!isFalling) {
                    // Just started falling
                    isFalling = true;
                    fallStartY = highestY;
                }

                // Apply extra gravity force
                player.setDeltaMovement(vel.x, vel.y + GRAVITY_FORCE, vel.z);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));

                // Particles while falling with increased gravity
                ServerLevel level = (ServerLevel) player.level;
                double speed = Math.abs(vel.y);

                // More particles the faster they fall
                int particleCount = (int) (speed * 5);
                level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    particleCount, 0.3, 0.2, 0.3, 0.02);

                if (speed > 1.0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        2, 0.15, 0.1, 0.15, 0.01);
                }
            }

            // Reset fall distance to prevent fall damage (the explosion replaces it)
            if (isFalling) {
                player.resetFallDistance();
            }

            // Check for landing after a fall
            if (isFalling && player.isOnGround()) {
                double fallDistance = fallStartY - currentY;

                if (fallDistance >= EXPLOSION_FALL_THRESHOLD) {
                    ServerLevel level = (ServerLevel) player.level;
                    BlockPos landPos = player.blockPosition();

                    // Explosion power scales with fall distance
                    // 5 blocks = 2.0, 10 blocks = 3.0, 20 blocks = 4.0, 40+ blocks = 5.5 (capped)
                    float power = (float) Math.min(2.0 + (fallDistance - EXPLOSION_FALL_THRESHOLD) * 0.1, 5.5);

                    // Make all players temporarily invulnerable
                    List<ServerPlayer> allPlayers = level.getServer().getPlayerList().getPlayers();
                    // Save positions before explosion
                    java.util.Map<ServerPlayer, Vec3> savedPositions = new java.util.HashMap<>();
                    for (ServerPlayer p : allPlayers) {
                        p.setInvulnerable(true);
                        savedPositions.put(p, p.position());
                    }

                    // Explode slightly below ground level so it actually breaks blocks
                    level.explode(
                        null,
                        landPos.getX() + 0.5, landPos.getY() - 0.5, landPos.getZ() + 0.5,
                        power,
                        false,
                        Level.ExplosionInteraction.BLOCK
                    );

                    // Restore vulnerability and cancel knockback
                    for (ServerPlayer p : allPlayers) {
                        p.setInvulnerable(false);
                        Vec3 saved = savedPositions.get(p);
                        p.setDeltaMovement(Vec3.ZERO);
                        p.connection.send(new ClientboundSetEntityMotionPacket(p));
                    }

                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        player.getX(), player.getY(), player.getZ(),
                        1, 0, 0, 0, 0);

                    level.playSound(null, landPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5f, 0.8f);
                }

                // Reset fall tracking
                isFalling = false;
                highestY = currentY;
            }

            return false;
        }
    }
}
