package fr.tt54.protectYourCastle.runnable;

import fr.tt54.protectYourCastle.game.GameParameters;
import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
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

        if(game.time >= GameParameters.GAME_DURATION.get()){
            if(game.hasWinner()) {
                game.finish();
                this.cancel();
            } else if(game.time == GameParameters.GAME_DURATION.get()){
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, .5f, .5f);
                }
                Bukkit.broadcastMessage("§6[Castle] §4MORT SUBITE !");
                Bukkit.broadcastMessage("§6[Castle] §eLa première équipe à gagner un point remporte la partie");
            }
        }

        for(Team.TeamColor teamColor : Team.TeamColor.values()){
            Team team = Team.getTeam(teamColor);
            if(!(team.getBannerLocation().getBlock().getState() instanceof Banner)){
                team.getBannerLocation().getBlock().setType(Material.valueOf(teamColor.getBanner().name()));
            }
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team != null){
                for(Team t : Team.getTeams()){
                    if(t != team && t.getProtectedSpawn() != null && t.getRollbackLocation() != null && t.getProtectedSpawn().contains(player.getLocation())){
                        player.teleport(t.getRollbackLocation());
                    }
                }
            }
        }
    }

}
