package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class LevitateTrap implements Trap {

    private static final int DURATION_TICKS = 20 * 10; // 10 seconds

    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.server.execute(() -> {
            int duration = getStackedDuration(player, MobEffects.LEVITATION, DURATION_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration));
        });
    }

    static int getStackedDuration(ServerPlayer player, MobEffect effect, int addTicks) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null) {
            return existing.getDuration() + addTicks;
        }
        return addTicks;
    }
}
