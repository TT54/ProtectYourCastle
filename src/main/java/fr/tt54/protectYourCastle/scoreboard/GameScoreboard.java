package fr.tt54.protectYourCastle.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import fr.tt54.protectYourCastle.game.Game;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class GameScoreboard extends ImpyriaScoreboard {

    public static final DecimalFormat format = new DecimalFormat("00");
    public static final DecimalFormat formatDouble = new DecimalFormat("0.0");

    private static final String[] arrows = new String[] {
            "⬆",
            "⬈",
            "➡",
            "⬊",
            "⬇",
            "⬋",
            "⬅",
            "⬉"};


    @Override
    protected void generateBoard(FastBoard fastBoard, Player player) {
        drawScoreboard(fastBoard, player);
    }

    @Override
    protected void refreshBoard(FastBoard fastBoard, Player player) {
        drawScoreboard(fastBoard, player);
    }

    private void drawScoreboard(FastBoard fastBoard, Player player){
        Game game = Game.currentGame;
        int minutes = game.time / 60;
        int seconds = game.time % 60;

        fastBoard.updateLine(0, "§7" + minutes + ":" + seconds);
    }
}
