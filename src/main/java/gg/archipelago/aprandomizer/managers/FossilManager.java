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

import gg.archipelago.aprandomizer.gui.ShopMenu;

import java.util.*;

public class FossilManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Margin over total shop cost (50%)
    private static final double FOSSIL_MARGIN = 1.5;

    // X-ray duration in ticks (15 seconds = 300 ticks)
    private static final int XRAY_DURATION_TICKS = 300;

    // World seed for deterministic fossil generation
    private static long worldSeed = 0;



    /**
     * Initialize the FossilManager with the world seed
     */
    public static void initialize(long seed) {
        worldSeed = seed;
        pendingXray = false;
        LOGGER.info("FossilManager initialized with seed: {}", seed);
    }


    // Async generation state
    private static boolean isGenerating = false;
    private static int genNextChunk = 0;
    private static int genTotalChunks = 0;
    private static int genSide = 0;
    private static int genFossilsPerChunk = 0;
    private static int genFossilsRemainder = 0;
    private static ServerLevel genLevel = null;
    private static int genTotalFossils = 0;

    public static void generateFossils(ServerLevel level, int chunkCount) {
        generateFossils(level, chunkCount, false);
    }

    /**
     * Generate fossils with option to force regeneration.
     * Schedules async generation: one chunk per tick to avoid server hang.
     * Each chunk gets exactly (targetFossils / chunkCount) fossils, placed on random solid blocks.
     */
    public static void generateFossils(ServerLevel level, int chunkCount, boolean force) {
        LOGGER.info("=== FOSSIL GENERATION START ===");

        if (APRandomizer.worldData == null) {
            LOGGER.error("Cannot generate fossils: worldData is null");
            return;
        }

        if (isGenerating) {
            LOGGER.warn("Fossil generation already in progress, ignoring");
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

        // Calculate total cost: all upgrades + item shop
        int totalCost = ShopManager.getTotalUpgradeCost() + (ShopMenu.ITEM_SHOP_COUNT * ShopMenu.ITEM_SHOP_COST);
        int targetFossils = (int) (totalCost * FOSSIL_MARGIN);

        LOGGER.info("Fossil balancing: totalCost={}, targetFossils={}, chunkCount={}",
            totalCost, targetFossils, chunkCount);

        // Set up async state
        genLevel = level;
        genTotalChunks = chunkCount;
        genSide = (int) Math.ceil(Math.sqrt(chunkCount));
        genFossilsPerChunk = targetFossils / chunkCount;
        genFossilsRemainder = targetFossils % chunkCount;
        genNextChunk = 0;
        genTotalFossils = 0;
        isGenerating = true;

        Utils.sendMessageToAll("§7Generating fossils... (0/" + chunkCount + " chunks)");
    }

    /**
     * Called every server tick. Processes one chunk per tick during generation.
     * Counts solid blocks, then picks exactly the right number as fossils.
     */
    public static void tickGeneration() {
        if (!isGenerating || genLevel == null) return;

        int chunkIndex = genNextChunk;
        int cx = chunkIndex % genSide;
        int cz = chunkIndex / genSide;
        int baseX = cx * 16;
        int baseZ = cz * 16;

        net.minecraft.world.level.chunk.LevelChunk chunk = genLevel.getChunk(cx, cz);
        if (chunk == null) {
            LOGGER.warn("Chunk {},{} is null, skipping", cx, cz);
        } else {
            // Collect all solid block positions in this chunk
            List<Long> solidPositions = new ArrayList<>();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -63; y <= 128; y++) {
                        BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                        BlockState blockState = genLevel.getBlockState(pos);

                        if (blockState.isAir()) continue;
                        if (blockState.is(Blocks.BEDROCK)) continue;
                        if (!blockState.isCollisionShapeFullBlock(genLevel, pos)) continue;

                        solidPositions.add(pos.asLong());
                    }
                }
            }

            // How many fossils for this chunk (distribute remainder to first chunks)
            int fossilsForChunk = genFossilsPerChunk + (chunkIndex < genFossilsRemainder ? 1 : 0);
            fossilsForChunk = Math.min(fossilsForChunk, solidPositions.size());

            // Deterministic shuffle based on seed + chunk index, then pick first N
            Random chunkRng = new Random(worldSeed ^ (chunkIndex * 48611L));
            Collections.shuffle(solidPositions, chunkRng);

            for (int i = 0; i < fossilsForChunk; i++) {
                APRandomizer.worldData.addGeneratedFossil(solidPositions.get(i));
            }

            genTotalFossils += fossilsForChunk;
            LOGGER.info("Chunk {}/{} done: {} solid blocks, {} fossils placed",
                chunkIndex + 1, genTotalChunks, solidPositions.size(), fossilsForChunk);
        }

        genNextChunk++;
        if (genNextChunk >= genTotalChunks) {
            // Generation complete
            isGenerating = false;
            genLevel = null;
            LOGGER.info("=== FOSSIL GENERATION COMPLETE === Total fossils: {}", genTotalFossils);
            Utils.sendMessageToAll("§aFossil generation complete! §7" + genTotalFossils + " fossils placed.");
            APRandomizer.getServer().execute(() -> {
                APRandomizer.getServer().saveEverything(true, true, true);
            });

            // Activate pending X-ray if one was deferred
            if (pendingXray) {
                pendingXray = false;
                LOGGER.info("Activating deferred Fossil X-ray");
                doActivateFossilXray();
            }
        }
    }

    /**
     * Check if fossil generation is currently in progress
     */
    public static boolean isGenerating() {
        return isGenerating;
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
     * Also kills any X-ray shulker at that position.
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

        // Kill any X-ray shulker at this position
        killShulkersAt(pos);

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

    // Pending X-ray activation (deferred until generation completes)
    private static boolean pendingXray = false;

    /**
     * Activate Fossil X-ray for ALL online players.
     * Called when receiving the item from Archipelago (possibly from AP websocket thread).
     * Delegates to server thread to avoid ConcurrentModificationException.
     */
    public static void activateFossilXrayForAll() {
        if (APRandomizer.getServer() == null) {
            LOGGER.warn("Cannot activate Fossil X-ray: server is null");
            return;
        }

        // Always execute on the server thread to avoid CME from AP websocket thread
        APRandomizer.getServer().execute(FossilManager::doActivateFossilXray);
    }

    /**
     * Internal X-ray activation — MUST run on server thread.
     */
    private static void doActivateFossilXray() {
        if (APRandomizer.getServer() == null || APRandomizer.worldData == null) {
            LOGGER.warn("Cannot activate Fossil X-ray: server or worldData is null");
            return;
        }

        // If fossils are still being generated, defer until generation completes
        if (isGenerating) {
            pendingXray = true;
            Utils.sendMessageToAll("§d[Archipelago] §eFossil X-ray received! §7Will activate when fossil generation completes...");
            LOGGER.info("Fossil X-ray deferred: generation in progress");
            return;
        }

        // If no fossils have been generated at all, defer
        if (!APRandomizer.worldData.areFossilsGenerated() ||
            APRandomizer.worldData.getGeneratedFossils().isEmpty()) {
            pendingXray = true;
            Utils.sendMessageToAll("§d[Archipelago] §eFossil X-ray received! §7Will activate when fossils are generated...");
            LOGGER.info("Fossil X-ray deferred: no fossils generated yet");
            return;
        }

        ServerLevel level = APRandomizer.getServer().overworld();

        // Check if there are existing xray shulkers in the world (extension vs fresh)
        boolean isExtension = hasXrayShulkersInWorld(level);

        if (isExtension) {
            // Find fossil positions in range of players
            Set<Long> inRangePositions = new HashSet<>();
            for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                for (BlockPos pos : findFossilsNearPlayer(player, level)) {
                    inRangePositions.add(pos.asLong());
                }
            }

            // Extend remaining time only on shulkers at in-range fossil positions
            int extended = 0;
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Shulker shulker)) continue;
                if (!shulker.getTags().contains("fossil_xray")) continue;
                if (!inRangePositions.contains(shulker.blockPosition().asLong())) continue;

                net.minecraft.nbt.CompoundTag data = shulker.getPersistentData();
                int oldRemaining = data.getInt("xray_remaining");
                data.putInt("xray_remaining", oldRemaining + XRAY_DURATION_TICKS);
                extended++;
            }

            // Spawn shulkers for any new fossils in range (not already occupied)
            int newShulkers = spawnNewXrayShulkers(level);

            Utils.sendMessageToAll("§d[Archipelago] §6Fossil X-ray extended!");
            LOGGER.info("Fossil X-ray extended, {} existing extended, {} new shulkers", extended, newShulkers);
        } else {
            // Fresh activation
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
                // Skip if there's already an xray shulker at this position
                if (hasXrayShulkerAt(level, pos)) {
                    continue;
                }

                int randomDelay = 20 + xrayRandom.nextInt(21); // 20-40 ticks
                Shulker shulker = spawnXrayShulker(level, pos, randomDelay);
                if (shulker != null) {
                    totalShulkers++;
                }
            }
        }
        return totalShulkers;
    }

    /**
     * Spawn an X-ray shulker at the given position with proper NBT tags
     */
    private static final Random xrayRandom = new Random();

    private static Shulker spawnXrayShulker(ServerLevel level, BlockPos pos, int staggerOffset) {
        Shulker shulker = EntityType.SHULKER.create(level);
        if (shulker == null) return null;

        shulker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        shulker.setNoAi(true);
        shulker.setSilent(true);
        shulker.setInvulnerable(true);
        shulker.setGlowingTag(true);
        shulker.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));

        // Countdown timer — decremented each tick, survives server restart
        shulker.getPersistentData().putInt("xray_remaining", XRAY_DURATION_TICKS + staggerOffset);

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
     * Kill any X-ray shulkers at the given block position.
     */
    public static void killShulkersAt(BlockPos pos) {
        if (APRandomizer.getServer() == null) return;
        ServerLevel level = APRandomizer.getServer().overworld();
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        for (Shulker shulker : level.getEntitiesOfClass(Shulker.class, area)) {
            shulker.setInvulnerable(false);
            shulker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * Check if there's already an xray shulker at the given position.
     */
    private static boolean hasXrayShulkerAt(ServerLevel level, BlockPos pos) {
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        for (Shulker shulker : level.getEntitiesOfClass(Shulker.class, area)) {
            if (shulker.getTags().contains("fossil_xray")) return true;
        }
        return false;
    }

    /**
     * Check if there are any fossil_xray shulkers anywhere in the world.
     */
    private static boolean hasXrayShulkersInWorld(ServerLevel level) {
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof Shulker shulker && shulker.getTags().contains("fossil_xray")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Periodically check for fossils in blocks destroyed by non-standard means
     * (fire, pistons, water, etc.) and auto-collect them.
     * Called every server tick, internally throttled to once per second.
     */
    private static int lastOrphanCheckTick = 0;
    public static void tickOrphanFossilCheck() {
        if (APRandomizer.worldData == null) return;
        if (APRandomizer.getServer() == null) return;

        int currentTick = APRandomizer.getServer().getTickCount();
        if (currentTick - lastOrphanCheckTick < 20) return;
        lastOrphanCheckTick = currentTick;

        ServerLevel level = APRandomizer.getServer().overworld();
        List<ServerPlayer> players = APRandomizer.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        for (ServerPlayer player : players) {
            BlockPos playerPos = player.blockPosition();
            for (Long posLong : APRandomizer.worldData.getGeneratedFossils()) {
                if (APRandomizer.worldData.isFossilCollected(posLong)) continue;

                BlockPos fossilPos = BlockPos.of(posLong);
                if (Math.abs(fossilPos.getX() - playerPos.getX()) > 32 ||
                    Math.abs(fossilPos.getY() - playerPos.getY()) > 32 ||
                    Math.abs(fossilPos.getZ() - playerPos.getZ()) > 32) continue;

                if (level.getBlockState(fossilPos).isAir()) {
                    killShulkersAt(fossilPos);
                    APRandomizer.worldData.markFossilCollected(posLong);
                    APRandomizer.worldData.addFossils(1);
                    int newBalance = APRandomizer.worldData.getFossilBalance();

                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        fossilPos.getX() + 0.5, fossilPos.getY() + 0.5, fossilPos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.1);

                    Utils.sendMessageToAll("§a" + player.getName().getString() + " found a fossil! §7Balance: §e" + newBalance);
                }
            }
        }
    }

    /**
     * Kill all shulkers in the world that are not inside a solid block.
     * Called every tick.
     */
    public static void tickStaleShulkerCleanup() {
        if (APRandomizer.getServer() == null) return;

        ServerLevel level = APRandomizer.getServer().overworld();
        List<Shulker> toKill = new ArrayList<>();

        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Shulker shulker)) continue;
            BlockPos shulkerPos = shulker.blockPosition();
            if (level.getBlockState(shulkerPos).isAir()) {
                toKill.add(shulker);
            }
        }

        if (!toKill.isEmpty()) {
            APRandomizer.getServer().execute(() -> {
                for (Shulker shulker : toKill) {
                    shulker.setInvulnerable(false);
                    shulker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            });
            LOGGER.debug("Cleaned up {} stale shulkers", toKill.size());
        }
    }

    /**
     * Every tick, decrement xray_remaining on all fossil_xray shulkers.
     * When it reaches 0, the shulker is removed. Freezes when no players are online.
     * Uses persistent NBT on each entity, so it survives server restarts.
     */
    public static void tickXraySessions() {
        if (APRandomizer.getServer() == null) return;

        ServerLevel level = APRandomizer.getServer().overworld();
        boolean noPlayersOnline = APRandomizer.getServer().getPlayerList().getPlayers().isEmpty();

        List<Shulker> expired = new ArrayList<>();

        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Shulker shulker)) continue;
            if (!shulker.getTags().contains("fossil_xray")) continue;

            net.minecraft.nbt.CompoundTag data = shulker.getPersistentData();
            if (!data.contains("xray_remaining")) {
                // No countdown = leftover from old version, kill it
                expired.add(shulker);
                continue;
            }

            // Freeze timer when no players are online
            if (noPlayersOnline) continue;

            int remaining = data.getInt("xray_remaining");
            remaining--;
            data.putInt("xray_remaining", remaining);

            if (remaining <= 0) {
                expired.add(shulker);
            }
        }

        for (Shulker shulker : expired) {
            shulker.setInvulnerable(false);
            shulker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * Get the world seed used for fossil generation
     */
    public static long getWorldSeed() {
        return worldSeed;
    }
}
