package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

public class BannerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void itemPickup(EntityPickupItemEvent event){
        if(Game.currentGame != null && Game.currentGame.isRunning() && event.getEntity() instanceof Player player){
            final Game game = Game.currentGame;
            final Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team != null && Team.isBannerItem(event.getItem().getItemStack())){
                if(game.pickupBanner(player)){
                    Bukkit.broadcastMessage("§6[Castle] " + team.getColor().getChatColor() + player.getName() + "§a vient de récupérer une bannière !");
                } else{
                    event.setCancelled(true);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cVotre équipe a déjà un morceau de totem dans un inventaire"));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerDropItem(PlayerDropItemEvent event){
        if(Team.isBannerItem(event.getItemDrop().getItemStack())){
            event.getPlayer().sendMessage("§cVous ne pouvez pas drop de morceau de totem");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void invMoveItem(InventoryClickEvent event){
        InventoryType type = event.getInventory().getType();
        if(type != InventoryType.PLAYER && type != InventoryType.CRAFTING && event.getCurrentItem() != null){
            if(Team.isBannerItem(event.getCurrentItem())){
                event.setCancelled(true);
            } else if(event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null &&
                    Team.isBannerItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))){
                event.setCancelled(true);
            }
        }
    }


}
