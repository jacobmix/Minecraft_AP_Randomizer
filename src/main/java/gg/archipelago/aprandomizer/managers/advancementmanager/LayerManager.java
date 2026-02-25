package gg.archipelago.aprandomizer.managers.advancementmanager;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class LayerManager {

    private static final Set<Integer> checkLayers = new HashSet<>();

    // Stores cleared (chunkIndex * 192 + layerIndex) keys
    private static final Set<Long> clearedLayers = new HashSet<>();

    public static final long START_INDEX = 50000;

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        int side = APRandomizer.getChunkSide();

        for (int y : checkLayers) {
            Level overWorld = APRandomizer.server.getLevel(Level.OVERWORLD);
            assert overWorld != null;

            // Check each chunk separately
            for (int cx = 0; cx < side; cx++) {
                for (int cz = 0; cz < side; cz++) {
                    int chunkIndex = cx * side + cz;
                    long key = (long) chunkIndex * 192 + (y + 63);

                    if (clearedLayers.contains(key))
                        continue;

                    boolean allAir = true;
                    int ox = cx * 16;
                    int oz = cz * 16;

                    checkBlock:
                    for (int x = ox; x < ox + 16; x++) {
                        for (int z = oz; z < oz + 16; z++) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            BlockState block = overWorld.getBlockState(blockPos);
                            if (!block.isAir()) {
                                allAir = false;
                                break checkBlock;
                            }
                        }
                    }

                    if (allAir) {
                        clearedLayers.add(key);
                        if (side == 1) {
                            Utils.sendTitleToAll(Component.literal("Layer " + y + " clear!"), Component.empty(), 0, 20, 0);
                        } else {
                            Utils.sendTitleToAll(Component.literal("Chunk " + chunkIndex + " Layer " + y + " clear!"), Component.empty(), 0, 20, 0);
                        }
                        APRandomizer.getGoalManager().updateGoal(true);
                        APRandomizer.getAP().checkLocation(START_INDEX + key);
                    }
                }
            }
        }
        checkLayers.clear();
    }

    public void addLayerCheck(int y) {
        if (y <= 128) {
            checkLayers.add(y);
        }
    }

    public void setCheckedLayers(Set<Long> locations) {
        clearedLayers.clear();
        for (Long location : locations) {
            // location is the AP location ID; key = locationID - START_INDEX
            clearedLayers.add(location - START_INDEX);
        }
    }

    public int getFinishedAmount() {
        return clearedLayers.size();
    }
}
