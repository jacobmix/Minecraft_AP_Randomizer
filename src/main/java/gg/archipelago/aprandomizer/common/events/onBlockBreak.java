package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.BlockProgressManager;
import gg.archipelago.aprandomizer.managers.BlocksBrokenManager;
import gg.archipelago.aprandomizer.managers.FossilManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.CustomAdvancementHandler;
import gg.archipelago.aprandomizer.managers.itemmanager.powers.ExcavationPower;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

@Mod.EventBusSubscriber
public class onBlockBreak {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    static void onExplosionEvent(ExplosionEvent.Detonate event) {
        for (BlockPos affectedBlock : event.getAffectedBlocks()) {
            APRandomizer.getLayerManager().addLayerCheck(affectedBlock.getY());
            // Check for fossils in exploded blocks
            FossilManager.checkAndCollectFossil(affectedBlock, null);
        }
    }

    @SubscribeEvent
    static void onPlayerBlockInteract(BlockEvent.BreakEvent event) {
        if(APRandomizer.isJailPlayers()) {
            event.setCanceled(true);
            return;
        }

        BlockPos pos = event.getPos();
        ServerPlayer player = (ServerPlayer) event.getPlayer();

        // Check for fossils in the broken block
        FossilManager.checkAndCollectFossil(pos, player);

        if(event.getLevel().getBlockState(pos).getBlock().equals(Blocks.TNT)) {
            event.setCanceled(true);
            Block tnt = event.getLevel().getBlockState(pos).getBlock();
            tnt.onCaughtFire(event.getLevel().getBlockState(pos), event.getPlayer().getLevel(), pos, null, event.getPlayer());
            event.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        if(event.getPlayer().getMainHandItem().getOrCreateTag().getBoolean("truepick")) {
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
                    // Check for fossils before breaking
                    FossilManager.checkAndCollectFossil(blockPos, player);
                    event.getLevel().destroyBlock(blockPos, true);
                }
            }
            event.getPlayer().getInventory().removeItem(event.getPlayer().getMainHandItem());
            CustomAdvancementHandler.grantAdvancement((ServerPlayer)event.getPlayer(), new ResourceLocation(APRandomizer.MODID, "archipelago/use_true_pick"));
            APRandomizer.getLayerManager().addLayerCheck(layer);

            // Re-enable block counting
            BlocksBrokenManager.setCountingEnabled(true);
            return;
        }

        // Handle insta-mine: if block had no progress in our system, it was broken instantly
        // We need to count it and trigger excavation
        if (!BlockProgressManager.hasProgress(pos) && event.getPlayer() instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.getLevel();

            // Count the block
            BlocksBrokenManager.addBlockBroken(serverPlayer);

            // Trigger excavation for insta-mined blocks (skip if using True Golden Pick)
            boolean isTruePick = serverPlayer.getMainHandItem().getOrCreateTag().getBoolean("truepick");
            Set<BlockPos> excavationTargets = isTruePick ?
                Set.of() : ExcavationPower.getExcavationTargets(pos, serverPlayer, level);
            if (!excavationTargets.isEmpty()) {
                ItemStack tool = serverPlayer.getMainHandItem();

                for (BlockPos excavatePos : excavationTargets) {
                    BlockState excavateState = level.getBlockState(excavatePos);
                    if (!excavateState.isAir()) {
                        level.destroyBlock(excavatePos, true, serverPlayer);
                        BlocksBrokenManager.addBlockBroken(serverPlayer);
                        APRandomizer.getLayerManager().addLayerCheck(excavatePos.getY());

                        // Damage tool for excavation
                        if (tool.isDamageableItem()) {
                            tool.hurtAndBreak(1, serverPlayer, (p) -> p.broadcastBreakEvent(serverPlayer.getUsedItemHand()));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    static void onBlockEvent(BlockEvent event) {
        APRandomizer.getLayerManager().addLayerCheck(event.getPos().getY());
    }
}
