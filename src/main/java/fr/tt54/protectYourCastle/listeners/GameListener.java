package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class GameListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Player player = event.getPlayer();
        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team != null){
            for(Team t : Team.getTeams()){
                if(t != team && t.getBase().contains(event.getBlock().getLocation())){
                    player.sendMessage("§cVous ne pouvez pas casser de blocs à la main dans la base ennemie");
                    event.setCancelled(true);
                }
            }
        }
    }

}
