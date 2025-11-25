package fr.tt54.protectYourCastle.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private static Map<UUID, ImpyriaScoreboard> displayedScoreboard = new HashMap<>();

    public static void showScoreboard(Player player, ImpyriaScoreboard scoreboard) {
        scoreboard.openPlayerScoreboard(player);
        displayedScoreboard.put(player.getUniqueId(), scoreboard);
    }

    public static void removeScoreboard(Player player) {
        ImpyriaScoreboard scoreboard = displayedScoreboard.get(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.removePlayer(player);
            displayedScoreboard.remove(player.getUniqueId());
        }
    }

    public static @Nullable ImpyriaScoreboard getDisplayedScoreboard(Player player) {
        return displayedScoreboard.get(player.getUniqueId());
    }

}
