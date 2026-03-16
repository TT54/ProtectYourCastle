package fr.tt54.protectYourCastle.listeners;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.game.GameParameters;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.mod_bridges.CuriosBridge;
import fr.tt54.protectYourCastle.scoreboard.ScoreboardManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event){
        final Player player = event.getPlayer();
        final Team team = Team.getPlayerTeam(player.getUniqueId());

        if(team != null){
            player.setPlayerListName(team.getColor().getChatColor() + "[" + team.getColor().name() + "] " + player.getName());
        }

        if(Game.getCurrentGame() != null) {
            if (team != null) {
                if(player.getGameMode() == GameMode.SPECTATOR) beginRespawn(player, team, Game.getCurrentGame());
                ProtectYourCastleMain.voiceChatBridge.joinTeamGroup(player, team);
            }

            if(Game.getCurrentGame().isRunning() && Game.getCurrentGame().getScoreboard() != null){
                ScoreboardManager.showScoreboard(player, Game.getCurrentGame().getScoreboard());
            }

            if(team == null){
                player.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get() + .5d, GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get() + .5d));
                player.setGameMode(GameMode.SURVIVAL);
            }

            Game.getCurrentGame().displayHolderBars(player);
        } else{
            player.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get() + .5d, GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get() + .5d));
            player.setGameMode(GameMode.SURVIVAL);
            ProtectYourCastleMain.voiceChatBridge.joinGlobalGroup(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){
        if(Game.getCurrentGame() == null || !Game.getCurrentGame().isRunning()) return;

        Player player = event.getPlayer();
        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team != null){
            for(Team t : Team.getTeams()){
                if(t != team && t.getBase().contains(event.getBlock().getLocation())){
                    event.setCancelled(true);
                    if(event.getBlock().getLocation().distanceSquared(t.getBannerLocation()) < .1){
                        Game game = Game.getCurrentGame();
                        if(game.getBannerHolders().containsKey(team.getColor()) || !game.pickupBanner(player)){
                            player.sendMessage("§cVotre équipe a déjà une bannière dans un inventaire");
                            return;
                        }
                        Bukkit.broadcastMessage("§6[Castle] " + t.getColor().getChatColor() + player.getName() + "§a a volé une bannière à la team " + t.getColor().getChatColor() + t.getColor().name());
                        Game.getCurrentGame().addBannerBroken(player);
                    } else {
                        player.sendMessage("§cVous ne pouvez pas casser de blocs à la main dans la base ennemie");
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event){
        if(Game.getCurrentGame() == null || !Game.getCurrentGame().isRunning()) return;

        Player player = event.getPlayer();
        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team != null){
            for(Team t : Team.getTeams()){
                if(t != team && t.getBase().contains(event.getBlock().getLocation())){
                    event.setCancelled(true);
                    player.sendMessage("§cVous ne pouvez pas poser de blocs à la main dans la base ennemie");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event){
        if(Game.getCurrentGame() == null || !Game.getCurrentGame().isRunning()) return;
        Player player = event.getPlayer();

        if(event.getClickedBlock() != null && event.getHand() == EquipmentSlot.HAND){
            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team != null && event.getClickedBlock().getLocation().distanceSquared(team.getBannerLocation()) < .1){
                event.setCancelled(true);
                if(Game.getCurrentGame().isBannerHolder(player)){
                    Game.getCurrentGame().placeBanner(team, player);
                } else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
                    player.sendMessage("§cVous ne pouvez pas casser votre bannière !");
                }
                return;
            }
        }

        if(Team.isBannerItem(event.getItem())){
            event.setCancelled(true);
            player.sendMessage("§cVous ne pouvez pas interagir avec la bannière en main");
        }
    }

    private static final Set<Material> ALLOWED_DROPS = Set.of(Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD);

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event){
        final Player player = event.getEntity();
        final Team team = Team.getPlayerTeam(event.getEntity().getUniqueId());

        List<ItemStack> addedDrops = new ArrayList<>();
        for(ItemStack is : event.getEntity().getInventory().getStorageContents()){
            if(is == null) continue;
            if(ALLOWED_DROPS.contains(is.getType())) addedDrops.add(is.clone());
            is.setAmount(0);
        }
        for(ItemStack is : event.getEntity().getInventory().getExtraContents()){
            if(is == null) continue;
            if(ALLOWED_DROPS.contains(is.getType())) addedDrops.add(is.clone());
            is.setAmount(0);
        }
        for(ItemStack is : event.getEntity().getInventory().getArmorContents()){
            if(is == null) continue;
            if(!GameParameters.KEEP_ARMOR.get()){
                is.setAmount(0);
            } else if(is.getType().name().contains("NETHERITE")){
                Material newType = Material.getMaterial(is.getType().name().replace("NETHERITE", "DIAMOND"));
                if(newType != null) {
                    is.setType(newType);
                }
            }
        }
        event.getDrops().addAll(addedDrops);
        if(!GameParameters.KEEP_ARTIFACTS.get()){
            CuriosBridge.clearPlayerCuriosInventory(player);
        }

        if(Game.getCurrentGame() != null && Game.getCurrentGame().isRunning()){
            boolean bannerHolder = team != null && player.getUniqueId().equals(Game.getCurrentGame().getBannerHolders().get(team.getColor()));

            if(player.getKiller() != null && player.getKiller() != player) {
                final Team killerTeam = Team.getPlayerTeam(player.getKiller().getUniqueId());
                if(killerTeam != team) {
                    Game.getCurrentGame().addKill(player.getKiller(), bannerHolder);
                }
            }

            if(bannerHolder){
                Game.getCurrentGame().getBannerHolders().remove(team.getColor());
                Bukkit.broadcastMessage("§6[Castle] " + team.getColor().getChatColor() + player.getName() + "§c a perdu la bannière ennemi qu'il transportait");
            }

            Game.getCurrentGame().addDeath(player);
        }

        Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), () -> {
            player.spigot().respawn();
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event){
        final Team team = Team.getPlayerTeam(event.getPlayer().getUniqueId());
        final Game game = Game.getCurrentGame();
        if(team != null && game != null){
            final Player player = event.getPlayer();
            beginRespawn(player, team, game);
        } else{
            event.setRespawnLocation(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get() + .5d, GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get() + .5d));
        }
    }

    private void beginRespawn(final Player player, Team team, Game game){
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§cVous allez respawn dans " + GameParameters.RESPAWN_DELAY.get() + " secondes");
        new BukkitRunnable() {

            int timeLeft = GameParameters.RESPAWN_DELAY.get();

            @Override
            public void run() {
                if(timeLeft == 0) {
                    game.spawnPlayer(player, team, false);
                    this.cancel();
                    return;
                } else{
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cRespawn dans " + timeLeft + "s"));
                }

                if(Game.getCurrentGame() == null || !Game.getCurrentGame().isRunning()){
                    this.cancel();
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(ProtectYourCastleMain.getInstance(), 0, 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractWithEntity(PlayerInteractEntityEvent event){
        if(Trader.isTrader(event.getRightClicked().getUniqueId())){
            event.setCancelled(true);
            if(event.getPlayer().isSneaking() && event.getPlayer().getGameMode() == GameMode.CREATIVE){
                Trader.openEditionMenu(event.getRightClicked().getUniqueId(), event.getPlayer());
            } else {
                Trader.openTradeMenu(event.getRightClicked().getUniqueId(), event.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void blockExplose(BlockExplodeEvent event){
        if(Game.getCurrentGame() != null){
            event.blockList().removeIf(block -> {
                if(block == null) return false;

                for(Team.TeamColor teamColor : Team.TeamColor.values()){
                    Team team = Team.getTeam(teamColor);
                    if(team.getBannerLocation().distanceSquared(block.getLocation()) <= 16){
                        return true;
                    }
                }

                return false;
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void entityExplose(EntityExplodeEvent event){
        if(Game.getCurrentGame() != null){
            event.blockList().removeIf(block -> {
                if(block == null) return false;

                for(Team.TeamColor teamColor : Team.TeamColor.values()){
                    Team team = Team.getTeam(teamColor);
                    if(team.getBannerLocation().distanceSquared(block.getLocation()) <= 16){
                        return true;
                    }
                }

                return false;
            });
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof Player player && GameParameters.ENABLE_DAMAGE_INDICATOR.get()){
            final Random random = new Random();
            final Location damageIndicatorLocation = player.getLocation().clone().add(
                    (random.nextBoolean() ? .2 : -.2) * (.5 + random.nextDouble()),
                    3,
                    (random.nextBoolean() ? .2 : -.2) * (.5 + random.nextDouble())
            );
            final TextDisplay textDisplay = (TextDisplay) player.getWorld().spawnEntity(damageIndicatorLocation, EntityType.TEXT_DISPLAY);
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setSeeThrough(true);
            DecimalFormat format = new DecimalFormat("0.0");
            textDisplay.setText("§b" + format.format(event.getFinalDamage()));

            Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), textDisplay::remove, 15L);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){
        Game game = Game.getCurrentGame();
        if(game != null && game.isRunning()) {
            Team team = Team.getPlayerTeam(event.getPlayer().getUniqueId());
            Entity entity = event.getPlayer().getVehicle();
            if(team != null && event.getTo() != null && event.getFrom().getWorld() == event.getTo().getWorld() && entity != null && !entity.isOnGround()){
                double distance = event.getTo().distance(event.getFrom());
                game.increaseDistanceInPlane(event.getPlayer(), distance);
            }
        }
    }

}
