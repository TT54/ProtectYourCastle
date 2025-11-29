package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class GameListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        final Player player = event.getPlayer();
        final Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team != null) {
            player.setPlayerListName(team.getColor().getChatColor() + "[" + team.getColor().name() + "] " + player.getName());
        }
    }

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
                        if(Game.currentGame.bannerHolder.containsKey(team.getColor())){
                            player.sendMessage("§cVotre équipe a déjà une bannière dans un inventaire");
                            return;
                        }
                        Bukkit.broadcastMessage("§6[Castle] " + t.getColor().getChatColor() + player.getName() + "§a a volé une bannière à la team " + t.getColor().getChatColor() + t.getColor().name());
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
        Player player = event.getPlayer();

        if(event.getClickedBlock() != null){
            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team != null && event.getClickedBlock().getLocation().distanceSquared(team.getBannerLocation()) < .1){
                event.setCancelled(true);
                ItemStack is = event.getItem();
                if(Team.isBannerItem(is)){
                    Game.currentGame.placeBanner(team, player, is);
                    is.setAmount(0);
                    return;
                } else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
                    player.sendMessage("§cVous ne pouvez pas casser votre bannière !");
                    return;
                }
            }
        }

        if(Team.isBannerItem(event.getItem())){
            event.setCancelled(true);
            player.sendMessage("§cVous ne pouvez pas interagir avec la bannière en main");
        }
    }

    private static final Set<Material> ALLOWED_DROPS = Set.of(Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD);

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        final Player player = event.getEntity();
        final Team team = Team.getPlayerTeam(event.getEntity().getUniqueId());

        event.getDrops().removeIf(loot -> !ALLOWED_DROPS.contains(loot.getType()));
        player.spigot().respawn();

        if(team != null && Game.currentGame != null && Game.currentGame.bannerHolder.get(team.getColor()).equals(player.getUniqueId())){
            Game.currentGame.bannerHolder.remove(team.getColor());
            Bukkit.broadcastMessage("§6[Castle] " + team.getColor().getChatColor() + player.getName() + "§c a perdu la bannière ennemi qu'il transportait");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        final Team team = Team.getPlayerTeam(event.getPlayer().getUniqueId());
        final Game game = Game.currentGame;
        if(team != null && game != null){
            final Player player = event.getPlayer();
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), () -> {
                game.spawnPlayer(player, team);
            }, 20L * 20);
        }
    }

}
