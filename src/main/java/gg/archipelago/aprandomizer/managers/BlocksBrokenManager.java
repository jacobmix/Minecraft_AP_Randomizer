package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class BlocksBrokenManager {

    private static final String OBJECTIVE_NAME = "blocks_broken";

    // Flag to temporarily disable counting (used by True Golden Pick)
    private static boolean countingEnabled = true;

    public static void setCountingEnabled(boolean enabled) {
        countingEnabled = enabled;
    }

    public static boolean isCountingEnabled() {
        return countingEnabled;
    }

    public static void addBlockBroken(ServerPlayer player, int count) {
        if (!countingEnabled || player == null) return;

        Scoreboard scoreboard = APRandomizer.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective != null) {
            Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
            score.add(count);
        }
    }

    public static void addBlockBroken(ServerPlayer player) {
        addBlockBroken(player, 1);
    }
    
    public static void addBlockBrokenAt(ServerPlayer player, BlockPos pos) {
        if (player.getLevel().getBlockState(pos).is(Blocks.SCAFFOLDING)) return;
        addBlockBroken(player, 1);
    }

    public static int getBlocksBroken(ServerPlayer player) {
        if (player == null) return 0;

        Scoreboard scoreboard = APRandomizer.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective != null) {
            Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
            return score.getScore();
        }
        return 0;
    }

    public static int getTotalBlocksBroken() {
        Scoreboard scoreboard = APRandomizer.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) return 0;

        int total = 0;
        for (Score score : scoreboard.getPlayerScores(objective)) {
            total += score.getScore();
        }
        return total;
    }
}
