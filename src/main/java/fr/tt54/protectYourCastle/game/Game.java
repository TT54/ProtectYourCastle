package fr.tt54.protectYourCastle.game;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.ResourceGenerator;
import fr.tt54.protectYourCastle.runnable.GameRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final List<ResourceGenerator> generators = new ArrayList<>();
    private Status gameStatus;
    private transient GameRunnable runnable;

    public Game() {
    }

    public void prepare(){
        this.gameStatus = Status.PREPARING;

        for(Player player : Bukkit.getOnlinePlayers()){
            // TODO Ouvrir le menu de sélection d'équipe
        }
    }

    public void launch(){
        if(gameStatus == Status.PREPARING){
            this.runnable = new GameRunnable(this);
            this.runnable.runTaskTimer(ProtectYourCastleMain.getInstance(), 20, 20);

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(true);
            }

            this.gameStatus = Status.RUNNING;
        }
    }

    public void stop(){
        if(this.runnable != null && gameStatus != Status.STOPPED) {
            this.runnable.cancel();
            this.runnable = null;

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(false);
            }

            this.gameStatus = Status.STOPPED;
        }
    }

    public List<ResourceGenerator> getGenerators() {
        return generators;
    }

    public enum Status{

        PREPARING,
        RUNNING,
        PAUSED,
        STOPPED;

    }
}
