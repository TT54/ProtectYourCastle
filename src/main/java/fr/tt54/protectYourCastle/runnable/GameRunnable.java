package fr.tt54.protectYourCastle.runnable;

import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GameRunnable extends BukkitRunnable {

    private final Game game;

    public GameRunnable(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        game.time++;
        if(game.scoreboard != null) {
            game.scoreboard.updatePlayersScoreboard();
        }

        for(ResourceGenerator generator : game.getGenerators()){
            generator.generate();
        }

        if(game.time >= Game.GAME_DURATION){
            if(game.hasWinner()) {
                game.finish();
            } else if(game.time == Game.GAME_DURATION){
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, .5f, .5f);
                }
                Bukkit.broadcastMessage("§6[Castle] §4MORT SUBITE !");
                Bukkit.broadcastMessage("§6[Castle] §eLa première équipe à gagner un point remporte la partie");
            }
        }
    }

}
