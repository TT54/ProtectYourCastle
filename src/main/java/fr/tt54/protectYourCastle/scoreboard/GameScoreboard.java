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
        int minutes = (Game.GAME_DURATION - game.time) / 60;
        int seconds = (Game.GAME_DURATION - game.time) % 60;

        int extraMinutes = (game.time - Game.GAME_DURATION) / 60;
        int extraSeconds = (game.time - Game.GAME_DURATION) % 60;

        fastBoard.updateTitle("§6§lCastle Defender");

        if(Game.GAME_DURATION - game.time > 0) {
            fastBoard.updateLine(0, "§fTemps restant : " + "§7" + minutes + ":" + seconds);
        } else{
            fastBoard.updateLine(0, "§cTemps additionnel : " + "§7" + extraMinutes + ":" + extraSeconds);
        }
    }
}
