package fr.tt54.protectYourCastle.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.GameParameters;
import fr.tt54.protectYourCastle.game.Team;
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
        Game game = Game.getCurrentGame();
        if(game == null){
            return;
        }
        Team team = Team.getPlayerTeam(player.getUniqueId());
        int minutes = (GameParameters.GAME_DURATION.get() - game.getTime()) / 60;
        int seconds = (GameParameters.GAME_DURATION.get() - game.getTime()) % 60;

        int extraMinutes = (game.getTime() - GameParameters.GAME_DURATION.get()) / 60;
        int extraSeconds = (game.getTime() - GameParameters.GAME_DURATION.get()) % 60;

        String teamName = team != null ? team.getColor().getChatColor() + team.getColor().name() : "§cAucune";

        Location teamBannerLocation = team != null ? team.getBannerLocation() : null;
        String teamBanner = teamBannerLocation != null ? teamBannerLocation.getBlockX() + " / " + teamBannerLocation.getBlockZ() : "§cAucune";
        Location target = teamBannerLocation == null ? null : teamBannerLocation.clone().add(.5, 0, .5);
        Vector dist = target == null || target.getWorld() != player.getWorld() ? new Vector(0, 0, 0) : target.toVector().subtract(player.getLocation().toVector()).setY(0);
        double distance = dist.length();
        float angle = 0f;
        if(target != null && distance > 0){
            dist.normalize();
            Vector playerEyes = player.getLocation().getDirection().clone().setY(0);
            angle = dist.angle(playerEyes);
            boolean left = dist.getCrossProduct(playerEyes).getY() < 0;
            if(left) angle = (float) (2 * Math.PI - angle);
        }
        int arrowIndex = ((int) ((angle + Math.PI / 8) * 1000000)) / ((int) (Math.PI / 4 * 1000000)) % arrows.length;

        int i = 0;



        fastBoard.updateTitle("§6§lCastle Defender");

        if(GameParameters.GAME_DURATION.get() - game.getTime() > 0) {
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
