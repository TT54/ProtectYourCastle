package fr.tt54.protectYourCastle.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class ImpyriaScoreboard {

    private final Map<Player, FastBoard> players = new HashMap<>();

    public ImpyriaScoreboard() {
    }

    protected abstract void generateBoard(FastBoard fastBoard, Player player);

    protected abstract void refreshBoard(FastBoard fastBoard, Player player);

    protected void openPlayerScoreboard(Player player) {
        if (!players.containsKey(player)) {
            FastBoard board = new FastBoard(player);
            generateBoard(board, player);
            players.put(player, board);
        }
    }

    public void updatePlayersScoreboard() {
        for (Map.Entry<Player, FastBoard> entry : players.entrySet()) {
            refreshBoard(entry.getValue(), entry.getKey());
        }
    }

    public void destroy() {
        for (Player p : new ArrayList<>(players.keySet())) {
            ScoreboardManager.removeScoreboard(p);
        }
    }

    protected void removePlayer(Player player) {
        FastBoard board = players.get(player);
        if (board != null) {
            board.delete();
        }
        players.remove(player);
    }
}
