package fr.tt54.protectYourCastle.cmd;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.game.*;
import fr.tt54.protectYourCastle.inventories.ConfirmationInventory;
import fr.tt54.protectYourCastle.inventories.trades.weapons.WeaponsListInventory;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

                if(Game.loadedWorld != null){
                    player.sendMessage("§cLe monde de jeu est déjà chargé, impossible de lancer une nouvelle partie");
                    return false;
                }

                if(!Game.createNew()){
                    player.sendMessage("§cUne partie est déjà en cours");
                    return false;
                }

                boolean empty = true;
                for(Player p : Bukkit.getOnlinePlayers()){
                    Team team = Team.getPlayerTeam(player.getUniqueId());
                    if(team != null) {
                        empty = false;
                        break;
                    }
                }

                if(empty) {
                    Game.currentGame = null;
                    player.sendMessage("§cImpossible de lancer la partie, aucune équipe n'a de joueur");
                    return false;
                }

                Game.currentGame.prepare(args[1]);
                Game.currentGame.launch();
                player.sendMessage("§aLa partie a bien été lancée");
                return true;
            } else if(args[0].equalsIgnoreCase("stop")){
                if(Game.currentGame == null){
                    player.sendMessage("§cIl n'y a aucune partie en cours");
                    return false;
                }
                Game.currentGame.stop();
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
                        boolean clearBefore = args.length == 3 && args[2].equalsIgnoreCase("withClear");
                        if(clearBefore){
                            for(Team team : Team.getTeams()){
                                for(UUID uuid : new ArrayList<>(team.getMembers())){
                                    team.leaveTeam(uuid);
                                }
                            }
                        }

                        Team.fillWithScores();
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
                        String name = "Marchant";
                        if(args.length > 2){
                            name = "";
                            for(int i = 2; i < args.length; i++){
                                name += " " + args[i];
                            }
                            name = name.substring(1);
                        }

                        Trader sameExisting = null;
                        for(Trader trader : Trader.traders.values()){
                            if(trader.getName().equalsIgnoreCase(name)){
                                sameExisting = trader;
                                player.sendMessage("§aUn marchant du même nom a été trouvé, ses trades ont été copiés");
                            }
                        }

                        Trader trader;
                        if(sameExisting != null){
                            List<Trader.NPCTrade> trades = new ArrayList<>();
                            for(Trader.NPCTrade trade : sameExisting.getTrades()){
                                trades.add(trade.clone());
                            }
                            trader = new Trader(name, trades, false);
                        } else {
                            trader = new Trader(name, false);
                        }
                        trader.spawn(player.getLocation());
                        player.sendMessage("§aVous avez fait apparaître un marchant");
                        return true;
                    } else if(args[1].equalsIgnoreCase("remove")){
                        Villager villager = (Villager) player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2, entity -> entity instanceof Villager).stream().min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation()))).orElse(null);
                        Trader removed = villager != null ? Trader.getTrader(villager.getUniqueId()) : null;

                        if(removed != null){
                            Trader.removeTrader(villager.getUniqueId());
                            villager.remove();
                            player.sendMessage("§aLe marchant " + removed.getName() + " a bien été supprimé");
                            return true;
                        } else {
                            player.sendMessage("§cAucun marchant trouvé autour de vous");
                            return false;
                        }
                    } else if(args[1].equalsIgnoreCase("respawn")){
                        for(Trader trader : new ArrayList<>(Trader.traders.values())){
                            if(trader.getSavedLocation() != null){
                                Location loc = trader.getSavedLocation().toLocation();
                                if(loc.getWorld() == player.getWorld() && loc.distance(player.getLocation()) < 64){
                                    trader.respawn();
                                }
                            }
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
                        if(Game.loadedWorld == null){
                            player.sendMessage("§cAucun monde d'édition n'est chargé");
                            return false;
                        }

                        World world = Bukkit.getWorld(Game.loadedWorld);
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
                        if(Game.loadedWorld == null){
                            player.sendMessage("§cAucun monde d'édition n'est chargé");
                            return false;
                        }

                        World world = Bukkit.getWorld(Game.loadedWorld);

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
                WeaponsListInventory inv = new WeaponsListInventory(player, 1);
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

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            return List.of();
        }

        if(args.length == 1){
            return tabComplete(args[0], "generator", "start", "team", "trader", "parameter", "stop", "scores", "edit", "weapons", "save", "load", "ranking");
        } else if(args.length == 2){
            if(args[0].equalsIgnoreCase("generator")){
                return tabComplete(args[1], "add", "remove", "edit_all");
            } else if(args[0].equalsIgnoreCase("team")){
                return tabComplete(args[1], "spawn", "base", "banner", "join", "leave", "protected", "rollback", "drawbridge", "fill", "clear");
            } else if(args[0].equalsIgnoreCase("trader")){
                return tabComplete(args[1], "spawn", "remove", "respawn");
            } else if(args[0].equalsIgnoreCase("parameter")){
                return tabComplete(args[1], "set", "get", "list");
            } else if(args[0].equalsIgnoreCase("edit")){
                return tabComplete(args[1], "join", "leave", "save", "create");
            } else if (args[0].equalsIgnoreCase("ranking")) {
                return tabComplete(args[1], "place", "update", "remove");
            } else if(args[0].equalsIgnoreCase("scores")){
                return tabComplete(args[1], "refresh");
            } else if(args[0].equalsIgnoreCase("start")){
                return tabComplete(args[1], Arrays.stream(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/").listFiles()).map(File::getName).toList());
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
                    return tabComplete(args[2], "withClear");
                }
            } else if(args[0].equalsIgnoreCase("parameter")){
                if(args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("get")){
                    return GameParameters.Parameter.existingParameters.stream().map(GameParameters.Parameter::getName).filter(s -> s.contains(args[2])).toList();
                }
            } else if (args[0].equalsIgnoreCase("ranking")) {
                if(args[1].equalsIgnoreCase("place")){
                    return tabComplete(args[2], Stream.of(RankingDisplay.RankingDisplayType.values()).map(type -> type.name().toLowerCase()));
                }
            } else if(args[0].equalsIgnoreCase("edit")){
                if(args[1].equalsIgnoreCase("join")){
                    return tabComplete(args[2], Arrays.stream(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/").listFiles()).map(File::getName).toList());
                }
            }
        } else if(args.length == 4){
            if(args[0].equalsIgnoreCase("generator")){
                if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("edit_all")){
                    return tabComplete(args[3], "1", "10", "20", "30", "60");
                }
            } else if(args[0].equalsIgnoreCase("team")){
                if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockX() + "") : List.of();
                } else if(args[1].equalsIgnoreCase("join")){
                    return tabComplete(args[3], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                }
            }
        } else if(args.length == 5){
             if(args[0].equalsIgnoreCase("team")){
                 if(args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("drawbridge") || args[1].equalsIgnoreCase("rollback") || args[1].equalsIgnoreCase("protected") || args[1].equalsIgnoreCase("base")){
                    Block block = player.getTargetBlockExact(5);
                    return block != null ? List.of(block.getLocation().getBlockY() + "") : List.of();
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
