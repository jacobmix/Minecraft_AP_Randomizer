package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APStorage.APMCData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import io.github.archipelagomw.ClientStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

@Mod.EventBusSubscriber
public class onJoin {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Set<String> BETA_TESTERS = Set.of(
        "BillyTheAlien_", "MK_404", "__Fe_", "Amasingearth",
        "jacobmix", "MarcyTheRed", "JonikHD", "Rigos07", "GambitTapper"
    );
    private static final String BETA_TEAM_NAME = "Betatester";
    private static boolean betaTeamInitialized = false;

    @SubscribeEvent
    static void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {

        ServerPlayer player = (ServerPlayer) event.getEntity();
        if(APRandomizer.isRace()) {
            player.setGameMode(GameType.SURVIVAL);
            APRandomizer.getServer().getPlayerList().deop(event.getEntity().getGameProfile());
        }


        APMCData data = APRandomizer.getApmcData();
        if (data.state == APMCData.State.MISSING)
            Utils.sendMessageToAll("No APMC file found, please only start the server via the APMC file.");
        else if (data.state == APMCData.State.INVALID_VERSION)
            Utils.sendMessageToAll("This Seed was generated using an incompatible randomizer version.");
        else if (data.state == APMCData.State.INVALID_SEED)
            Utils.sendMessageToAll("Invalid Minecraft World please only start the Minecraft server via the correct APMC file");

        if(data.state != APMCData.State.VALID)
            return;

        if(APRandomizer.getAP().isConnected() && APRandomizer.isJailPlayers()) {
            APRandomizer.getAP().setGameState(ClientStatus.CLIENT_READY);
        }

        ServerScoreboard scoreboard = APRandomizer.getServer().getScoreboard();
        Objective stats = scoreboard.getObjective("deaths");
        if(stats != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), stats);
        }


        APRandomizer.getGoalManager().updateInfoBar();

        // Register player so they receive queued items when offline
        APRandomizer.getItemManager().registerPlayer(player);

        if (APRandomizer.isJailPlayers()) {
            // Game not started yet - teleport to jail
            BlockPos jail = APRandomizer.getJailPosition();
            player.teleportTo(jail.getX(), jail.getY(), jail.getZ());
        } else {
            // Game already started - teleport to starting platform (world spawn)
            BlockPos spawn = player.getLevel().getSharedSpawnPos();
            player.teleportTo(spawn.getX(), spawn.getY() + 1, spawn.getZ());

            // Give any pending items that were received while player was offline
            APRandomizer.getItemManager().givePendingItems(player);
        }
        player.setGameMode(GameType.SURVIVAL);

        // Beta tester team handling
        handleBetaTester(player);
    }

    private static void handleBetaTester(ServerPlayer player) {
        ServerScoreboard scoreboard = APRandomizer.getServer().getScoreboard();

        // Create team once
        if (!betaTeamInitialized) {
            PlayerTeam team = scoreboard.getPlayerTeam(BETA_TEAM_NAME);
            if (team == null) {
                team = scoreboard.addPlayerTeam(BETA_TEAM_NAME);
            }
            team.setColor(ChatFormatting.BLUE);
            betaTeamInitialized = true;
        }

        String playerName = player.getGameProfile().getName();
        if (BETA_TESTERS.contains(playerName)) {
            PlayerTeam team = scoreboard.getPlayerTeam(BETA_TEAM_NAME);
            if (team != null && !team.getPlayers().contains(playerName)) {
                scoreboard.addPlayerToTeam(playerName, team);
                Utils.sendMessageToPlayer(player, "§9§lThank you for participating in the beta test! Stay tuned for the next updates!");
            }
        }
    }
}
