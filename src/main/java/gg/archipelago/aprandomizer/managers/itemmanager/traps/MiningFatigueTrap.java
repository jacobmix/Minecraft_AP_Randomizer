package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import static net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN;

public class MiningFatigueTrap implements Trap {

    private static final int DURATION_TICKS = 20 * 10; // 10 seconds

    @Override
    public void trigger(ServerPlayer player) {
        APRandomizer.server.execute(() -> {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1F));
            int duration = LevitateTrap.getStackedDuration(player, DIG_SLOWDOWN, DURATION_TICKS);
            player.addEffect(new MobEffectInstance(DIG_SLOWDOWN, duration));
        });
    }
}
