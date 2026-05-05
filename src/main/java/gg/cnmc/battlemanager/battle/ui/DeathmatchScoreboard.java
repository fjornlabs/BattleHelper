package gg.cnmc.battlemanager.battle.ui;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class DeathmatchScoreboard {

    public static ScoreboardObjective DEATHS;

    public static void setup(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        ScoreboardObjective existing = scoreboard.getNullableObjective("bm_deaths");

        if (existing == null) {
            DEATHS = scoreboard.addObjective(
                    "bm_deaths",
                    ScoreboardCriterion.DUMMY,
                    Text.literal("§cDeaths"),
                    ScoreboardCriterion.RenderType.INTEGER
            );
        } else {
            DEATHS = existing;
        }

        // Use '1' for the sidebar display slot in 1.20.1
        scoreboard.setObjectiveSlot(
                1,
                DEATHS
        );

        resetAll(server);
    }

    public static void resetAll(MinecraftServer server) {
        if (DEATHS == null) return;

        Scoreboard scoreboard = server.getScoreboard();

        // Initialize scores to 0 (Required so that the scoreboard appears on the screen)
        scoreboard.getPlayerScore("attackers", DEATHS).setScore(0);
        scoreboard.getPlayerScore("defenders", DEATHS).setScore(0);
    }

    public static void addDeath(MinecraftServer server, String teamName) {
        if (DEATHS == null) return;

        Scoreboard scoreboard = server.getScoreboard();

        ScoreboardPlayerScore score =
                scoreboard.getPlayerScore(teamName, DEATHS);

        score.setScore(score.getScore() + 1);
    }

    public static int getDeaths(MinecraftServer server, String teamName) {
        if (DEATHS == null) return 0;

        Scoreboard scoreboard = server.getScoreboard();

        return scoreboard.getPlayerScore(teamName, DEATHS).getScore();
    }

    public static void cleanup(MinecraftServer server) {
        if (DEATHS == null) return;

        Scoreboard scoreboard = server.getScoreboard();

        scoreboard.removeObjective(DEATHS);
        DEATHS = null;
    }
}