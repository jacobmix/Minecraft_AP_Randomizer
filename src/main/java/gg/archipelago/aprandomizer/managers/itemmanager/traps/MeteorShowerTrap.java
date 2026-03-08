package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber
public class MeteorShowerTrap implements Trap {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Set<Block> PROTECTED_BLOCKS = Set.of(
        Blocks.BEDROCK,
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.NETHERITE_BLOCK
    );

    // Lista globale dei meteori attivi - aggiornata ogni server tick
    private static final List<MeteorTracker> activeTrackers = new ArrayList<>();

    private final Random random = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (activeTrackers.isEmpty()) return;

        Iterator<MeteorTracker> it = activeTrackers.iterator();
        while (it.hasNext()) {
            MeteorTracker tracker = it.next();
            boolean done = tracker.tick();
            if (done) it.remove();
        }
    }

    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.getServer().execute(() -> {
            ServerLevel level = APRandomizer.getServer().overworld();

            // Raccogli i chunk unici che contengono almeno un player
            Set<Long> seen = new java.util.HashSet<>();
            List<BlockPos> chunkCenters = new java.util.ArrayList<>();

            for (ServerPlayer p : APRandomizer.getServer().getPlayerList().getPlayers()) {
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(
                    p.blockPosition().getX() >> 4,
                    p.blockPosition().getZ() >> 4
                );
                if (seen.add(chunkKey)) {
                    // Centro del chunk: X = chunkX*16+8, Z = chunkZ*16+8
                    int cx = (p.blockPosition().getX() >> 4) * 16 + 8;
                    int cz = (p.blockPosition().getZ() >> 4) * 16 + 8;
                    chunkCenters.add(new BlockPos(cx, 0, cz));
                }
            }

            if (chunkCenters.isEmpty()) return;

            // Suono di avviso per tutti i player
            for (ServerPlayer p : APRandomizer.getServer().getPlayerList().getPlayers()) {
                level.playSound(null, p.blockPosition(), SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 1.5f, 0.5f);
            }

            // Per ogni chunk con player, spawna meteore con delay tra ognuna
            int delay = 0;
            for (BlockPos center : chunkCenters) {
                int meteorsPerChunk = 5;
                for (int i = 0; i < meteorsPerChunk; i++) {
                    final BlockPos spawnCenter = center;
                    APRandomizer.getServer().tell(new TickTask(
                        APRandomizer.getServer().getTickCount() + delay,
                        () -> spawnMeteorAt(level, spawnCenter)
                    ));
                    delay += 20; // ~1 second between each meteor
                }
            }
        });
    }

    private void spawnMeteorAt(ServerLevel level, BlockPos chunkCenter) {
        // Piccolo offset randomico attorno al centro del chunk (max 6 blocchi)
        int offsetX = random.nextInt(13) - 6;
        int offsetZ = random.nextInt(13) - 6;

        BlockPos spawnPos = new BlockPos(chunkCenter.getX() + offsetX, 200, chunkCenter.getZ() + offsetZ);

        level.setBlock(spawnPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        FallingBlockEntity meteor = FallingBlockEntity.fall(level, spawnPos, Blocks.MAGMA_BLOCK.defaultBlockState());

        if (meteor == null) {
            level.setBlock(spawnPos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        meteor.time = 1;
        meteor.setHurtsEntities(2.0f, 40);
        meteor.dropItem = false;

        double velocityX = (random.nextDouble() - 0.5) * 0.3;
        double velocityZ = (random.nextDouble() - 0.5) * 0.3;
        meteor.setDeltaMovement(velocityX, -0.5, velocityZ);

        level.playSound(null, spawnPos, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0f, 0.8f);

        activeTrackers.add(new MeteorTracker(level, meteor, new Random()));
    }

    private static class MeteorTracker {
        private final ServerLevel level;
        private final FallingBlockEntity meteor;
        private final Random random;
        private int ticks = 0;

        MeteorTracker(ServerLevel level, FallingBlockEntity meteor, Random random) {
            this.level = level;
            this.meteor = meteor;
            this.random = random;
        }

        // Ritorna true quando il tracker è finito (da rimuovere dalla lista)
        boolean tick() {
            ticks++;

            // Meteora atterrata: FallingBlockEntity si rimuove quando piazza il blocco
            if (meteor.isRemoved()) {
                explodeAt(meteor.blockPosition());
                return true;
            }

            // Timeout
            if (ticks > 200) {
                explodeAt(meteor.blockPosition());
                meteor.discard();
                return true;
            }

            // Particelle mentre cade
            Vec3 pos = meteor.position();
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 5, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.3, pos.z, 3, 0.15, 0.15, 0.15, 0.01);
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.0);

            if (ticks % 10 == 0) {
                level.playSound(null, BlockPos.containing(pos), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 0.8f, 1.2f);
            }

            return false;
        }

        private void explodeAt(BlockPos lastPos) {
            // Cerca il blocco di magma piazzato dall'atterraggio
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos checkPos = lastPos.offset(0, dy, 0);
                if (level.getBlockState(checkPos).is(Blocks.MAGMA_BLOCK)) {
                    Vec3 explodePos = new Vec3(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);
                    level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                    createExplosion(level, checkPos, explodePos, random);
                    return;
                }
            }
            createExplosion(level, lastPos, new Vec3(lastPos.getX() + 0.5, lastPos.getY() + 0.5, lastPos.getZ() + 0.5), random);
        }
    }

    private static void createExplosion(ServerLevel level, BlockPos impactPos, Vec3 exactPos, Random random) {
        level.playSound(null, impactPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5f, 0.9f);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, exactPos.x, exactPos.y, exactPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, exactPos.x, exactPos.y, exactPos.z, 30, 1.5, 1.0, 1.5, 0.1);
        level.sendParticles(ParticleTypes.LAVA, exactPos.x, exactPos.y, exactPos.z, 15, 1.0, 0.5, 1.0, 0.0);

        level.explode(null, exactPos.x, exactPos.y, exactPos.z, 3.5f, false, Level.ExplosionInteraction.BLOCK);

        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    BlockPos checkPos = impactPos.offset(x, 0, z);
                    for (int y = 2; y >= -2; y--) {
                        BlockPos pos = checkPos.offset(0, y, 0);
                        if (!level.getBlockState(pos).isAir() &&
                            !PROTECTED_BLOCKS.contains(level.getBlockState(pos).getBlock())) {
                            if (random.nextFloat() < 0.4f) {
                                level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
