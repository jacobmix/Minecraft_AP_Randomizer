package gg.archipelago.aprandomizer;

import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.events.AttemptedConnection;
import gg.archipelago.aprandomizer.events.ConnectResult;
import gg.archipelago.aprandomizer.events.PrintJson;
import gg.archipelago.aprandomizer.events.LocationInfo;
import gg.archipelago.aprandomizer.events.ReceiveItem;
import io.github.archipelagomw.Client;
import io.github.archipelagomw.flags.ItemsHandling;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class APClient extends Client {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public SlotData slotData;

    private final MinecraftServer server;

    APClient(MinecraftServer server) {
        super();

        this.setGame("Minecraft Dig");
        this.setItemsHandlingFlags(ItemsHandling.SEND_ITEMS + ItemsHandling.SEND_OWN_ITEMS + ItemsHandling.SEND_STARTING_INVENTORY);
        this.server = server;

        //give our item manager the list of received items to give to players as they log in.
        APRandomizer.getItemManager().setReceivedItems(getItemManager().getReceivedItemIDs());

        this.getEventManager().registerListener(new ConnectResult(this));
        this.getEventManager().registerListener(new AttemptedConnection());
        this.getEventManager().registerListener(new ReceiveItem());
        this.getEventManager().registerListener(new PrintJson(this));
        this.getEventManager().registerListener(new LocationInfo());
    }

    public SlotData getSlotData() {
        return slotData;
    }


//    @Override
//    public void onPrint(String print) {
//        if (!print.startsWith(getAlias() + ":")) {
//            Utils.sendMessageToAll(print);
//        }
//    }
//


    @Override
    public void onError(Exception ex) {
        String error = String.format("Connection error: %s", ex.getLocalizedMessage());
        Utils.sendMessageToAll(error);
    }

    @Override
    public void onClose(String reason, int attemptingReconnect) {
        if (attemptingReconnect > 0) {
            Utils.sendMessageToAll(String.format("%s \n... reconnecting in %ds", reason, attemptingReconnect));
        } else {
            Utils.sendMessageToAll(reason);
        }
        APRandomizer.getGoalManager().updateInfoBar();
    }
}
