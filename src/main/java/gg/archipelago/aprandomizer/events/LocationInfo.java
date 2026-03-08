package gg.archipelago.aprandomizer.events;

import gg.archipelago.aprandomizer.APRandomizer;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.LocationInfoEvent;
import io.github.archipelagomw.parts.NetworkItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocationInfo {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long ITEM_SHOP_AP_START_ID = 54800;
    private static final int ITEM_SHOP_COUNT = 27;

    @ArchipelagoEventListener
    public void onLocationInfo(LocationInfoEvent event) {
        if (APRandomizer.worldData == null) return;

        for (NetworkItem item : event.locations) {
            long locID = item.locationID;

            // Check if this is an Item Shop location
            if (locID >= ITEM_SHOP_AP_START_ID && locID < ITEM_SHOP_AP_START_ID + ITEM_SHOP_COUNT) {
                int shopIndex = (int) (locID - ITEM_SHOP_AP_START_ID);
                APRandomizer.worldData.setShopItemFlag(shopIndex, item.flags);
                if (item.itemName != null && !item.itemName.isEmpty()) {
                    APRandomizer.worldData.setShopItemName(shopIndex, item.itemName);
                }
                if (item.playerName != null && !item.playerName.isEmpty()) {
                    APRandomizer.worldData.setShopItemPlayer(shopIndex, item.playerName);
                }
                LOGGER.info("Scouted Item Shop {}: '{}' for '{}' flags={} ({})",
                    shopIndex + 1, item.itemName, item.playerName, item.flags, getFlagName(item.flags));
            }
        }
    }

    private String getFlagName(int flags) {
        if ((flags & io.github.archipelagomw.flags.NetworkItem.ADVANCEMENT) != 0) return "progression";
        if ((flags & io.github.archipelagomw.flags.NetworkItem.USEFUL) != 0) return "useful";
        if ((flags & io.github.archipelagomw.flags.NetworkItem.TRAP) != 0) return "trap";
        return "filler";
    }
}
