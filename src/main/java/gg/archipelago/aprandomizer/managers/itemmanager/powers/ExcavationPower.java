package gg.archipelago.aprandomizer.managers.itemmanager.powers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class ExcavationPower implements Power {

    private static final HashMap<Player, Direction> faces = new HashMap<>();

    public static int level = 0;

    @SubscribeEvent
    public static void detectBlockFace(PlayerInteractEvent event) {
        faces.put(event.getEntity(), event.getFace());
    }

    /**
     * Get the excavation target positions for a block being mined.
     * Returns empty set if excavation doesn't apply.
     */
    public static Set<BlockPos> getExcavationTargets(BlockPos pos, Player player, Level world) {
        HashSet<BlockPos> positions = new HashSet<>();

        if (level < 1 || player.isCrouching()) {
            return positions;
        }

        BlockState baseBlock = world.getBlockState(pos);
        if (baseBlock.isAir()) {
            return positions;
        }

        Direction face = faces.get(player);
        if (face == null) {
            // Fallback: use player's looking direction
            face = Direction.getNearest(
                (float) player.getLookAngle().x,
                (float) player.getLookAngle().y,
                (float) player.getLookAngle().z
            ).getOpposite();
        }

        // Build list of potential positions based on excavation level and face
        HashSet<BlockPos> potentialPositions = new HashSet<>();
        switch (face) {
            case DOWN, UP -> {
                // Normalize rotation to 0-360
                float rot = player.getYHeadRot() % 360;
                if (rot < 0) rot += 360;
                // 0=south, 90=west, 180=north, 270=east
                // Determine primary axis (line direction) and secondary direction (expansion toward player facing)
                boolean primaryNS = (rot >= 45 && rot < 135) || (rot >= 225 && rot < 315);

                BlockPos secondaryDir;
                BlockPos oppositeDir;
                if (primaryNS) {
                    boolean facingEast = rot >= 225 && rot < 315;
                    secondaryDir = facingEast ? pos.east() : pos.west();
                    oppositeDir = facingEast ? pos.west() : pos.east();
                } else {
                    boolean facingNorth = rot >= 135 && rot < 225;
                    secondaryDir = facingNorth ? pos.north() : pos.south();
                    oppositeDir = facingNorth ? pos.south() : pos.north();
                }

                if (level >= 1) {
                    if (primaryNS) {
                        potentialPositions.add(pos.north());
                        potentialPositions.add(pos.south());
                    } else {
                        potentialPositions.add(pos.east());
                        potentialPositions.add(pos.west());
                    }
                }
                if (level >= 2) {
                    potentialPositions.add(secondaryDir);
                    if (primaryNS) {
                        potentialPositions.add(secondaryDir.north());
                        potentialPositions.add(secondaryDir.south());
                    } else {
                        potentialPositions.add(secondaryDir.east());
                        potentialPositions.add(secondaryDir.west());
                    }
                }
                if (level >= 3) {
                    potentialPositions.add(oppositeDir);
                    if (primaryNS) {
                        potentialPositions.add(oppositeDir.north());
                        potentialPositions.add(oppositeDir.south());
                    } else {
                        potentialPositions.add(oppositeDir.east());
                        potentialPositions.add(oppositeDir.west());
                    }
                }
            }
            case EAST, WEST -> {
                if (level >= 1) {
                    // 2 blocks to the side
                    potentialPositions.add(pos.north());
                    potentialPositions.add(pos.south());
                }
                if (level >= 2) {
                    // Row of 3 blocks above (center + sides)
                    potentialPositions.add(pos.above());
                    potentialPositions.add(pos.above().north());
                    potentialPositions.add(pos.above().south());
                }
                if (level >= 3) {
                    // Complete 3x3 area
                    potentialPositions.add(pos.below());
                    potentialPositions.add(pos.below().north());
                    potentialPositions.add(pos.below().south());
                }
            }
            case NORTH, SOUTH -> {
                if (level >= 1) {
                    // 2 blocks to the side
                    potentialPositions.add(pos.east());
                    potentialPositions.add(pos.west());
                }
                if (level >= 2) {
                    // Row of 3 blocks above (center + sides)
                    potentialPositions.add(pos.above());
                    potentialPositions.add(pos.above().east());
                    potentialPositions.add(pos.above().west());
                }
                if (level >= 3) {
                    // Complete 3x3 area
                    potentialPositions.add(pos.below());
                    potentialPositions.add(pos.below().east());
                    potentialPositions.add(pos.below().west());
                }
            }
        }

        // Filter to only valid excavation targets
        // Allow excavation if the player can mine the block (has dig speed > 1)
        // This makes gold pickaxe and other tools work properly
        float baseDestroySpeed = baseBlock.getDestroySpeed(world, pos);
        for (BlockPos excavatePos : potentialPositions) {
            BlockState targetState = world.getBlockState(excavatePos);
            // Skip bedrock and other unbreakable blocks (destroySpeed < 0)
            if (targetState.is(Blocks.BEDROCK) || targetState.getDestroySpeed(world, excavatePos) < 0) {
                continue;
            }
            if (!targetState.isAir() &&
                player.getDigSpeed(targetState, excavatePos) > 1.0f &&
                baseDestroySpeed >= targetState.getDestroySpeed(world, excavatePos)) {
                positions.add(excavatePos.immutable());
            }
        }

        return positions;
    }

    @Override
    public void grantPower() {
        level++;
    }

    /**
     * Set the excavation level directly (used by shop system)
     */
    public static void setLevel(int newLevel) {
        level = newLevel;
    }

    /**
     * Get the current excavation level
     */
    public static int getLevel() {
        return level;
    }
}
