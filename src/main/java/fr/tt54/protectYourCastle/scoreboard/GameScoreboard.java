package fr.tt54.protectYourCastle.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
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

    }
}
