package fr.tt54.protectYourCastle.cmd;

import fr.tt54.protectYourCastle.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Game;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CmdCastle extends CoreCommand {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("§cVous devez être un joueur pour exécuter cette commande");
            return false;
        }

        if(!player.hasPermission("castle.manage")){
            player.sendMessage("§cVous n'avez pas la permission d'exécuter cette commande");
            return false;
        }

        if(args.length >= 1) {
            if (args[0].equalsIgnoreCase("generator")) {
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("add")){
                        if(args.length != 4){
                            player.sendMessage("§cLe bon usage est '/castle generator add <material> <delay>'");
                            return false;
                        }

                        Material material;
                        try {
                            material = Material.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLe material " + args[2].toUpperCase() + " n'existe pas");
                            return false;
                        }

                        int delay;
                        try{
                            delay = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLe bon usage est '/castle generator add <material> <delay>'");
                            return false;
                        }

                        Location location = player.getLocation().getBlock().getLocation().clone().add(.5, .5, .5);

                        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(location.clone().add(0, 1.5, 0), EntityType.TEXT_DISPLAY);
                        textDisplay.setBillboard(Display.Billboard.CENTER);
                        textDisplay.setSeeThrough(true);

                        ResourceGenerator generator = new ResourceGenerator(material, delay, delay, location);
                        ResourceGenerator.addGenerator(generator);
                        player.sendMessage("§aUn générateur a été ajouté sur votre position");
                        return true;
                    }
                }
            } else if(args[0].equalsIgnoreCase("start")){
                if(!Game.createNew()){
                    player.sendMessage("§cUne partie est déjà en cours");
                    return false;
                }
                Game.currentGame.prepare();
                Game.currentGame.launch();
                player.sendMessage("§aLa partie a bien été lancée");
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1){
            return tabComplete(args[0], "generator", "start");
        }
        if(args.length == 2){
            if(args[0].equalsIgnoreCase("generator")){
                return tabComplete(args[1], "add");
            }
        }
        if(args.length == 3){
            if(args[0].equalsIgnoreCase("generator")){
                if(args[1].equalsIgnoreCase("add")){
                    return tabComplete(args[2], Arrays.stream(Material.values()).map(mat -> mat.name().toLowerCase()));
                }
            }
        }
        if(args.length == 4){
            if(args[0].equalsIgnoreCase("generator")){
                if(args[1].equalsIgnoreCase("add")){
                    return tabComplete(args[2], "1", "10", "20", "30", "60");
                }
            }
        }
        return List.of();
    }
}
