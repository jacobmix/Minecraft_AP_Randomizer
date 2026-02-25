package gg.archipelago.aprandomizer.common.Utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gg.archipelago.aprandomizer.APRandomizer;
import io.github.archipelagomw.Print.APPrint;
import io.github.archipelagomw.Print.APPrintColor;
import io.github.archipelagomw.Print.APPrintPart;
import io.github.archipelagomw.Print.APPrintType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.Collections;

public class Utils {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    private static final MinecraftServer server = APRandomizer.getServer();

    /**
     * @param source command source to send the message.
     * @param Message Message to send
     * send a message to whoever ran the command.
     */

    public static void SendMessage(CommandSourceStack source, String Message) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            player.sendSystemMessage(Component.literal(Message));
        } catch (CommandSyntaxException e) {
            source.getServer().sendSystemMessage(Component.literal(Message));
        }
    }

    public static void sendMessageToAll(String message) {
        sendMessageToAll(Component.literal(message));
    }

    public static void sendMessageToAll(Component message) {
        //tell the server to send the message in a thread safe way.
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(message, false));
    }

    public static void sendMessageToPlayer(ServerPlayer player, String message) {
        sendMessageToPlayer(player, Component.literal(message));
    }

    public static void sendMessageToPlayer(ServerPlayer player, Component message) {
        server.execute(() -> player.sendSystemMessage(message));
    }

    public static void sendFancyMessageToAll(APPrint apPrint) {
        Component message = Utils.apPrintToTextComponent(apPrint);

        //tell the server to send the message in a thread safe way.
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(message, false));

    }

    public static Component apPrintToTextComponent(APPrint apPrint) {
        MutableComponent message = Component.literal("");
        for (int i = 0; apPrint.parts.length > i; ++i) {
            APPrintPart part = apPrint.parts[i];
            LOGGER.trace("part[" + i + "]: " + part.text + ", " + part.color + ", " + part.type);
            Style style = Style.EMPTY;
            //no default color was sent so use our own coloring.
            Color color = Color.WHITE;
            boolean bold = false;
            boolean underline = false;

            if (part.color == APPrintColor.none) {
                if (APRandomizer.getAP().getMyName().equals(part.text)) {
                    color = APPrintColor.gold.color;
                    bold = true;
                } else if (part.type == APPrintType.playerID) {
                    color = APPrintColor.yellow.color;
                } else if (part.type == APPrintType.locationID) {
                    color = APPrintColor.green.color;
                } else if (part.type == APPrintType.itemID) {
                    color = APPrintColor.cyan.color;
                }

            } else if (part.color == APPrintColor.underline)
                underline = true;
            else if (part.color == APPrintColor.bold)
                bold = true;
            else
                color = part.color.color;

            //blank out the first two bits because minecraft doesn't deal with alpha values
            int iColor = color.getRGB() & ~(0xFF << 24);
            style = Style.EMPTY.withColor(iColor).withBold(bold).withUnderlined(underline);

            message.append(Component.literal(part.text).withStyle(style));
        }
        return message;
    }

    public static void sendTitleToAll(Component title, Component subTitle, int fadeIn, int stay, int fadeOut) {
        server.execute(() -> TitleQueue.queueTitle(new QueuedTitle(server.getPlayerList().getPlayers(), fadeIn, stay, fadeOut, subTitle, title)));
    }

    public static void sendTitleToAll(Component title, Component subTitle, Component chatMessage, int fadeIn, int stay, int fadeOut) {
        server.execute(() -> TitleQueue.queueTitle(new QueuedTitle(server.getPlayerList().getPlayers(), fadeIn, stay, fadeOut, subTitle, title, chatMessage)));
    }

    public static void sendActionBarToAll(String actionBarMessage) {
        server.execute(() -> TitleUtils.showActionBar(server.getPlayerList().getPlayers(), Component.literal(actionBarMessage)));
    }

    public static void sendActionBarToPlayer(ServerPlayer player, String actionBarMessage) {
        server.execute(() -> {
            //TitleUtils.setTimes(Collections.singletonList(player), fadeIn, stay, fadeOut);
            TitleUtils.showActionBar(Collections.singletonList(player), Component.literal(actionBarMessage));
        });
    }

    public static void PlaySoundToAll(SoundEvent sound) {
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.playNotifySound(sound, SoundSource.MASTER, 1, 1);
            }
        });
    }

    public static Vec3 getRandomPosition(Vec3 pos, int radius) {
        double a = Math.random()*Math.PI*2;
        double b = Math.random()*Math.PI/2;
        double x = radius * Math.cos(a) * Math.sin(b) + pos.x;
        double z = radius * Math.sin(a) * Math.sin(b) + pos.z;
        double y = radius * Math.cos(b) + pos.y;
        return new Vec3(x,y,z);
    }

    public static void giveItemToPlayer(ServerPlayer player, ItemStack itemstack) {
        boolean flag = player.getInventory().add(itemstack);
        if (flag && itemstack.isEmpty()) {
            itemstack.setCount(1);
            ItemEntity itementity1 = player.drop(itemstack, false);
            if (itementity1 != null) {
                itementity1.makeFakeItem();
            }

            player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.inventoryMenu.broadcastChanges();
        } else {
            ItemEntity itementity = player.drop(itemstack, false);
            if (itementity != null) {
                itementity.setNoPickUpDelay();
                itementity.setTarget(player.getUUID());
            }
        }
    }
}
