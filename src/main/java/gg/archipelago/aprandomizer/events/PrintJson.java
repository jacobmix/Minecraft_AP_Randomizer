package gg.archipelago.aprandomizer.events;

import gg.archipelago.aprandomizer.APClient;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import io.github.archipelagomw.Print.APPrint;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.PrintJSONEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrintJson {

    private static final Logger LOGGER = LogManager.getLogger();
    APClient client;

    public PrintJson(APClient apClient) {
        client = apClient;
    }

    @ArchipelagoEventListener
    public void onPrintJson(PrintJSONEvent event) {
        APPrint apPrint = event.apPrint;
        if(apPrint.parts.length > 0) {
            if (!apPrint.parts[0].text.startsWith(client.getAlias() + ":")) {
                Utils.sendFancyMessageToAll(apPrint);
            }
        }
    }
}
