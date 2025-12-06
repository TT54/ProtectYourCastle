package fr.tt54.protectYourCastle.cmd;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.game.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CmdDrawbridge implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("§cVous devez être un joueur pour exécuter cette commande");
            return false;
        }

        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team == null){
            sender.sendMessage("§cVous devez avoir une team pour exécuter cette commande");
            return false;
        }

        if(team.getDrawbridgeLocation() == null){
            sender.sendMessage("§cVotre team n'a pas de pont-levis");
            return false;
        }

        team.getDrawbridgeLocation().getBlock().setType(Material.REDSTONE_BLOCK);
        Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), () -> {
            team.getDrawbridgeLocation().getBlock().setType(Material.AIR);
        }, 10L);
        player.sendMessage("§aPont-levis ouvert !");

        return true;
    }
}
