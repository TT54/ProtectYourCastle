package fr.tt54.protectYourCastle.cmd;

import fr.tt54.protectYourCastle.inventories.lobby.GameStatsListInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CmdStats implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("§cVous devez être un joueur pour exécuter cette commande");
            return false;
        }

        GameStatsListInventory inv = new GameStatsListInventory(player, 1);
        inv.openInventory();
        return true;
    }
}
