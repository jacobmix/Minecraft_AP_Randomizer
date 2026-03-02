package gg.archipelago.aprandomizer.managers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Manages shared persistent block breaking progress with combined tool speeds.
 * Progress is shared between all players mining the same block.
 * Speed is the sum of all miners' dig speeds.
 */
public class BlockProgressManager {

    // Maps BlockPos to breaking progress (0.0 to 1.0)
    private static final Map<BlockPos, Float> blockProgress = new HashMap<>();

    // Maps BlockPos to the entity ID used for crack animation
    private static final Map<BlockPos, Integer> blockBreakerId = new HashMap<>();

    // Maps BlockPos to map of player UUID -> their current dig speed for this block
    private static final Map<BlockPos, Map<UUID, Float>> activeMinerSpeeds = new HashMap<>();

    private static int nextBreakerId = -1000;

    /**
     * Get the current progress for a block (0.0 to 1.0)
     */
    public static float getProgress(BlockPos pos) {
        return blockProgress.getOrDefault(pos, 0.0f);
    }

    /**
     * Set the progress for a block (0.0 to 1.0)
     */
    public static void setProgress(BlockPos pos, float progress) {
        if (progress <= 0.0f) {
            blockProgress.remove(pos);
        } else {
            blockProgress.put(pos.immutable(), Math.min(progress, 1.0f));
        }
    }

    /**
     * Register a player as actively mining a block with their dig speed
     */
    public static void registerMiner(BlockPos pos, ServerPlayer player, float digSpeed) {
        BlockPos immutablePos = pos.immutable();
        activeMinerSpeeds.computeIfAbsent(immutablePos, k -> new HashMap<>()).put(player.getUUID(), digSpeed);
    }

    /**
     * Unregister a player from mining a block
     */
    public static void unregisterMiner(BlockPos pos, ServerPlayer player) {
        Map<UUID, Float> miners = activeMinerSpeeds.get(pos);
        if (miners != null) {
            miners.remove(player.getUUID());
            if (miners.isEmpty()) {
                activeMinerSpeeds.remove(pos);
            }
        }
    }

    /**
     * Unregister a player from all blocks they might be mining
     */
    public static void unregisterMinerFromAll(ServerPlayer player) {
        UUID playerId = player.getUUID();
        activeMinerSpeeds.values().forEach(miners -> miners.remove(playerId));
        activeMinerSpeeds.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Get the number of players currently mining a block
     */
    public static int getMinerCount(BlockPos pos) {
        Map<UUID, Float> miners = activeMinerSpeeds.get(pos);
        return miners != null ? miners.size() : 0;
    }

    /**
     * Get the combined dig speed of all miners on a block (sum of all speeds)
     */
    public static float getCombinedDigSpeed(BlockPos pos) {
        Map<UUID, Float> miners = activeMinerSpeeds.get(pos);
        if (miners == null || miners.isEmpty()) return 0.0f;

        float totalSpeed = 0.0f;
        for (Float speed : miners.values()) {
            totalSpeed += speed;
        }
        return totalSpeed;
    }

    /**
     * Add progress to a block from a single player's dig speed
     * @param playerDigSpeed The player's dig speed for this block
     * @param destroySpeed The block's destroy speed (hardness)
     * @return true if block should be broken (progress >= 1.0)
     */
    public static boolean addProgress(BlockPos pos, float playerDigSpeed, float destroySpeed) {
        if (playerDigSpeed <= 0 || destroySpeed < 0) return false;

        float progressPerTick;
        if (playerDigSpeed > destroySpeed * 30.0f) {
            // Instant break
            progressPerTick = 1.0f;
        } else {
            progressPerTick = playerDigSpeed / destroySpeed / 30.0f;
        }

        float current = getProgress(pos);
        float newProgress = Math.min(current + progressPerTick, 1.0f);
        setProgress(pos, newProgress);
        return newProgress >= 1.0f;
    }

    /**
     * Check if a block has any progress (was being mined through our system)
     */
    public static boolean hasProgress(BlockPos pos) {
        return blockProgress.containsKey(pos) || activeMinerSpeeds.containsKey(pos);
    }

    /**
     * Clear progress for a block (called when block is broken)
     */
    public static void clearProgress(BlockPos pos) {
        blockProgress.remove(pos);
        blockBreakerId.remove(pos);
        activeMinerSpeeds.remove(pos);
    }

    /**
     * Get or create a breaker ID for crack animation
     */
    public static int getBreakerId(BlockPos pos) {
        return blockBreakerId.computeIfAbsent(pos.immutable(), k -> nextBreakerId--);
    }

    /**
     * Send crack animation to all players
     * @param level The server level
     * @param pos Block position
     * @param progress Progress from 0.0 to 1.0
     */
    public static void sendCrackAnimation(ServerLevel level, BlockPos pos, float progress) {
        int breakerId = getBreakerId(pos);
        // Convert 0.0-1.0 to 0-9 for Minecraft's crack stages (-1 to remove)
        int stage = progress >= 1.0f ? -1 : (int)(progress * 10.0f);
        level.destroyBlockProgress(breakerId, pos, stage);
    }

    /**
     * Remove crack animation
     */
    public static void removeCrackAnimation(ServerLevel level, BlockPos pos) {
        if (blockBreakerId.containsKey(pos)) {
            int breakerId = blockBreakerId.get(pos);
            level.destroyBlockProgress(breakerId, pos, -1);
        }
    }

    /**
     * Clear all progress (called on world unload)
     */
    public static void clearAll() {
        blockProgress.clear();
        blockBreakerId.clear();
        activeMinerSpeeds.clear();
    }
}
