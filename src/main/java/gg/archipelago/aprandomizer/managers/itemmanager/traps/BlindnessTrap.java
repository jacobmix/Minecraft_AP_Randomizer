package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BlindnessTrap implements Trap {

    private static final int DURATION_TICKS = 20 * 10; // 10 seconds

    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.server.execute(() -> {
            int blindDuration = LevitateTrap.getStackedDuration(player, MobEffects.BLINDNESS, DURATION_TICKS);
            int darkDuration = LevitateTrap.getStackedDuration(player, MobEffects.DARKNESS, DURATION_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindDuration));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, darkDuration));
        });
    }
}
