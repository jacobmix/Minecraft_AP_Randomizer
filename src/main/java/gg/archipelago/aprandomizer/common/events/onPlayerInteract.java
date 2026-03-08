package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.gui.ShopMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class onPlayerInteract {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    static void onPlayerBlockInteract(PlayerInteractEvent event) {
        if(event.getSide().isClient())
            return;
        //stop all right click interactions if game has not started.
        if(APRandomizer.isJailPlayers()) {
            event.setCanceled(true);
            return;
        }

        int side = APRandomizer.getChunkSide();
        LevelChunk chunk = event.getLevel().getChunkAt(event.getPos());
        // Only block interactions outside the positive chunk area (allow spawn platform in negative coords)
        if(chunk.getPos().x >= side || chunk.getPos().z >= side)
            event.setCanceled(true);

    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()) return;
        if (APRandomizer.isJailPlayers()) return;

        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        CompoundTag tag = heldItem.getTag();
        if (tag != null && tag.getBoolean("isShopItem")) {
            // Open the shop GUI
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                ShopMenu.openShop(serverPlayer);
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        int side = APRandomizer.getChunkSide();
        int cx = (int) Math.floor(event.getPos().getX() / 16.0);
        int cz = (int) Math.floor(event.getPos().getZ() / 16.0);
        // Only block breaking outside the positive chunk area (allow spawn platform in negative coords)
        if(cx >= side || cz >= side)
            event.setCanceled(true);
    }

    @SubscribeEvent
    static void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        int side = APRandomizer.getChunkSide();
        int cx = (int) Math.floor(event.getPos().getX() / 16.0);
        int cz = (int) Math.floor(event.getPos().getZ() / 16.0);
        // Only block placing outside the positive chunk area (allow spawn platform in negative coords)
        if(cx >= side || cz >= side)
            event.setCanceled(true);
    }

}
