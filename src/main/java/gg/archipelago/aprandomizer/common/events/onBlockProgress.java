package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.BlockProgressManager;
import gg.archipelago.aprandomizer.managers.BlocksBrokenManager;
import gg.archipelago.aprandomizer.managers.FossilManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.CustomAdvancementHandler;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import gg.archipelago.aprandomizer.managers.itemmanager.powers.ExcavationPower;
import gg.archipelago.aprandomizer.mixin.ServerPlayerGameModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class onBlockProgress {

    // Track what block each player was mining last tick
    private static final Map<UUID, BlockPos> lastMiningPos = new HashMap<>();

    /**
     * When a block is broken, clear its stored progress
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        BlockProgressManager.clearProgress(pos);

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockProgressManager.removeCrackAnimation(serverLevel, pos);
        }
    }

    /**
     * When a player disconnects, unregister them from all blocks
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BlockProgressManager.unregisterMinerFromAll(serverPlayer);
            lastMiningPos.remove(serverPlayer.getUUID());
        }
    }

    /**
     * Track when players start/stop breaking blocks and manage shared progress
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        if (!(event.player instanceof ServerPlayer serverPlayer))
            return;

        if (APRandomizer.isJailPlayers())
            return;

        ServerLevel level = serverPlayer.getLevel();
        UUID playerId = serverPlayer.getUUID();

        // Get the block the player is currently mining (if any)
        BlockPos currentMiningPos = getMiningBlock(serverPlayer);
        BlockPos lastPos = lastMiningPos.get(playerId);

        // Check if player switched blocks or stopped mining
        if (lastPos != null && (currentMiningPos == null || !currentMiningPos.equals(lastPos))) {
            BlockProgressManager.unregisterMiner(lastPos, serverPlayer);
        }

        if (currentMiningPos != null) {
            BlockState state = level.getBlockState(currentMiningPos);

            if (!state.isAir()) {
                // Calculate this player's dig speed for this block
                float playerDigSpeed = serverPlayer.getDigSpeed(state, currentMiningPos);

                // Apply efficiency upgrade multiplier
                playerDigSpeed *= ItemManager.getEfficiencyMultiplier();

                // Register this player as mining this block with their dig speed
                BlockProgressManager.registerMiner(currentMiningPos, serverPlayer, playerDigSpeed);
                lastMiningPos.put(playerId, currentMiningPos.immutable());

                // Get block hardness
                float destroySpeed = state.getDestroySpeed(level, currentMiningPos);

                if (destroySpeed >= 0) {
                    // Add THIS player's progress contribution only
                    boolean shouldBreak = BlockProgressManager.addProgress(currentMiningPos, playerDigSpeed, destroySpeed);
                    float currentProgress = BlockProgressManager.getProgress(currentMiningPos);

                    // Send crack animation to all players
                    BlockProgressManager.sendCrackAnimation(level, currentMiningPos, currentProgress);

                    // Also add progress to excavation target blocks (skip if using True Golden Pick)
                    boolean isTruePick = serverPlayer.getMainHandItem().getOrCreateTag().getBoolean("truepick");
                    Set<BlockPos> excavationTargets = isTruePick ?
                        Set.of() : ExcavationPower.getExcavationTargets(currentMiningPos, serverPlayer, level);
                    for (BlockPos excavatePos : excavationTargets) {
                        BlockState excavateState = level.getBlockState(excavatePos);
                        if (!excavateState.isAir()) {
                            float excavateDigSpeed = serverPlayer.getDigSpeed(excavateState, excavatePos);
                            // Apply efficiency upgrade multiplier
                            excavateDigSpeed *= ItemManager.getEfficiencyMultiplier();
                            BlockProgressManager.registerMiner(excavatePos, serverPlayer, excavateDigSpeed);

                            float excavateDestroySpeed = excavateState.getDestroySpeed(level, excavatePos);
                            if (excavateDestroySpeed >= 0) {
                                boolean excavateShouldBreak = BlockProgressManager.addProgress(excavatePos, excavateDigSpeed, excavateDestroySpeed);
                                float excavateProgress = BlockProgressManager.getProgress(excavatePos);
                                BlockProgressManager.sendCrackAnimation(level, excavatePos, excavateProgress);

                                if (excavateShouldBreak) {
                                    BlockProgressManager.clearProgress(excavatePos);
                                    BlockProgressManager.removeCrackAnimation(level, excavatePos);
                                    FossilManager.checkAndCollectFossil(excavatePos, serverPlayer);
                                    level.destroyBlock(excavatePos, true, serverPlayer);
                                    BlocksBrokenManager.addBlockBroken(serverPlayer);
                                    APRandomizer.getLayerManager().addLayerCheck(excavatePos.getY());

                                    // Damage tool for excavation
                                    ItemStack tool = serverPlayer.getMainHandItem();
                                    if (tool.isDamageableItem()) {
                                        tool.hurtAndBreak(1, serverPlayer, (p) -> p.broadcastBreakEvent(serverPlayer.getUsedItemHand()));
                                    }
                                }
                            }
                        }
                    }

                    // If progress reached 100%, break the block
                    if (shouldBreak) {
                        // Clear progress first
                        BlockProgressManager.clearProgress(currentMiningPos);
                        BlockProgressManager.removeCrackAnimation(level, currentMiningPos);
                        lastMiningPos.remove(playerId);

                        FossilManager.checkAndCollectFossil(currentMiningPos, serverPlayer);

                        // Check for True Golden Pick
                        if (serverPlayer.getMainHandItem().getOrCreateTag().getBoolean("truepick")) {
                            handleTrueGoldenPick(serverPlayer, level, currentMiningPos);
                        } else {
                            // Actually break the block with drops
                            level.destroyBlock(currentMiningPos, true, serverPlayer);

                            // Count for scoreboard
                            BlocksBrokenManager.addBlockBroken(serverPlayer);
                        }

                        // Check layer
                        APRandomizer.getLayerManager().addLayerCheck(currentMiningPos.getY());
                    }
                }
            }
        } else {
            lastMiningPos.remove(playerId);
        }
    }

    /**
     * Get the block position the player is currently mining, or null if not mining
     */
    private static BlockPos getMiningBlock(ServerPlayer player) {
        ServerPlayerGameModeAccessor accessor = (ServerPlayerGameModeAccessor) player.gameMode;
        if (accessor.isDestroyingBlock()) {
            return accessor.getDestroyPos();
        }
        return null;
    }

    /**
     * Handle True Golden Pick effect - clears entire layer in chunk
     */
    private static void handleTrueGoldenPick(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Disable block counting for True Golden Pick
        BlocksBrokenManager.setCountingEnabled(false);

        int layer = pos.getY();
        // Clear only the chunk the player is standing in
        int cx = (int) Math.floor(pos.getX() / 16.0);
        int cz = (int) Math.floor(pos.getZ() / 16.0);
        int ox = cx * 16;
        int oz = cz * 16;
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                BlockPos blockPos = new BlockPos(x, layer, z);
                FossilManager.checkAndCollectFossil(blockPos, player);
                level.destroyBlock(blockPos, true);
            }
        }

        // Remove the True Golden Pick from inventory
        player.getInventory().removeItem(player.getMainHandItem());

        // Grant advancement
        CustomAdvancementHandler.grantAdvancement(player, new ResourceLocation(APRandomizer.MODID, "archipelago/use_true_pick"));

        // Re-enable block counting
        BlocksBrokenManager.setCountingEnabled(true);
    }
}
