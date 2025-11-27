package fr.tt54.protectYourCastle.runnable;

import fr.tt54.protectYourCastle.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Game;
import org.bukkit.scheduler.BukkitRunnable;

public class GameRunnable extends BukkitRunnable {

    private final Game game;

    public GameRunnable(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        for(ResourceGenerator generator : game.getGenerators()){
            generator.generate();
        }
    }

}
