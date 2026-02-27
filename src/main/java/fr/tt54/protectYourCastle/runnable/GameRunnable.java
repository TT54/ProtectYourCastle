package fr.tt54.protectYourCastle.runnable;

import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.GameParameters;
import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GameRunnable extends BukkitRunnable {

    private final Game game;
    private int resourcesToGenerate = 0;

    public GameRunnable(Game game) {
        this.game = game;
        for(Team team : Team.getTeams())
            this.resourcesToGenerate = Math.max(this.resourcesToGenerate, team.getMembers().size());
    }

    @Override
    public void run() {
        if(this.isCancelled()) return;

        game.increaseTime();
        if(game.getScoreboard() != null) {
            game.getScoreboard().updatePlayersScoreboard();
        }

        for(ResourceGenerator generator : game.getGenerators()){
            generator.generate(GameParameters.INCREASED_RESOURCES.get() && generator.getMaterial() != Material.DIAMOND && generator.getMaterial() != Material.EMERALD
                    ? resourcesToGenerate : 1);
        }

        if(game.getTime() >= GameParameters.GAME_DURATION.get()){
            if(game.hasWinner()) {
                game.finish();
                this.cancel();
            } else if(game.getTime() == GameParameters.GAME_DURATION.get()){
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, .5f, .5f);
                }
                Bukkit.broadcastMessage("§6[Castle] §4MORT SUBITE !");
                Bukkit.broadcastMessage("§6[Castle] §eLa première équipe à gagner un point remporte la partie");
            }
        }

        for(Team.TeamColor teamColor : Team.TeamColor.values()){
            Team team = Team.getTeam(teamColor);
            if(team == null) continue;

            Location bannerLocation = team.getBannerLocation();
            if(bannerLocation == null) continue;

            if(!(bannerLocation.getBlock().getState() instanceof Banner)){
                bannerLocation.getBlock().setType(Material.valueOf(teamColor.getBanner().name()));
            }

            Player player = game.getBannerHolder(teamColor);
            if(player != null){
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§eVous portez une bannière " + teamColor.getChatColor() + teamColor.name().toLowerCase() + "§e !"));
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
