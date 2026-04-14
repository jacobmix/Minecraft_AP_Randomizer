package gg.archipelago.aprandomizer.events;

import gg.archipelago.aprandomizer.APClient;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.SlotData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.advancementmanager.CustomAdvancementHandler;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import io.github.archipelagomw.ClientStatus;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ConnectionResultEvent;
import io.github.archipelagomw.network.ConnectionResult;
import io.github.archipelagomw.network.client.CreateAsHint;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.border.WorldBorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectResult {

    private static final Logger LOGGER = LogManager.getLogger();
    APClient client;

    public ConnectResult(APClient apClient) {
        client = apClient;
    }

    @ArchipelagoEventListener
    public void onConnectResult(ConnectionResultEvent event) {
        if (event.getResult() == ConnectionResult.Success) {
            Utils.sendMessageToAll("Connected to Archipelago Server.");

            // Reset reconnect attempts on successful connection
            APRandomizer.resetReconnectAttempts();

            client.slotData = event.getSlotData(SlotData.class);

            APRandomizer.getLayerManager().setCheckedLayers(client.getLocationManager().getCheckedLocations());

            //give our item manager the list of received items to give to players as they log in.
            APRandomizer.getItemManager().setReceivedItems(client.getItemManager().getReceivedItemIDs());

            // Scout Item Shop locations to get item classifications
            scoutItemShopLocations();

            // Auto-start the game when connected
            APRandomizer.server.execute(() -> {
                if (APRandomizer.isJailPlayers()) {
                    // First time starting - initialize everything
                    autoStartGame();
                } else {
                    // Reconnecting after crash/restart - just set playing state
                    APRandomizer.getAP().setGameState(ClientStatus.CLIENT_PLAYING);

                    // Restore world border
                    int numChunks;
                    if (APRandomizer.isProgressiveChunks()) {
                        numChunks = APRandomizer.getUnlockedChunks();
                    } else {
                        numChunks = APRandomizer.getMaxChunks();
                    }
                    int side = (int) Math.ceil(Math.sqrt(numChunks));
                    double centerCoord = side * 8.0 - 1.0;
                    double borderSize = side * 16.0 + 8.0;

                    WorldBorder border = APRandomizer.getServer().overworld().getWorldBorder();
                    border.setCenter(centerCoord, centerCoord);
                    border.setSize(borderSize);
                    border.setWarningBlocks(0);
                    border.setWarningTime(0);
                    border.setDamageSafeZone(0);
                    border.setDamagePerBlock(Double.MAX_VALUE);

                    Utils.sendMessageToAll("Reconnected! Game resuming...");
                }
                APRandomizer.getGoalManager().updateInfoBar();
            });

        } else {
            // Connection failed - handle retry if this was an auto-reconnect attempt
            String pendingAddress = APRandomizer.getPendingReconnectAddress();

            if (event.getResult() == ConnectionResult.InvalidPassword) {
                Utils.sendMessageToAll("Invalid Password.");
            } else if (event.getResult() == ConnectionResult.IncompatibleVersion) {
                Utils.sendMessageToAll("Server Sent Incompatible Version Error.");
            } else if (event.getResult() == ConnectionResult.InvalidSlot) {
                Utils.sendMessageToAll("Invalid Slot Name. (this is case sensitive)");
            } else if (event.getResult() == ConnectionResult.SlotAlreadyTaken) {
                Utils.sendMessageToAll("Room Slot has all ready been taken.");
            }

            // Schedule retry if we have a pending reconnect address
            if (pendingAddress != null && !pendingAddress.isEmpty()) {
                LOGGER.info("Connection failed, scheduling retry...");
                APRandomizer.scheduleReconnectRetry(pendingAddress);
            }
        }
    }

    /**
     * Scout Item Shop locations for unlocked tiers only.
     * Uses BROADCAST_NEW (only option available), but since we only scout
     * locations the player has access to, hints are expected.
     */
    private void scoutItemShopLocations() {
        int shopTier = APRandomizer.worldData != null ? APRandomizer.worldData.getShopTierUnlocked() : 0;
        if (shopTier <= 0) return;

        int count = Math.min(shopTier * 9, 27);
        java.util.ArrayList<Long> locationIDs = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            locationIDs.add(54800L + i);
        }
        client.scoutLocations(locationIDs, CreateAsHint.BROADCAST_NEW);
    }

    /**
     * Auto-start the game (same logic as /start command)
     */
    private void autoStartGame() {
        Utils.sendTitleToAll(Component.literal("Dig"), Component.literal("The Chunk"), 20 * 2, 20 * 5, 20 * 3);

        APRandomizer.getAP().setGameState(ClientStatus.CLIENT_PLAYING);
        APRandomizer.setJailPlayers(false);

        // Determine border size based on progressive chunks mode
        int numChunks;
        if (APRandomizer.isProgressiveChunks()) {
            // Use current unlocked chunks in progressive mode
            numChunks = APRandomizer.getUnlockedChunks();
        } else {
            // Full size in normal mode
            numChunks = APRandomizer.getMaxChunks();
        }

        int side = (int) Math.ceil(Math.sqrt(numChunks));
        double centerCoord = side * 8.0 - 1.0;
        double borderSize = side * 16.0 + 8.0;

        WorldBorder border = APRandomizer.getServer().overworld().getWorldBorder();
        border.setCenter(centerCoord, centerCoord);
        border.setSize(borderSize);
        border.setWarningBlocks(0);
        border.setWarningTime(0);
        border.setDamageSafeZone(0);
        border.setDamagePerBlock(Double.MAX_VALUE);

        for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
            CustomAdvancementHandler.grantAdvancement(player, new ResourceLocation(APRandomizer.MODID, "archipelago/root"));
        }

        LOGGER.info("Game auto-started on successful connection");
    }
}
