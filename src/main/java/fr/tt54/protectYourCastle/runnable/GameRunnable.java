package fr.tt54.protectYourCastle.runnable;

import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Game;
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

        if(game.time == Game.GAME_DURATION){
            game.finish();
        }
    }

}
