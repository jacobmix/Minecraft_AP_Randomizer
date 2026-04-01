package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.advancementmanager.CustomAdvancementHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResultHolder;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class onExplosiveBow {
    private static final Logger LOGGER = LogManager.getLogger();

    // Track explosive arrows by their UUID
    private static final Set<UUID> explosiveArrows = new HashSet<>();
    // Track which player shot each explosive arrow (arrow UUID -> shooter UUID)
    private static final Map<UUID, UUID> arrowShooters = new HashMap<>();
    // Players who nocked an explosive bow (tracked before shot to avoid last-shot break issue)
    private static final Set<UUID> pendingExplosiveShot = new HashSet<>();

    @SubscribeEvent
    public static void onArrowNock(ArrowNockEvent event) {
        // Allow explosive bow to be used without arrows in inventory
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isExplosiveBow(event.getBow())) return;

        // Track that this player is about to fire an explosive arrow
        pendingExplosiveShot.add(player.getUUID());

        // Give a temporary arrow if they have none (Infinity needs at least 1)
        boolean hadArrow = player.getInventory().contains(new ItemStack(Items.ARROW));
        if (!hadArrow) {
            player.getInventory().add(new ItemStack(Items.ARROW, 1));
        }

        // Force the bow to start charging
        player.startUsingItem(event.getHand());
        event.setAction(InteractionResultHolder.consume(event.getBow()));
    }

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (event.getLevel().isClientSide()) return;

        if (arrow.getOwner() instanceof ServerPlayer player) {
            if (pendingExplosiveShot.remove(player.getUUID())) {
                explosiveArrows.add(arrow.getUUID());
                arrowShooters.put(arrow.getUUID(), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (event.getEntity().level.isClientSide()) return;

        UUID arrowId = arrow.getUUID();
        if (explosiveArrows.contains(arrowId)) {
            explosiveArrows.remove(arrowId);
            UUID shooterUUID = arrowShooters.remove(arrowId);

            Level level = arrow.level;
            Vec3 pos = arrow.position();

            // Explode with the shooter as source (so they don't take damage from their own explosion)
            level.explode(
                arrow.getOwner(), // source entity - won't take damage from own explosion
                pos.x, pos.y, pos.z,
                3.5f,
                false,
                Level.ExplosionInteraction.BLOCK
            );

            arrow.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerKilledByExplosiveBow(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (victim.equals(killer)) return;

        // Check if the kill was from an explosion where the killer had an explosive bow
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            // Check if the killer is holding or recently used an explosive bow
            if (isExplosiveBow(killer.getMainHandItem()) || isExplosiveBow(killer.getOffhandItem())) {
                CustomAdvancementHandler.grantAdvancement(killer,
                    new ResourceLocation(APRandomizer.MODID, "archipelago/dirty_killer"));
            }
        }
    }

    private static boolean isExplosiveBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("explosiveBow");
    }
}
