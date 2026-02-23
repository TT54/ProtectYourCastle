package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.pycmod.events.PlayerDamagedByPlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ModEventListener {

    public static void onPlayerDamagedByPlayer(PlayerDamagedByPlayerEvent event){
        Player attacked = Bukkit.getPlayer(event.attackedPlayerUUID());
        Player damager = Bukkit.getPlayer(event.damagerPlayerUUID());

        if(Game.getCurrentGame() != null && Game.getCurrentGame().isRunning() && attacked != null && damager != null){
            Game.getCurrentGame().increaseDamageDealt(damager.getUniqueId(), event.damageAmount());
        }
    }

}
