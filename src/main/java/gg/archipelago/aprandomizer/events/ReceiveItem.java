package gg.archipelago.aprandomizer.events;

import gg.archipelago.aprandomizer.APRandomizer;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ReceiveItemEvent;

public class ReceiveItem {

    @ArchipelagoEventListener
    public static void onReceiveItem(ReceiveItemEvent event) {
        APRandomizer.getItemManager().giveItemToAll(event.getItemID(), event.getIndex());
    }
}
