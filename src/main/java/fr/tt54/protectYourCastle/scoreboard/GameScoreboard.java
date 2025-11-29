package fr.tt54.protectYourCastle.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
        Team team = Team.getPlayerTeam(player.getUniqueId());
        int minutes = (Game.GAME_DURATION - game.time) / 60;
        int seconds = (Game.GAME_DURATION - game.time) % 60;

        int extraMinutes = (game.time - Game.GAME_DURATION) / 60;
        int extraSeconds = (game.time - Game.GAME_DURATION) % 60;

        String teamName = team != null ? team.getColor().getChatColor() + team.getColor().name() : "§cAucune";

        String teamBanner = team != null ? team.getBannerLocation().getBlockX() + " / " + team.getBannerLocation().getBlockZ() : "§cAucune";
        Location target = team == null ? null : team.getBannerLocation().clone().add(.5, 0, .5);
        Vector dist = target == null || target.getWorld() != player.getWorld() ? new Vector(0, 0, 0) : target.toVector().subtract(player.getLocation().toVector()).setY(0);
        double distance = dist.length();
        dist.normalize();
        Vector playerEyes = player.getLocation().getDirection().clone().setY(0);
        float angle = target == null ? 0 : dist.angle(playerEyes);
        boolean left = target == null || dist.getCrossProduct(playerEyes).getY() < 0;
        if(left) angle = (float) (2 * Math.PI - angle);
        int arrowIndex = ((int) ((angle + Math.PI / 8) * 1000000)) / ((int) (Math.PI / 4 * 1000000)) % arrows.length;

        int i = 0;



        fastBoard.updateTitle("§6§lCastle Defender");

        if(Game.GAME_DURATION - game.time > 0) {
            fastBoard.updateLine(i++, "§fTemps restant : " + "§7" + format.format(minutes) + ":" + format.format(seconds));
        } else{
            fastBoard.updateLine(i++, "§cTemps additionnel : " + "§7" + format.format(extraMinutes) + ":" + format.format(extraSeconds));
        }

        fastBoard.updateLine(i++, "§0");
        fastBoard.updateLine(i++, "§eEquipe " + teamName);
        fastBoard.updateLine(i++, "§eBannière : §f" + teamBanner);
        fastBoard.updateLine(i++, "§e" + arrows[arrowIndex] + " " + format.format(distance) + " blocs");
        fastBoard.updateLine(i++, "§1");

        for(Team t : Team.getTeams()){
            String tName = t.getColor().getChatColor() + t.getColor().name();
            fastBoard.updateLine(i++, tName + "§e : " + game.getPoints(t.getColor()) + " points");
        }
    }
}
