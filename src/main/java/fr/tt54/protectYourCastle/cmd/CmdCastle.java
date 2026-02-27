package fr.tt54.protectYourCastle.cmd;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.game.*;
import fr.tt54.protectYourCastle.inventories.ConfirmationInventory;
import fr.tt54.protectYourCastle.inventories.trades.weapons.WeaponsBundleListInventory;
import fr.tt54.protectYourCastle.utils.Area;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Stream;

public class CmdCastle extends CoreCommand {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("§cVous devez être un joueur pour exécuter cette commande");
            return false;
        }

        String rootArg = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if(args.length == 0 || rootArg.equals("help")){
            this.sendHelp(player, args.length >= 2 ? args[1] : null, player.hasPermission("castle.manage"));
            return true;
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
                        ResourceGenerator.spawnResourceGenerator(material, delay, delay, location);
                        player.sendMessage("§aUn générateur a été ajouté sur votre position");
                        return true;
                    } else if(args[1].equalsIgnoreCase("remove")){
                        ResourceGenerator targetGenerator = null;
                        for(ResourceGenerator gen : ResourceGenerator.getResourceGenerators()){
                            if(gen.getLocation().getWorld() == player.getWorld() && player.getLocation().distanceSquared(gen.getLocation()) <= 1){
                                targetGenerator = gen;
                                break;
                            }
                        }

                        if(targetGenerator == null){
                            player.sendMessage("§cIl n'y a aucun générateur situé à moins d'un bloc de vous");
                            return false;
                        }

                        if(ResourceGenerator.removeResourceGenerator(targetGenerator)){
                            player.sendMessage("§aLe générateur de " + targetGenerator.getMaterial().name().toLowerCase() + " a été supprimé");
                        } else{
                            player.sendMessage("§cUne erreur est survenue lors de la suppression d'un générateur de " + targetGenerator.getMaterial().name().toLowerCase());
                        }
                        return true;
                    } else if(args[1].equalsIgnoreCase("edit_all")){
                        if(args.length != 4){
                            player.sendMessage("§cLe bon usage est '/castle generator edit_all <material to edit> <delay>'");
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
                            player.sendMessage("§cLe bon usage est '/castle generator edit_all <material to edit> <delay>'");
                            return false;
                        }

                        List<ResourceGenerator> toEdit = ResourceGenerator.getResourceGenerators().stream().filter(r -> r.getMaterial() == material).toList();
                        if(toEdit.isEmpty()){
                            player.sendMessage("§cIl n'y a aucun générateur de ce type");
                            return false;
                        }

                        for(ResourceGenerator generator : toEdit){
                            generator.setCooldown(delay);
                        }
                        player.sendMessage("§aLes générateurs de " + material.name().toLowerCase() + " ont un délais de " + delay + " secondes");
                        return true;
                    }
                }
            } else if(args[0].equalsIgnoreCase("start")){
                if(args.length != 2){
                    player.sendMessage("§cLe bon usage est '/castle start <map>'");
                    return false;
                }

                if(Game.getLoadedWorld() != null){
                    player.sendMessage("§cLe monde de jeu est déjà chargé, impossible de lancer une nouvelle partie");
                    return false;
                }

                StartValidationReport validationReport = this.validateStartConfiguration(args[1]);
                if(!validationReport.warnings.isEmpty()){
                    player.sendMessage("§6[Castle] §eVérification config: " + validationReport.warnings.size() + " avertissement(s)");
                    for(String warning : validationReport.warnings){
                        player.sendMessage("§e- " + warning);
                    }
                }
                if(!validationReport.errors.isEmpty()){
                    player.sendMessage("§6[Castle] §cImpossible de lancer la partie: configuration invalide");
                    for(String error : validationReport.errors){
                        player.sendMessage("§c- " + error);
                    }
                    return false;
                }

                boolean empty = true;
                int redCount = 0;
                int yellowCount = 0;
                for(Player p : Bukkit.getOnlinePlayers()){
                    Team team = Team.getPlayerTeam(p.getUniqueId());
                    if(team != null) {
                        empty = false;
                        if(team.getColor() == Team.TeamColor.RED){
                            redCount++;
                        } else if(team.getColor() == Team.TeamColor.YELLOW){
                            yellowCount++;
                        }
                    }
                }

                if(empty) {
                    player.sendMessage("§cImpossible de lancer la partie, aucune équipe n'a de joueur");
                    return false;
                }
                if(redCount == 0 || yellowCount == 0){
                    player.sendMessage("§e[Castle] Une équipe est vide (RED=" + redCount + ", YELLOW=" + yellowCount + "). La partie peut démarrer, mais ce n'est pas conseillé.");
                }

                if(!Game.createNew()){
                    player.sendMessage("§cUne partie est déjà en cours");
                    return false;
                }

                Game game = Game.getCurrentGame();
                game.prepare(args[1]);
                if(game.getGameStatus() != Game.Status.PREPARING){
                    player.sendMessage("§cÉchec de préparation de la partie. Vérifiez que la map '" + args[1] + "' existe et est complète.");
                    game.stop();
                    return false;
                }

                game.launch();
                if(!game.isRunning()){
                    player.sendMessage("§cÉchec au lancement de la partie.");
                    game.stop();
                    return false;
                }
                player.sendMessage("§aLa partie a bien été lancée");
                return true;
            } else if(args[0].equalsIgnoreCase("stop")){
                if(Game.getCurrentGame() == null){
                    player.sendMessage("§cIl n'y a aucune partie en cours");
                    return false;
                }
                Game.getCurrentGame().stop();
                player.sendMessage("§aLa partie a bien été arrêtée");
                Bukkit.broadcastMessage("§6[Castle] §cLa partie a été arrêtée");
                return true;
            } else if(args[0].equalsIgnoreCase("team")){
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("spawn")){
                        if(args.length != 6){
                            player.sendMessage("§cLa bon usage est '/castle team spawn <team> <x> <y> <z>'");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        int x, y, z;
                        try {
                            x = Integer.parseInt(args[3]);
                            y = Integer.parseInt(args[4]);
                            z = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLa bon usage est '/castle team spawn <x> <y> <z>'");
                            return false;
                        }

                        Team.getTeam(teamColor).setSpawnLocation(new Location(player.getWorld(), x + .5d, y + 1, z + .5d));
                        player.sendMessage("§aLe nouveau spawn de l'équipe " + teamColor.name() + " est en " + x + " " + y + " " + z);
                        return true;
                    } else if(args[1].equalsIgnoreCase("rollback")){
                        if(args.length != 6){
                            player.sendMessage("§cLa bon usage est '/castle team rollback <team> <x> <y> <z>'");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        int x, y, z;
                        try {
                            x = Integer.parseInt(args[3]);
                            y = Integer.parseInt(args[4]);
                            z = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLa bon usage est '/castle team rollback <x> <y> <z>'");
                            return false;
                        }

                        Team.getTeam(teamColor).setRollbackLocation(new Location(player.getWorld(), x + .5d, y + 1, z + .5d));
                        player.sendMessage("§aLe nouveau point de rollback de l'équipe " + teamColor.name() + " est en " + x + " " + y + " " + z);
                        return true;
                    } else if(args[1].equalsIgnoreCase("drawbridge")){
                        if(args.length != 6){
                            player.sendMessage("§cLa bon usage est '/castle team drawbridge <team> <x> <y> <z>'");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        int x, y, z;
                        try {
                            x = Integer.parseInt(args[3]);
                            y = Integer.parseInt(args[4]);
                            z = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLa bon usage est '/castle team drawbridge <x> <y> <z>'");
                            return false;
                        }

                        Team.getTeam(teamColor).setDrawbridgeLocation(new Location(player.getWorld(), x, y, z));
                        player.sendMessage("§aLe nouveau pont-levis de l'équipe " + teamColor.name() + " est en " + x + " " + y + " " + z);
                        return true;
                    } else if(args[1].equalsIgnoreCase("base")){
                        if(args.length != 9){
                            player.sendMessage("§cLa bon usage est '/castle team spawn <team> <x1> <y1> <z1> <x2> <y2> <z2> '");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        int x1, y1, z1, x2, y2, z2;
                        try {
                            x1 = Integer.parseInt(args[3]);
                            y1 = Integer.parseInt(args[4]);
                            z1 = Integer.parseInt(args[5]);
                            x2 = Integer.parseInt(args[6]);
                            y2 = Integer.parseInt(args[7]);
                            z2 = Integer.parseInt(args[8]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLa bon usage est '/castle team spawn <team> <x1> <y1> <z1> <x2> <y2> <z2> '");
                            return false;
                        }

                        Location loc1 = new Location(player.getWorld(), x1, y1, z1);
                        Location loc2 = new Location(player.getWorld(), x2, y2, z2);
                        Team.getTeam(teamColor).setBase(new Area(loc1, loc2, false));
                        player.sendMessage("§aLa nouvelle base de l'équipe " + teamColor.name() + " a été placée");
                        return true;
                    } else if(args[1].equalsIgnoreCase("protected")){
                        if(args.length != 9){
                            player.sendMessage("§cLa bon usage est '/castle team protected <team> <x1> <y1> <z1> <x2> <y2> <z2> '");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        int x1, y1, z1, x2, y2, z2;
                        try {
                            x1 = Integer.parseInt(args[3]);
                            y1 = Integer.parseInt(args[4]);
                            z1 = Integer.parseInt(args[5]);
                            x2 = Integer.parseInt(args[6]);
                            y2 = Integer.parseInt(args[7]);
                            z2 = Integer.parseInt(args[8]);
                        } catch (NumberFormatException e){
                            player.sendMessage("§cLa bon usage est '/castle team protected <team> <x1> <y1> <z1> <x2> <y2> <z2> '");
                            return false;
                        }

                        Location loc1 = new Location(player.getWorld(), x1, y1, z1);
                        Location loc2 = new Location(player.getWorld(), x2, y2, z2);
                        Team.getTeam(teamColor).setProtectedSpawn(new Area(loc1, loc2, true));
                        player.sendMessage("§aLa nouvelle zone protégée de l'équipe " + teamColor.name() + " a été placée");
                        return true;
                    } else if(args[1].equalsIgnoreCase("banner")){
                        if(args.length != 3){
                            player.sendMessage("§cLa bon usage est '/castle team banner <team>'");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        Block block = player.getTargetBlockExact(5);
                        if(block == null || !(block.getState() instanceof Banner banner)){
                            player.sendMessage("§cVous devez viser une bannière pour exécuter cette commande !");
                            return false;
                        }

                        Team team = Team.getTeam(teamColor);
                        team.setBannerLocation(block.getLocation());
                        player.sendMessage("§aLa bannière de l'équipe " + teamColor.name() + " a été placée en " + block.getLocation().getBlockX() + " " + block.getLocation().getBlockY() + " " + block.getLocation().getBlockZ());
                        return true;
                    } else if(args[1].equalsIgnoreCase("join")){
                        if(args.length != 4){
                            player.sendMessage("§cLa bon usage est '/castle team join <team> <player>'");
                            return false;
                        }

                        Team.TeamColor teamColor;
                        try {
                            teamColor = Team.TeamColor.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            player.sendMessage("§cLa team " + args[2] + " n'existe pas");
                            return false;
                        }

                        Player target = Bukkit.getPlayer(args[3]);
                        if(target == null){
                            player.sendMessage("§cLe joueur " + args[3] + " n'est pas connecté");
                            return false;
                        }

                        Team team = Team.getTeam(teamColor);
                        team.joinTeam(target.getUniqueId());
                        Bukkit.broadcastMessage("§a" + target.getName() + " a rejoint l'équipe " + team.getColor().getChatColor() + team.getColor().name());
                        return true;
                    } else if(args[1].equalsIgnoreCase("leave")){
                        if(args.length != 3){
                            player.sendMessage("§cLa bon usage est '/castle team leave <player>'");
                            return false;
                        }

                        OfflinePlayer target = Bukkit.getPlayer(args[2]);
                        if(target == null){
                            player.sendMessage("§cLe joueur " + args[2] + " n'existe pas");
                            return false;
                        }

                        Team team = Team.getPlayerTeam(target.getUniqueId());
                        if(team == null){
                            player.sendMessage("§cLe joueur " + target.getName() + " n'a pas d'équipe");
                            return false;
                        }

                        team.leaveTeam(target.getUniqueId());
                        Bukkit.broadcastMessage("§a" + target.getName() + " a quitté l'équipe " + team.getColor().getChatColor() + team.getColor().name());
                        return true;
                    } else if(args[1].equalsIgnoreCase("fill")){
                        boolean clearBefore = args.length >= 3 && args[2].equalsIgnoreCase("withClear");
                        boolean random = args.length == 4 && args[3].equalsIgnoreCase("randomly");
                        if(clearBefore){
                            for(Team team : Team.getTeams()){
                                for(UUID uuid : new ArrayList<>(team.getMembers())){
                                    team.leaveTeam(uuid);
                                }
                            }
                        }

                        if(!random) {
                            Team.fillWithScores();
                        } else {
                            Team.fillRandomly();
                        }
                        player.sendMessage("§aLes équipes ont bien été remplies");
                        return true;
                    } else if(args[1].equalsIgnoreCase("clear")){
                        for(Team team : Team.getTeams()){
                            for(UUID uuid : new ArrayList<>(team.getMembers())){
                                team.leaveTeam(uuid);
                            }
                        }
                        player.sendMessage("§aLes équipes ont bien été vidées");
                        return true;
                    }
                }
            } else if(args[0].equalsIgnoreCase("trader")){
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("spawn")){
                        String requestedType = null;
                        StringBuilder nameBuilder = new StringBuilder();
                        for(int i = 2; i < args.length; i++){
                            if(args[i].equalsIgnoreCase("--type")){
                                if(i + 1 >= args.length){
                                    player.sendMessage("§cBon usage : '/castle trader spawn [name] [--type <type>]'");
                                    return false;
                                }
                                requestedType = args[++i];
                            } else {
                                if(nameBuilder.length() > 0){
                                    nameBuilder.append(" ");
                                }
                                nameBuilder.append(args[i]);
                            }
                        }
                        String name = nameBuilder.isEmpty() ? "Marchand" : nameBuilder.toString();

                        Trader sameExisting = null;
                        for(Trader existingTrader : Trader.traders.values()){
                            if(existingTrader != null && existingTrader.getName().equalsIgnoreCase(name)){
                                sameExisting = existingTrader;
                                break;
                            }
                        }

                        Trader trader;
                        if(requestedType != null){
                            trader = new Trader(name, false);
                            if(!trader.bindToType(requestedType)){
                                player.sendMessage("§cType inconnu: " + requestedType + ". Utilisez '/castle trader type list'");
                                return false;
                            }
                        } else if(sameExisting != null && sameExisting.getTraderType() != null){
                            trader = new Trader(name, false);
                            trader.bindToType(sameExisting.getTraderType());
                            player.sendMessage("§aUn marchand du même nom a été trouvé, son type a été recopié");
                        } else if(sameExisting != null){
                            List<Trader.NPCTrade> trades = new ArrayList<>();
                            for(Trader.NPCTrade trade : sameExisting.getTrades()){
                                trades.add(trade.clone());
                            }
                            trader = new Trader(name, trades, sameExisting.isWeaponTrader());
                            player.sendMessage("§aUn marchand du même nom a été trouvé, ses trades ont été copiés");
                        } else {
                            trader = new Trader(name, false);
                        }
                        trader.spawn(player.getLocation());
                        if(trader.getTraderType() != null){
                            player.sendMessage("§aVous avez fait apparaître un marchand de type §e" + trader.getTraderType());
                        } else {
                            player.sendMessage("§aVous avez fait apparaître un marchand");
                        }
                        return true;
                    } else if(args[1].equalsIgnoreCase("remove")){
                        Villager villager = this.getNearestTraderVillager(player, 2);
                        Trader removed = villager != null ? Trader.getTrader(villager.getUniqueId()) : null;

                        if(removed != null){
                            Trader.removeTrader(villager.getUniqueId());
                            villager.remove();
                            player.sendMessage("§aLe marchand " + removed.getName() + " a bien été supprimé");
                            return true;
                        } else {
                            player.sendMessage("§cAucun marchand trouvé autour de vous");
                            return false;
                        }
                    } else if(args[1].equalsIgnoreCase("respawn")){
                        int respawnedCount = 0;
                        for(Trader trader : new ArrayList<>(Trader.traders.values())){
                            if(trader.getSavedLocation() != null){
                                Location loc = trader.getSavedLocation().toLocation();
                                if(loc.getWorld() == player.getWorld() && loc.distance(player.getLocation()) < 64){
                                    trader.respawn();
                                    respawnedCount++;
                                }
                            }
                        }
                        player.sendMessage("§aMarchands respawn autour de vous: §e" + respawnedCount);
                        return true;
                    } else if(args[1].equalsIgnoreCase("type")){
                        if(args.length < 3){
                            player.sendMessage("§cBon usage : '/castle trader type <list|save|bind|unbind|duplicate|delete> ...'");
                            return false;
                        }

                        String action = args[2].toLowerCase(Locale.ROOT);
                        if(action.equals("list")){
                            Set<String> types = Trader.getTraderTypeNames();
                            if(types.isEmpty()){
                                player.sendMessage("§eAucun type de marchand enregistré");
                            } else {
                                player.sendMessage("§7----- §eTypes de marchands §7-----");
                                for(String typeName : types){
                                    int trades = Trader.getTraderTypeTradeCount(typeName);
                                    Boolean weaponTrader = Trader.isTraderTypeWeaponTrader(typeName);
                                    String style = Boolean.TRUE.equals(weaponTrader) ? "vendeur d'armes" : "normal";
                                    player.sendMessage("§f- §e" + typeName + " §7(" + trades + " trades, " + style + ")");
                                }
                            }
                            return true;
                        }

                        if(!Set.of("save", "bind", "unbind", "duplicate", "delete").contains(action)){
                            player.sendMessage("§cAction inconnue. Utilisez '/castle trader type <list|save|bind|unbind|duplicate|delete>'");
                            return false;
                        }

                        Trader targetTrader = this.getNearestTrader(player, 2);
                        if(!action.equals("duplicate") && !action.equals("delete") && targetTrader == null){
                            player.sendMessage("§cAucun marchand trouvé autour de vous");
                            return false;
                        }

                        if(action.equals("save")){
                            if(args.length != 4){
                                player.sendMessage("§cBon usage : '/castle trader type save <type>'");
                                return false;
                            }

                            if(!Trader.saveTypeFromTrader(args[3], targetTrader)){
                                player.sendMessage("§cNom de type invalide. Utilisez uniquement a-z, 0-9, '_' ou '-'");
                                return false;
                            }
                            player.sendMessage("§aType enregistré et lié au marchand: §e" + targetTrader.getTraderType());
                            return true;
                        } else if(action.equals("bind")){
                            if(args.length != 4){
                                player.sendMessage("§cBon usage : '/castle trader type bind <type>'");
                                return false;
                            }

                            if(!targetTrader.bindToType(args[3])){
                                player.sendMessage("§cType inconnu: " + args[3] + ". Utilisez '/castle trader type list'");
                                return false;
                            }
                            player.sendMessage("§aMarchand lié au type: §e" + targetTrader.getTraderType());
                            return true;
                        } else if(action.equals("unbind")){
                            if(args.length != 3){
                                player.sendMessage("§cBon usage : '/castle trader type unbind'");
                                return false;
                            }
                            if(targetTrader.getTraderType() == null){
                                player.sendMessage("§eCe marchand n'a pas de type lié");
                                return true;
                            }
                            targetTrader.unbindType();
                            player.sendMessage("§aLe marchand a été délié de son type");
                            return true;
                        } else if(action.equals("duplicate")){
                            if(args.length != 5){
                                player.sendMessage("§cBon usage : '/castle trader type duplicate <source> <target>'");
                                return false;
                            }

                            if(!Trader.duplicateTraderType(args[3], args[4])){
                                player.sendMessage("§cDuplication impossible. Vérifiez la source, la cible et le format du nom de type.");
                                return false;
                            }
                            player.sendMessage("§aType dupliqué: §e" + args[3] + " §7-> §e" + args[4]);
                            return true;
                        } else if(action.equals("delete")){
                            if(args.length != 4){
                                player.sendMessage("§cBon usage : '/castle trader type delete <type>'");
                                return false;
                            }

                            if(!Trader.deleteTraderType(args[3])){
                                player.sendMessage("§cType introuvable: " + args[3]);
                                return false;
                            }
                            player.sendMessage("§aType supprimé: §e" + args[3]);
                            return true;
                        }
                    }
                }
            } else if(args[0].equalsIgnoreCase("set_duration")){
                if(args.length != 2){
                    player.sendMessage("§cBon usage : '/castle set_duration <duration>'");
                    return false;
                }

                int duration;
                try {
                    duration = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    player.sendMessage("§cBon usage : '/castle set_duration <duration>'");
                    return false;
                }

                GameParameters.gameParameters.setParameter(GameParameters.GAME_DURATION, 60 * duration);
                player.sendMessage("§aDurée de : " + duration + "min");
                return true;
            } else if(args[0].equalsIgnoreCase("parameter")){
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("set")){
                        if(args.length != 4){
                            player.sendMessage("§cBon usage : '/castle parameter set <parameter> <value>'");
                            return false;
                        }

                        String parameter = args[2];
                        String value = args[3];

                        if(GameParameters.Parameter.getParameter(parameter) == null){
                            player.sendMessage("§cLe paramètre " + parameter + " est introuvable");
                            return false;
                        }

                        if(GameParameters.gameParameters.setParameter(parameter, value)){
                            player.sendMessage("§aParamètre mis à jour : §3" + parameter + " §2-> §e" + value);
                            return true;
                        }

                        player.sendMessage("§cLa valeur renseignée est incompatible avec ce paramètre ou ce dernier n'existe pas");
                        return false;
                    } else if(args[1].equalsIgnoreCase("get")){
                        if(args.length != 3){
                            player.sendMessage("§cBon usage : '/castle parameter get <parameter>'");
                            return false;
                        }

                        String paramName = args[2];
                        GameParameters.Parameter<?> parameter = GameParameters.Parameter.getParameter(paramName);
                        if(parameter != null){
                            player.sendMessage("§8-------- §eParamètres §8--------");
                            player.sendMessage("§6" + parameter.getName() + " : §e" + GameParameters.gameParameters.getParameter(parameter).toString());
                            return true;
                        }

                        player.sendMessage("§cLe paramètre " + paramName + " est introuvable");
                        return false;
                    } else if (args[1].equalsIgnoreCase("list")) {
                        GameParameters parameters = GameParameters.gameParameters;
                        player.sendMessage("§8-------- §eParamètres §8--------");
                        for(GameParameters.Parameter<?> parameter : GameParameters.Parameter.existingParameters){
                            player.sendMessage("§6" + parameter.getName() + " : §e" + parameters.getParameter(parameter).toString());
                        }
                        return true;
                    }
                }
            } else if(args[0].equalsIgnoreCase("scores")){
                if(args.length == 2 && args[1].equalsIgnoreCase("refresh")){
                    GameStatistics.recalculateAllPlayersScores();
                    player.sendMessage("§aScores rafraîchis");
                    return true;
                }

                player.sendMessage("§7----- §eScores §7-----");
                DecimalFormat format = new DecimalFormat("#");
                int rank = 1;
                for(UUID uuid : GameStatistics.getRegisteredPlayers()){
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                    player.sendMessage("§b" + rank + "- §6" + p.getName() + " : §f" + format.format(GameStatistics.getPlayerCurrentScore(uuid)) + " points §7(Total : " + format.format(GameStatistics.getPlayerTotalScore(uuid)) + ")");
                    rank++;
                }
                return true;
            } else if(args[0].equalsIgnoreCase("edit")){
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("join")){
                        if(args.length != 3){
                            player.sendMessage("§cBon usage : '/castle edit join <world>'");
                            return false;
                        }

                        String worldName = args[2];
                        World world = Bukkit.getWorld(worldName);

                        if(world == null){
                            world = Game.loadWorld(worldName);
                        }

                        if(world == null){
                            player.sendMessage("§cLe monde d'édition n'existe pas et n'a pas pu être chargé");
                            return false;
                        }

                        player.setGameMode(GameMode.CREATIVE);
                        player.teleport(world.getHighestBlockAt(0, 0).getLocation().clone().add(0, 1, 0));
                        player.sendMessage("§aVous avez été envoyé dans le monde d'édition");
                        return true;
                    } else if(args[1].equalsIgnoreCase("leave")){
                        if(Game.getLoadedWorld() == null){
                            player.sendMessage("§cAucun monde d'édition n'est chargé");
                            return false;
                        }

                        World world = Bukkit.getWorld(Game.getLoadedWorld());
                        if(world == null){
                            player.sendMessage("§cLe monde d'édition n'est pas chargé");
                            return false;
                        }

                        ConfirmationInventory inv = new ConfirmationInventory("Quitter", player, List.of("§aQuitter SANS SAUVEGARDER", "§7Faites /castle edit save pour quitter en sauvegardant"), List.of("§cRetour"), () -> {
                            player.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get(), GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get()));
                            player.sendMessage("§aVous avez quitté le monde d'édition sans le sauvegarder");

                            for(Player p : Bukkit.getOnlinePlayers()){
                                if(p.getWorld() == world) return;
                            }

                            Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), () -> Game.unloadWorld(world, false), 10L);
                        }, () -> {});
                        inv.openInventory();

                        return true;
                    } else if(args[1].equalsIgnoreCase("save")){
                        if(Game.getLoadedWorld() == null){
                            player.sendMessage("§cAucun monde d'édition n'est chargé");
                            return false;
                        }

                        World world = Bukkit.getWorld(Game.getLoadedWorld());

                        if(world == null){
                            player.sendMessage("§cLe monde d'édition n'est pas chargé");
                            return false;
                        }

                        for(Player p : new ArrayList<>(Bukkit.getOnlinePlayers())){
                            p.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get(), GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get()));
                            p.sendMessage("§aSauvegarde du monde d'édition, vous avez été renvoyé au lobby");
                        }

                        Game.unloadWorld(world, true);
                        player.sendMessage("§aLe monde a bien été sauvegardé");

                        return true;
                    } else if(args[1].equalsIgnoreCase("create")){
                        if(args.length != 3){
                            player.sendMessage("§cBon usage : '/castle edit create <world>'");
                            return false;
                        }

                        String worldName = args[2];
                        if(Bukkit.getWorld(worldName) != null || new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + worldName).exists()){
                            player.sendMessage("§cUn monde avec ce nom existe déjà");
                            return false;
                        }

                        World world = Game.createWorld(worldName);

                        if(world == null){
                            player.sendMessage("§cUne erreur est survenue lors de la création du monde");
                            return false;
                        }

                        player.setGameMode(GameMode.CREATIVE);
                        player.teleport(world.getHighestBlockAt(0, 0).getLocation().clone().add(0, 1, 0));
                        player.sendMessage("§aLe monde d'édition a été créé et vous avez été téléporté à l'intérieur");
                        return true;
                    }
                }
            } else if(args[0].equalsIgnoreCase("weapons")){
                WeaponsBundleListInventory inv = new WeaponsBundleListInventory(player, 1);
                inv.openInventory();
                return true;
            } else if(args[0].equalsIgnoreCase("save")){
                ProtectYourCastleMain.getInstance().saveCommon();
                ProtectYourCastleMain.getInstance().saveGame();
            } else if(args[0].equalsIgnoreCase("load")){
                ProtectYourCastleMain.getInstance().loadCommon();
                ProtectYourCastleMain.getInstance().loadGame();
            } else if (args[0].equalsIgnoreCase("ranking")) {
                if(args.length >= 2){
                    if(args[1].equalsIgnoreCase("place")){
                        if(args.length != 3){
                            player.sendMessage("§cBon usage : '/castle ranking place <type>'");
                            return false;
                        }

                        try {
                            RankingDisplay.RankingDisplayType type = RankingDisplay.RankingDisplayType.valueOf(args[2].toUpperCase());
                            RankingDisplay.spawnDisplay(type, player.getLocation().clone().add(0, .5, 0));
                            player.sendMessage("§aUn affichage de classement " + type.getDisplayName() + " a été créé à votre position");
                            return true;
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("§cLe type de classement " + args[1] + " n'existe pas");
                            return false;
                        }
                    } else if(args[1].equalsIgnoreCase("update")){
                        RankingDisplay.updateDisplays();
                    } else if(args[1].equalsIgnoreCase("remove")) {
                        TextDisplay display = (TextDisplay) player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2, entity -> entity instanceof TextDisplay).stream().min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation()))).orElse(null);
                        if(display != null){
                            if(RankingDisplay.removeDisplay(display)){
                                player.sendMessage("§aAffichage de classement supprimé");
                            } else {
                                player.sendMessage("§cAucun affichage de classement trouvé autour de vous");
                            }
                        }
                    }
                }
            }
        }

        player.sendMessage("§cSous-commande inconnue. Utilisez /castle help");
        this.sendHelp(player, null, true);
        return true;
    }

    private Villager getNearestTraderVillager(Player player, double radius){
        return (Villager) player.getWorld().getNearbyEntities(
                player.getLocation(),
                radius,
                radius,
                radius,
                entity -> entity instanceof Villager && Trader.isTrader(entity.getUniqueId())
        ).stream().min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation()))).orElse(null);
    }

    private Trader getNearestTrader(Player player, double radius){
        Villager villager = this.getNearestTraderVillager(player, radius);
        return villager == null ? null : Trader.getTrader(villager.getUniqueId());
    }

    private List<String> getTraderTypeNames(){
        return new ArrayList<>(Trader.getTraderTypeNames());
    }

    private List<String> getTraderNames(){
        return Trader.traders.values().stream()
                .filter(Objects::nonNull)
                .map(Trader::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private StartValidationReport validateStartConfiguration(String worldName){
        StartValidationReport report = new StartValidationReport();

        File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + worldName);
        if(!sourceGameWorldFolder.exists() || !sourceGameWorldFolder.isDirectory()){
            report.errors.add("La map '" + worldName + "' est introuvable dans le dossier worlds.");
            return report;
        }

        String[] requiredMapFiles = {"level.dat", "generators.json", "teams.json", "traders.json", "weapons.json"};
        for(String fileName : requiredMapFiles){
            if(!new File(sourceGameWorldFolder, fileName).exists()){
                report.errors.add("Fichier manquant dans la map '" + worldName + "': " + fileName);
            }
        }

        if(GameParameters.GAME_DURATION.get() <= 0){
            report.errors.add("Le paramètre game_duration doit être > 0.");
        }
        if(GameParameters.RESPAWN_DELAY.get() < 0){
            report.errors.add("Le paramètre respawn_delay doit être >= 0.");
        }
        if(GameParameters.MAP_RADIUS.get() <= 0){
            report.errors.add("Le paramètre map_radius doit être > 0.");
        }

        for(Team.TeamColor teamColor : Team.TeamColor.values()){
            Team team = Team.getTeam(teamColor);
            if(team == null){
                report.errors.add("L'équipe " + teamColor.name() + " n'est pas définie.");
                continue;
            }

            if(team.getSpawnLocation() == null){
                report.errors.add("L'équipe " + teamColor.name() + " n'a pas de spawn.");
            }
            if(team.getBannerLocation() == null){
                report.errors.add("L'équipe " + teamColor.name() + " n'a pas de bannière.");
            }
            if(team.getBase() == null){
                report.errors.add("L'équipe " + teamColor.name() + " n'a pas de zone de base.");
            }
            if(team.getRollbackLocation() == null){
                report.warnings.add("L'équipe " + teamColor.name() + " n'a pas de point rollback.");
            }
            if(team.getProtectedSpawn() == null){
                report.warnings.add("L'équipe " + teamColor.name() + " n'a pas de zone protégée.");
            }
            if(team.getDrawbridgeLocation() == null){
                report.warnings.add("L'équipe " + teamColor.name() + " n'a pas de pont-levis.");
            }
        }

        List<ResourceGenerator> generators = ResourceGenerator.getResourceGenerators();
        if(generators.isEmpty()){
            report.warnings.add("Aucun générateur de ressources n'est configuré.");
        } else {
            for(int i = 0; i < generators.size(); i++){
                ResourceGenerator generator = generators.get(i);
                if(generator == null){
                    report.errors.add("Le générateur #" + (i + 1) + " est invalide (null).");
                    continue;
                }

                if(generator.getMaterial() == null){
                    report.errors.add("Le générateur #" + (i + 1) + " n'a pas de matériau.");
                }
                if(generator.getCooldown() <= 0){
                    report.errors.add("Le générateur #" + (i + 1) + " a un cooldown <= 0.");
                }

                Location location;
                try {
                    location = generator.getLocation();
                } catch (Exception e){
                    report.errors.add("Le générateur #" + (i + 1) + " a une position invalide.");
                    continue;
                }
                if(location == null){
                    report.errors.add("Le générateur #" + (i + 1) + " n'a pas de position.");
                }
            }
        }

        if(Trader.traders.isEmpty()){
            report.warnings.add("Aucun marchand n'est actuellement enregistré.");
        } else {
            for(Map.Entry<UUID, Trader> entry : Trader.traders.entrySet()){
                UUID traderUUID = entry.getKey();
                Trader trader = entry.getValue();
                if(traderUUID == null || trader == null){
                    report.errors.add("Un marchand enregistré est invalide (UUID ou valeur null).");
                    continue;
                }

                if(trader.getName() == null || trader.getName().isBlank()){
                    report.errors.add("Le marchand " + traderUUID + " n'a pas de nom.");
                }
                if(trader.getSavedLocation() == null){
                    report.warnings.add("Le marchand '" + trader.getName() + "' n'a pas de position sauvegardée.");
                }
                if(trader.isWeaponTrader() && GameParameters.ENABLE_RANDOM_WEAPONS.get() && Trader.weapons.isEmpty()){
                    report.warnings.add("Le marchand d'armes '" + trader.getName() + "' n'a aucun bundle d'armes disponible.");
                }
                if(!trader.isWeaponTrader() && trader.getTrades().isEmpty()){
                    report.warnings.add("Le marchand '" + trader.getName() + "' n'a aucun trade.");
                }
            }
        }

        return report;
    }

    private static class StartValidationReport {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
    }

    private void sendHelp(Player player, @Nullable String topic, boolean canManage){
        String normalizedTopic = topic == null ? "" : topic.toLowerCase(Locale.ROOT);
        List<String> topics = this.getHelpTopics();

        if(!canManage){
            player.sendMessage("§6[Castle] §eAide");
            player.sendMessage("§7Vous n'avez pas accès aux commandes d'administration.");
            player.sendMessage("§7Commandes disponibles:");
            player.sendMessage("§f- /castle help");
            return;
        }

        if(normalizedTopic.isBlank()){
            player.sendMessage("§6[Castle] §eAide générale");
            player.sendMessage("§7Commandes principales:");
            player.sendMessage("§f- /castle start <map>");
            player.sendMessage("§f- /castle stop");
            player.sendMessage("§f- /castle team ...");
            player.sendMessage("§f- /castle generator ...");
            player.sendMessage("§f- /castle trader ...");
            player.sendMessage("§f- /castle edit ...");
            player.sendMessage("§f- /castle parameter ...");
            player.sendMessage("§f- /castle ranking ...");
            player.sendMessage("§f- /castle scores [refresh]");
            player.sendMessage("§f- /castle weapons");
            player.sendMessage("§7Détail: /castle help <topic>");
            player.sendMessage("§7Topics: " + String.join(", ", topics));
            return;
        }

        switch (normalizedTopic){
            case "game", "start", "stop" -> {
                player.sendMessage("§6[Castle] §eAide game");
                player.sendMessage("§f- /castle start <map> §7Lance une partie");
                player.sendMessage("§f- /castle stop §7Arrête la partie");
                player.sendMessage("§f- /castle save §7Sauvegarde paramètres + game");
                player.sendMessage("§f- /castle load §7Recharge paramètres + game");
            }
            case "generator", "generators" -> {
                player.sendMessage("§6[Castle] §eAide generator");
                player.sendMessage("§f- /castle generator add <material> <delay>");
                player.sendMessage("§f- /castle generator remove");
                player.sendMessage("§f- /castle generator edit_all <material> <delay>");
            }
            case "team", "teams" -> {
                player.sendMessage("§6[Castle] §eAide team");
                player.sendMessage("§f- /castle team spawn <team> <x> <y> <z>");
                player.sendMessage("§f- /castle team banner <team>");
                player.sendMessage("§f- /castle team base <team> <x1> <y1> <z1> <x2> <y2> <z2>");
                player.sendMessage("§f- /castle team protected <team> <x1> <y1> <z1> <x2> <y2> <z2>");
                player.sendMessage("§f- /castle team rollback <team> <x> <y> <z>");
                player.sendMessage("§f- /castle team drawbridge <team> <x> <y> <z>");
                player.sendMessage("§f- /castle team join <team> <player>");
                player.sendMessage("§f- /castle team leave <player>");
                player.sendMessage("§f- /castle team fill [withClear|withoutClear] [randomly]");
                player.sendMessage("§f- /castle team clear");
            }
            case "trader", "traders" -> {
                player.sendMessage("§6[Castle] §eAide trader");
                player.sendMessage("§f- /castle trader spawn [name] [--type <type>]");
                player.sendMessage("§f- /castle trader remove");
                player.sendMessage("§f- /castle trader respawn");
                player.sendMessage("§f- /castle trader type list");
                player.sendMessage("§f- /castle trader type save <type>");
                player.sendMessage("§f- /castle trader type bind <type>");
                player.sendMessage("§f- /castle trader type unbind");
                player.sendMessage("§f- /castle trader type duplicate <source> <target>");
                player.sendMessage("§f- /castle trader type delete <type>");
            }
            case "parameter", "params", "config" -> {
                player.sendMessage("§6[Castle] §eAide parameter");
                player.sendMessage("§f- /castle parameter list");
                player.sendMessage("§f- /castle parameter get <parameter>");
                player.sendMessage("§f- /castle parameter set <parameter> <value>");
            }
            case "edit", "world", "worlds" -> {
                player.sendMessage("§6[Castle] §eAide edit");
                player.sendMessage("§f- /castle edit join <world>");
                player.sendMessage("§f- /castle edit create <world>");
                player.sendMessage("§f- /castle edit save");
                player.sendMessage("§f- /castle edit leave");
            }
            case "ranking", "rankings" -> {
                player.sendMessage("§6[Castle] §eAide ranking");
                player.sendMessage("§f- /castle ranking place <type>");
                player.sendMessage("§f- /castle ranking update");
                player.sendMessage("§f- /castle ranking remove");
            }
            case "scores", "stats" -> {
                player.sendMessage("§6[Castle] §eAide scores");
                player.sendMessage("§f- /castle scores");
                player.sendMessage("§f- /castle scores refresh");
                player.sendMessage("§f- /stats");
            }
            default -> {
                player.sendMessage("§cTopic d'aide inconnu: " + topic);
                player.sendMessage("§7Topics disponibles: " + String.join(", ", topics));
            }
        }
    }

    private List<String> getHelpTopics(){
        return List.of("game", "generator", "team", "trader", "parameter", "edit", "ranking", "scores");
    }

    private List<String> getWorldNames() {
        File[] worlds = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/").listFiles();
        if(worlds == null){
            return List.of();
        }
        return Arrays.stream(worlds)
                .filter(File::isDirectory)
                .map(File::getName)
                .sorted()
                .toList();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            return List.of();
        }
        if(args.length == 0){
            return List.of();
        }

        boolean canManage = player.hasPermission("castle.manage");
        if(args.length == 1){
            return tabComplete(args[0], canManage
                    ? List.of("help", "generator", "start", "team", "trader", "parameter", "stop", "scores", "edit", "weapons", "save", "load", "ranking")
                    : List.of("help"));
        }
        if(args[0].equalsIgnoreCase("help")){
            return args.length == 2 ? tabComplete(args[1], this.getHelpTopics()) : List.of();
        }
        if(!canManage){
            return List.of();
        }

        if(args.length == 2){
            if(args[0].equalsIgnoreCase("generator")){
                return tabComplete(args[1], "add", "remove", "edit_all");
            } else if(args[0].equalsIgnoreCase("team")){
                return tabComplete(args[1], "spawn", "base", "banner", "join", "leave", "protected", "rollback", "drawbridge", "fill", "clear");
            } else if(args[0].equalsIgnoreCase("trader")){
                return tabComplete(args[1], "spawn", "remove", "respawn", "type");
            } else if(args[0].equalsIgnoreCase("parameter")){
                return tabComplete(args[1], "set", "get", "list");
            } else if(args[0].equalsIgnoreCase("edit")){
                return tabComplete(args[1], "join", "leave", "save", "create");
            } else if (args[0].equalsIgnoreCase("ranking")) {
                return tabComplete(args[1], "place", "update", "remove");
            } else if(args[0].equalsIgnoreCase("scores")){
                return tabComplete(args[1], "refresh");
            } else if(args[0].equalsIgnoreCase("start")){
                return tabComplete(args[1], this.getWorldNames());
            }
        } else if(args.length == 3){
            if(args[0].equalsIgnoreCase("generator")){
                if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("edit_all")){
                    return tabComplete(args[2], Arrays.stream(Material.values()).map(mat -> mat.name().toLowerCase()));
                }
            } else if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base") || args[1].equalsIgnoreCase("banner") || args[1].equalsIgnoreCase("join")){
                    return tabComplete(args[2], Arrays.stream(Team.TeamColor.values()).map(teamColor -> teamColor.name().toLowerCase()).toList());
                } else if(args[1].equalsIgnoreCase("fill")){
                    return tabComplete(args[2], "withClear", "withoutClear");
                }
            } else if(args[0].equalsIgnoreCase("parameter")){
                if(args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("get")){
                    return GameParameters.Parameter.existingParameters.stream().map(GameParameters.Parameter::getName).filter(s -> s.contains(args[2])).toList();
                }
            } else if(args[0].equalsIgnoreCase("trader")){
                if(args[1].equalsIgnoreCase("spawn")){
                    List<String> suggestions = new ArrayList<>(this.getTraderNames());
                    suggestions.add("Marchand");
                    suggestions.add("--type");
                    return tabComplete(args[2], suggestions);
                }
                if(args[1].equalsIgnoreCase("type")){
                    return tabComplete(args[2], "list", "save", "bind", "unbind", "duplicate", "delete");
                }
            } else if (args[0].equalsIgnoreCase("ranking")) {
                if(args[1].equalsIgnoreCase("place")){
                    return tabComplete(args[2], Stream.of(RankingDisplay.RankingDisplayType.values()).map(type -> type.name().toLowerCase()));
                }
            } else if(args[0].equalsIgnoreCase("edit")){
                if(args[1].equalsIgnoreCase("join")){
                    return tabComplete(args[2], this.getWorldNames());
                }
            }
        } else if(args.length == 4){
            if(args[0].equalsIgnoreCase("generator")){
                if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("edit_all")){
                    return tabComplete(args[3], "1", "10", "20", "30", "60");
                }
            } else if(args[0].equalsIgnoreCase("trader")){
                if(args[1].equalsIgnoreCase("spawn")){
                    if(args[2].equalsIgnoreCase("--type")){
                        return tabComplete(args[3], this.getTraderTypeNames());
                    }
                    return tabComplete(args[3], "--type");
                } else if(args[1].equalsIgnoreCase("type")){
                    if(args[2].equalsIgnoreCase("save") || args[2].equalsIgnoreCase("bind") || args[2].equalsIgnoreCase("delete") || args[2].equalsIgnoreCase("duplicate")){
                        return tabComplete(args[3], this.getTraderTypeNames());
                    }
                }
            } else if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockX() + "") : List.of();
                } else if(args[1].equalsIgnoreCase("join")){
                    return tabComplete(args[3], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                } else if(args[1].equalsIgnoreCase("fill")){
                    return tabComplete(args[3], "randomly");
                }
            }
        } else if(args.length == 5){
             if(args[0].equalsIgnoreCase("team")){
                 if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockY() + "") : List.of();
                }
            } else if(args[0].equalsIgnoreCase("trader")){
                if(args[1].equalsIgnoreCase("spawn") && args[3].equalsIgnoreCase("--type")){
                    return tabComplete(args[4], this.getTraderTypeNames());
                }
                if(args[1].equalsIgnoreCase("type") && args[2].equalsIgnoreCase("duplicate")){
                    String duplicateHint = args[3].isBlank() ? "type_copy" : args[3] + "_copy";
                    return tabComplete(args[4], duplicateHint);
                }
            }
        } else if(args.length == 6){
            if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockZ() + "") : List.of();
                }
            }
        } else if(args.length == 7){
            if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("base") || args[1].equalsIgnoreCase("protected")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockX() + "") : List.of();
                }
            }
        } else if(args.length == 8){
            if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("base") || args[1].equalsIgnoreCase("protected")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockY() + "") : List.of();
                }
            }
        } else if(args.length == 9){
            if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("base") || args[1].equalsIgnoreCase("protected")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockZ() + "") : List.of();
                }
            }
        }

        return List.of();
    }
}
