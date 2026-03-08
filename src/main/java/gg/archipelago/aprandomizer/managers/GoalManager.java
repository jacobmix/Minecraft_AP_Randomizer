package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.managers.advancementmanager.LayerManager;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APStorage.APMCData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import io.github.archipelagomw.ClientStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GoalManager {

    private final LayerManager layerManager;

    private CustomBossEvent layerDugBar;
    private CustomBossEvent connectionInfoBar;

    private final APMCData apmc;

    public GoalManager () {
        apmc = APRandomizer.getApmcData();
        layerManager = APRandomizer.getLayerManager();
        initializeInfoBar();
    }

    private int getTotalChecks() {
        return Math.max(apmc.chunk_count, 1) * 192;
    }

    public void initializeInfoBar() {
        CustomBossEvents bossInfoManager = APRandomizer.getServer().getCustomBossEvents();
        layerDugBar = bossInfoManager.create(new ResourceLocation(APRandomizer.MODID,"layer-dug-bar"), Component.literal(""));
        layerDugBar.setMax(getTotalChecks());
        layerDugBar.setColor(BossEvent.BossBarColor.BLUE);
        layerDugBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);

        connectionInfoBar = bossInfoManager.create(new ResourceLocation(APRandomizer.MODID,"connection-info-bar"), Component.literal("Not connected to Archipelago").withStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))));
        connectionInfoBar.setMax(1);
        connectionInfoBar.setValue(1);
        connectionInfoBar.setColor(BossEvent.BossBarColor.RED);
        connectionInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);

        updateInfoBar();
        //layerDugBar.setVisible(true);
        connectionInfoBar.setVisible(true);
    }

    public void updateGoal(boolean canFinish) {
        updateInfoBar();
        if(canFinish)
            checkGoalCompletion();
    }


    public void updateInfoBar() {
        if(layerDugBar == null)
            return;
        APRandomizer.getServer().execute(() -> {
            layerDugBar.setPlayers(APRandomizer.getServer().getPlayerList().getPlayers());
            connectionInfoBar.setPlayers(APRandomizer.getServer().getPlayerList().getPlayers());
        });

        layerDugBar.setValue(layerManager.getFinishedAmount());

        connectionInfoBar.setVisible(!APRandomizer.isConnected());
        layerDugBar.setVisible(!APRandomizer.isJailPlayers());

        layerDugBar.setName(Component.literal("Layers Dug (" + layerManager.getFinishedAmount() + "/" + getTotalChecks() + ")"));

    }

    public void checkGoalCompletion() {
        if(!APRandomizer.isConnected())
            return;

        if(layerManager.getFinishedAmount() >= getTotalChecks()) {
            APRandomizer.getAP().setGameState(ClientStatus.CLIENT_GOAL);

            // Mark goal as completed to stop receiving items
            if (APRandomizer.worldData != null && !APRandomizer.worldData.isGoalCompleted()) {
                APRandomizer.worldData.setGoalCompleted(true);
                showVictoryCelebration();
            }
        }
    }

    private void showVictoryCelebration() {
        APRandomizer.getServer().execute(() -> {
            for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                // Big title on screen
                player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
                player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("§6§lVICTORY!")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("§eAll layers have been dug!")));

                // Play victory sounds
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            }

            Utils.sendMessageToAll("§6§l=============================");
            Utils.sendMessageToAll("§e§l  GOAL COMPLETED!");
            Utils.sendMessageToAll("§a  All layers have been dug!");
            Utils.sendMessageToAll("§6§l=============================");
        });
    }
}
