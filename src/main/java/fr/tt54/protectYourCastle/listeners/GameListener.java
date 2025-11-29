package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GameListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        if(Game.currentGame == null || !Game.currentGame.isRunning()) return;

        Player player = event.getPlayer();
        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team != null){
            for(Team t : Team.getTeams()){
                if(t != team && t.getBase().contains(event.getBlock().getLocation())){
                    event.setCancelled(true);
                    if(event.getBlock().getLocation().distanceSquared(t.getBannerLocation()) < .1){
                        Bukkit.broadcastMessage(t.getColor().getChatColor() + player.getName() + "§a a volé une bannière à la team " + t.getColor().getChatColor() + t.getColor().name());
                        player.getWorld().dropItem(player.getLocation().clone().add(0, .5, 0), t.getBannerItem());
                    } else {
                        player.sendMessage("§cVous ne pouvez pas casser de blocs à la main dans la base ennemie");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        if(Game.currentGame == null || !Game.currentGame.isRunning()) return;

        if(event.getClickedBlock() != null){
            Player player = event.getPlayer();
            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team != null && event.getClickedBlock().getLocation().distanceSquared(team.getBannerLocation()) < .1){
                event.setCancelled(true);
                if(Team.isBannerItem(player.getInventory().getItemInMainHand())){
                    Game.currentGame.addPoint(team.getColor(), player, player.getInventory().getItemInMainHand().getAmount());
                    player.getInventory().getItemInMainHand().setAmount(0);
                } else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
                    player.sendMessage("§cVous ne pouvez pas casser votre bannière !");
                }
            }
        }
    }

}
