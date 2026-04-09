package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.ShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class SandRain implements Trap {
    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.getServer().execute(() -> {
            ServerLevel world = APRandomizer.getServer().overworld();
            Vec3 pos = player.position();
            // Change block given based on tool level of player.
            int currentToolLevel = APRandomizer.worldData.getPurchasedTier(ShopManager.CATEGORY_TOOLS);
            String blockName;
            if (currentToolLevel == 0) { // Wooden Tools
                blockName = "minecraft:suspicious_sand";
            } else if (currentToolLevel == 1 || currentToolLevel == 2) { // Stone or Iron
                blockName = "minecraft:sand";
            } else if (currentToolLevel == 3) { // Diamond
                blockName = "minecraft:packed_mud";
            } else { // Netherite
                blockName = "minecraft:anvil";
            }
            CompoundTag blockStateTag = new CompoundTag();
            CompoundTag nameTag = new CompoundTag();
            nameTag.putString("Name", blockName);
            blockStateTag.put("BlockState", nameTag);
            // Change radius based on efficiency level of player.
            int currentEfficiencyLevel = APRandomizer.worldData.getPurchasedTier(ShopManager.CATEGORY_EFFICIENCY);
            int radius = 5 * (currentEfficiencyLevel + 1);
            for (int x = (int)pos.x - radius; x <= (int)pos.x + radius; x++) {
                for (int z = (int)pos.z - radius; z <= (int)pos.z + radius; z++) {
                    BlockPos blockPos = new BlockPos(x, (int)pos.y + 15, z);
                    if(world.isEmptyBlock(blockPos)) {
                        Entity e = EntityType.FALLING_BLOCK.create(world);
                        e.deserializeNBT(blockStateTag);
                        e.setPos(blockPos.getCenter());
                        world.addFreshEntity(e);
                    }
                }
            }
        });
    }
}
