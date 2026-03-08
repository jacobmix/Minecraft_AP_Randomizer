package gg.archipelago.aprandomizer.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.FossilManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class FossilCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void Register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fossil")
                .then(Commands.literal("status")
                    .executes(FossilCommand::status))
                .then(Commands.literal("regenerate")
                    .requires(source -> source.hasPermission(2))
                    .executes(FossilCommand::regenerate))
                .then(Commands.literal("xray")
                    .requires(source -> source.hasPermission(2))
                    .executes(FossilCommand::xray))
                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(FossilCommand::add)))
        );
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        String status = FossilManager.getFossilStatus();
        Utils.SendMessage(context.getSource(), "§eFossil Status: §7" + status);
        LOGGER.info("Fossil status: {}", status);
        return 1;
    }

    private static int regenerate(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getServer().overworld();
        int chunkCount = APRandomizer.getMaxChunks();

        Utils.SendMessage(context.getSource(), "§eForce regenerating fossils...");
        LOGGER.info("Force regenerating fossils for {} chunks", chunkCount);

        FossilManager.forceRegenerateFossils(level, chunkCount);

        String status = FossilManager.getFossilStatus();
        Utils.SendMessage(context.getSource(), "§aFossil regeneration complete. §7" + status);
        return 1;
    }

    private static int xray(CommandContext<CommandSourceStack> context) {
        Utils.SendMessage(context.getSource(), "§eActivating Fossil X-ray for all players...");
        FossilManager.activateFossilXrayForAll();
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) {
        String amountStr = StringArgumentType.getString(context, "amount");
        try {
            int amount = Integer.parseInt(amountStr);
            if (APRandomizer.worldData != null) {
                APRandomizer.worldData.addFossils(amount);
                int newBalance = APRandomizer.worldData.getFossilBalance();
                Utils.sendMessageToAll("§aAdded " + amount + " fossils. New balance: " + newBalance);
                return 1;
            }
        } catch (NumberFormatException e) {
            Utils.SendMessage(context.getSource(), "§cInvalid amount: " + amountStr);
        }
        return 0;
    }

    @SubscribeEvent
    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        FossilCommand.Register(event.getDispatcher());
    }
}
