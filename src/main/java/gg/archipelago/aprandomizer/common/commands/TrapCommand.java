package gg.archipelago.aprandomizer.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.itemmanager.traps.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod.EventBusSubscriber
public class TrapCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    // Registry of all traps - add new traps here
    private static final Map<String, Supplier<Trap>> TRAP_REGISTRY = new HashMap<>();

    static {
        // Register all traps
        TRAP_REGISTRY.put("meteor", MeteorShowerTrap::new);
        TRAP_REGISTRY.put("bee", BeeTrap::new);
        TRAP_REGISTRY.put("creeper", CreeperTrap::new);
        TRAP_REGISTRY.put("sand", SandRain::new);
        TRAP_REGISTRY.put("wither", FakeWither::new);
        TRAP_REGISTRY.put("goon", GoonTrap::new);
        TRAP_REGISTRY.put("fish", FishFountainTrap::new);
        TRAP_REGISTRY.put("blindness", BlindnessTrap::new);
        TRAP_REGISTRY.put("phantom", PhantomTrap::new);
        TRAP_REGISTRY.put("levitate", LevitateTrap::new);
        TRAP_REGISTRY.put("aboutface", AboutFaceTrap::new);
        TRAP_REGISTRY.put("anvil", AnvilTrap::new);
        TRAP_REGISTRY.put("water", WaterTrap::new);
        TRAP_REGISTRY.put("ghast", GhastTrap::new);
        TRAP_REGISTRY.put("fatigue", MiningFatigueTrap::new);
        TRAP_REGISTRY.put("gravity", IncreasedGravityTrap::new);
    }

    private static final SuggestionProvider<CommandSourceStack> TRAP_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(TRAP_REGISTRY.keySet(), builder);
    };

    public static void Register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("trap")
                .requires(source -> source.hasPermission(2)) // Op only
                .then(Commands.argument("trapname", StringArgumentType.word())
                    .suggests(TRAP_SUGGESTIONS)
                    .executes(TrapCommand::triggerTrap))
                .then(Commands.literal("list")
                    .executes(TrapCommand::listTraps))
        );
    }

    private static int triggerTrap(CommandContext<CommandSourceStack> context) {
        String trapName = StringArgumentType.getString(context, "trapname").toLowerCase();

        if (!TRAP_REGISTRY.containsKey(trapName)) {
            Utils.SendMessage(context.getSource(), "§cUnknown trap: " + trapName);
            Utils.SendMessage(context.getSource(), "§7Use /trap list to see available traps");
            return 0;
        }

        // Get the player who executed the command
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception e) {
            Utils.SendMessage(context.getSource(), "§cThis command must be run by a player");
            return 0;
        }

        // Create and trigger the trap
        Trap trap = TRAP_REGISTRY.get(trapName).get();
        trap.trigger(player);

        Utils.SendMessage(context.getSource(), "§aTriggered trap: §e" + trapName);
        LOGGER.info("Triggered trap {} on player {}", trapName, player.getName().getString());

        return 1;
    }

    private static int listTraps(CommandContext<CommandSourceStack> context) {
        Utils.SendMessage(context.getSource(), "§eAvailable traps:");
        StringBuilder sb = new StringBuilder("§7");
        int count = 0;
        for (String trapName : TRAP_REGISTRY.keySet().stream().sorted().toList()) {
            if (count > 0) sb.append(", ");
            sb.append(trapName);
            count++;
        }
        Utils.SendMessage(context.getSource(), sb.toString());
        return 1;
    }

    @SubscribeEvent
    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        TrapCommand.Register(event.getDispatcher());
    }
}
