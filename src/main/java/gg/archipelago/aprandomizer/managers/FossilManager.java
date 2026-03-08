package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class FossilManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Fossil density: 2% chance per candidate block
    private static final double FOSSIL_DENSITY = 0.005;

    // X-ray duration in ticks (15 seconds = 300 ticks)
    private static final int XRAY_DURATION_TICKS = 300;

    // World seed for deterministic fossil generation
    private static long worldSeed = 0;

    // Active X-ray shulkers mapped by their BlockPos (for removal when block is mined)
    private static final Map<Long, Shulker> activeXrayShulkers = new HashMap<>();

    // End tick for current X-ray session
    private static int xrayEndTick = 0;

    /**
     * Initialize the FossilManager with the world seed
     */
    public static void initialize(long seed) {
        worldSeed = seed;
        LOGGER.info("FossilManager initialized with seed: {}", seed);
    }

    /**
     * Generate fossils in the world. This should be called once after world generation.
     * Only places fossils where solid blocks exist.
     * @param level The server level
     * @param chunkCount Number of chunks in the world
     */
    public static void generateFossils(ServerLevel level, int chunkCount) {
        generateFossils(level, chunkCount, false);
    }

    /**
     * Generate fossils with option to force regeneration
     */
    public static void generateFossils(ServerLevel level, int chunkCount, boolean force) {
        LOGGER.info("=== FOSSIL GENERATION START ===");
        LOGGER.info("worldData null? {}", APRandomizer.worldData == null);
        LOGGER.info("worldSeed: {}", worldSeed);
        LOGGER.info("chunkCount: {}", chunkCount);

        if (APRandomizer.worldData == null) {
            LOGGER.error("Cannot generate fossils: worldData is null");
            return;
        }

        // Don't regenerate if already done (unless forced)
        if (!force && APRandomizer.worldData.areFossilsGenerated()) {
            LOGGER.info("Fossils already generated ({} fossils), skipping",
                APRandomizer.worldData.getGeneratedFossils().size());
            return;
        }

        // If forcing, clear old fossils
        if (force) {
            LOGGER.info("Force regenerating fossils, clearing old data...");
            APRandomizer.worldData.setGeneratedFossils(new HashSet<>());
        }

        LOGGER.info("Generating fossils for {} chunks...", chunkCount);

        int side = (int) Math.ceil(Math.sqrt(chunkCount));
        int fossilCount = 0;
        int solidBlocks = 0;
        int airBlocks = 0;
        int bedrockBlocks = 0;

        LOGGER.info("Grid side: {}", side);

        // Scan all chunks and Y levels
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int cx = chunkIndex % side;
            int cz = chunkIndex / side;
            int baseX = cx * 16;
            int baseZ = cz * 16;

            LOGGER.info("Processing chunk {} at cx={}, cz={}, baseX={}, baseZ={}",
                chunkIndex, cx, cz, baseX, baseZ);

            // Force load the chunk to ensure blocks are accessible
            net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
            if (chunk == null) {
                LOGGER.warn("Chunk {},{} is null, skipping", cx, cz);
                continue;
            }

            int chunkSolid = 0;
            int chunkFossils = 0;

            // Scan each block in the chunk
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -63; y <= 128; y++) {
                        BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                        BlockState blockState = level.getBlockState(pos);

                        // Skip air
                        if (blockState.isAir()) {
                            airBlocks++;
                            continue;
                        }

                        // Skip bedrock
                        if (blockState.is(Blocks.BEDROCK)) {
                            bedrockBlocks++;
                            continue;
                        }

                        // Skip non-full blocks (stairs, fences, slabs, vines, etc.)
                        if (!blockState.isCollisionShapeFullBlock(level, pos)) {
                            continue;
                        }

                        solidBlocks++;
                        chunkSolid++;

                        // Use deterministic random based on position
                        long posHash = (worldSeed ^ (pos.getX() * 73856093L) ^
                                       (pos.getY() * 19349663L) ^ (pos.getZ() * 83492791L));
                        Random posRng = new Random(posHash);

                        // Check if this position should be a fossil
                        if (posRng.nextDouble() < FOSSIL_DENSITY) {
                            APRandomizer.worldData.addGeneratedFossil(pos.asLong());
                            fossilCount++;
                            chunkFossils++;
                        }
                    }
                }
            }

            LOGGER.info("Chunk {} done: {} solid blocks, {} fossils", chunkIndex, chunkSolid, chunkFossils);
        }

        LOGGER.info("=== FOSSIL GENERATION COMPLETE ===");
        LOGGER.info("Total: {} air, {} bedrock, {} solid blocks, {} fossils",
            airBlocks, bedrockBlocks, solidBlocks, fossilCount);
        LOGGER.info("Fossils in worldData: {}", APRandomizer.worldData.getGeneratedFossils().size());

        // Save the world data
        APRandomizer.getServer().execute(() -> {
            APRandomizer.getServer().saveEverything(true, true, true);
        });
    }

    /**
     * Force regenerate fossils (for debugging)
     */
    public static void forceRegenerateFossils(ServerLevel level, int chunkCount) {
        generateFossils(level, chunkCount, true);
    }

    /**
     * Debug: print fossil status
     */
    public static String getFossilStatus() {
        if (APRandomizer.worldData == null) {
            return "worldData is null";
        }
        int generated = APRandomizer.worldData.getGeneratedFossils().size();
        int collected = APRandomizer.worldData.getCollectedFossils().size();
        int balance = APRandomizer.worldData.getFossilBalance();
        return String.format("Generated: %d, Collected: %d, Balance: %d, Seed: %d",
            generated, collected, balance, worldSeed);
    }

    /**
     * Check if a block position contains a fossil (checks stored positions)
     */
    public static boolean isFossilBlock(BlockPos pos) {
        if (APRandomizer.worldData == null) return false;
        return APRandomizer.worldData.isFossilPosition(pos.asLong());
    }

    /**
     * Check if a block contains a fossil and collect it if so.
     * Note: X-ray shulker removal should be done by the caller (onBlockBreak) before calling this.
     * @param pos The block position
     * @param player The player who broke the block (can be null for explosions)
     * @return true if a fossil was collected
     */
    public static boolean checkAndCollectFossil(BlockPos pos, ServerPlayer player) {
        if (APRandomizer.worldData == null) return false;

        long posLong = pos.asLong();

        // Check if this position is a fossil
        if (!APRandomizer.worldData.isFossilPosition(posLong)) return false;

        // Check if already collected
        if (APRandomizer.worldData.isFossilCollected(posLong)) return false;

        // Mark as collected
        APRandomizer.worldData.markFossilCollected(posLong);

        // Add to balance
        APRandomizer.worldData.addFossils(1);
        int newBalance = APRandomizer.worldData.getFossilBalance();

        // Get the level for effects
        ServerLevel level = APRandomizer.getServer().overworld();

        // Feedback: particles
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            20, 0.5, 0.5, 0.5, 0.1);

        // Feedback: sound for all nearby players
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP,
            SoundSource.BLOCKS, 1.0f, 1.5f);

        // Feedback: chat message
        String playerName = player != null ? player.getName().getString() : "An explosion";
        Utils.sendMessageToAll("§a" + playerName + " found a fossil! §7Balance: §e" + newBalance);

        LOGGER.info("Fossil collected at {} by {}. New balance: {}", pos, playerName, newBalance);

        // Save the world data
        APRandomizer.getServer().execute(() -> {
            APRandomizer.getServer().saveEverything(true, true, true);
        });

        return true;
    }

    /**
     * Activate Fossil X-ray for ALL online players.
     * Called when receiving the item from Archipelago.
     * Spawns glowing invisible shulkers at fossil positions.
     */
    public static void activateFossilXrayForAll() {
        ServerLevel level = APRandomizer.getServer().overworld();
        int currentTick = APRandomizer.getServer().getTickCount();

        boolean isExtension = !activeXrayShulkers.isEmpty() && currentTick <= xrayEndTick;

        if (isExtension) {
            // Extend existing X-ray duration
            xrayEndTick += XRAY_DURATION_TICKS;
            int remainingSeconds = (xrayEndTick - currentTick) / 20;

            // Extend invisibility on existing shulkers
            for (Shulker shulker : activeXrayShulkers.values()) {
                if (!shulker.isRemoved()) {
                    shulker.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY,
                        xrayEndTick - currentTick + 20,
                        0, true, false
                    ));
                }
            }

            // Also spawn shulkers for any new fossils in range
            int newShulkers = spawnNewXrayShulkers(level);

            Utils.sendMessageToAll("§d[Archipelago] §6Fossil X-ray extended! §7(" + remainingSeconds + "s remaining)");
            LOGGER.info("Fossil X-ray extended, {} new shulkers, end tick: {}", newShulkers, xrayEndTick);
        } else {
            // Fresh activation
            clearAllXrayShulkers();
            xrayEndTick = currentTick + XRAY_DURATION_TICKS;

            int totalShulkers = spawnNewXrayShulkers(level);

            Utils.sendMessageToAll("§d[Archipelago] §eFossil X-ray received! Look for glowing outlines nearby!");
            LOGGER.info("Fossil X-ray activated, spawned {} shulkers", totalShulkers);
        }

        for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
            player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static int spawnNewXrayShulkers(ServerLevel level) {
        int totalShulkers = 0;
        for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
            List<BlockPos> fossilPositions = findFossilsNearPlayer(player, level);

            for (BlockPos pos : fossilPositions) {
                long posLong = pos.asLong();

                if (activeXrayShulkers.containsKey(posLong)) {
                    continue;
                }

                Shulker shulker = spawnXrayShulker(level, pos);
                if (shulker != null) {
                    activeXrayShulkers.put(posLong, shulker);
                    totalShulkers++;
                }
            }
        }
        return totalShulkers;
    }

    /**
     * Spawn an X-ray shulker at the given position with proper NBT tags
     */
    private static Shulker spawnXrayShulker(ServerLevel level, BlockPos pos) {
        Shulker shulker = EntityType.SHULKER.create(level);
        if (shulker == null) return null;

        shulker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // NoAI
        shulker.setNoAi(true);

        // Silent
        shulker.setSilent(true);

        // Invulnerable
        shulker.setInvulnerable(true);

        // Glowing
        shulker.setGlowingTag(true);

        // Set Peek:0 via NBT (closed shell)
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        shulker.saveWithoutId(nbt);
        nbt.putByte("Peek", (byte) 0);
        shulker.load(nbt);

        // Invisibility effect (Id:14) with duration matching X-ray duration
        MobEffectInstance invisibility = new MobEffectInstance(
            MobEffects.INVISIBILITY,
            XRAY_DURATION_TICKS + 20,
            0,
            true,
            false
        );
        shulker.addEffect(invisibility);

        // Tag for identification
        shulker.addTag("fossil_xray");

        level.addFreshEntity(shulker);

        return shulker;
    }

    /**
     * Find all uncollected fossils near a player
     */
    private static List<BlockPos> findFossilsNearPlayer(ServerPlayer player, ServerLevel level) {
        List<BlockPos> fossils = new ArrayList<>();
        if (APRandomizer.worldData == null) return fossils;

        BlockPos playerPos = player.blockPosition();
        int radius = 15;

        // Check stored fossil positions within range
        for (Long posLong : APRandomizer.worldData.getGeneratedFossils()) {
            // Skip if already collected
            if (APRandomizer.worldData.isFossilCollected(posLong)) {
                continue;
            }

            BlockPos fossilPos = BlockPos.of(posLong);

            // Check if within radius of player
            if (Math.abs(fossilPos.getX() - playerPos.getX()) <= radius &&
                Math.abs(fossilPos.getY() - playerPos.getY()) <= radius &&
                Math.abs(fossilPos.getZ() - playerPos.getZ()) <= radius) {

                // Verify the block is still solid (not already mined)
                BlockState blockState = level.getBlockState(fossilPos);
                if (!blockState.isAir()) {
                    fossils.add(fossilPos);
                }
            }
        }

        return fossils;
    }

    /**
     * Remove X-ray shulker at a specific position (called when block is mined)
     */
    public static void removeXrayShulkerAt(long posLong) {
        Shulker shulker = activeXrayShulkers.remove(posLong);
        if (shulker != null) {
            shulker.setInvulnerable(false);
            shulker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            LOGGER.debug("Removed X-ray shulker at position {}", posLong);
        }
    }

    /**
     * Remove X-ray shulker at a specific BlockPos (convenience method)
     */
    public static void removeXrayShulkerAt(BlockPos pos) {
        removeXrayShulkerAt(pos.asLong());
    }

    /**
     * Clear all active X-ray shulkers
     */
    public static void clearAllXrayShulkers() {
        for (Shulker shulker : activeXrayShulkers.values()) {
            if (shulker != null) {
                shulker.setInvulnerable(false);
                shulker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        activeXrayShulkers.clear();
        xrayEndTick = 0;
    }

    /**
     * Call this every tick to check if X-ray has expired
     */
    public static void tickXraySessions() {
        if (activeXrayShulkers.isEmpty()) return;

        int currentTick = APRandomizer.getServer().getTickCount();

        if (currentTick > xrayEndTick) {
            clearAllXrayShulkers();
            LOGGER.info("Fossil X-ray expired, removed all shulkers");
        }
    }

    /**
     * Get the world seed used for fossil generation
     */
    public static long getWorldSeed() {
        return worldSeed;
    }
}
