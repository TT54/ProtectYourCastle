package fr.tt54.protectYourCastle.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.inventories.lobby.GameStatsInventory;
import fr.tt54.protectYourCastle.mod_bridges.CuriosBridge;
import fr.tt54.protectYourCastle.runnable.GameRunnable;
import fr.tt54.protectYourCastle.scoreboard.GameScoreboard;
import fr.tt54.protectYourCastle.scoreboard.ScoreboardManager;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import fr.tt54.protectYourCastle.utils.SavedLocation;
import fr.tt54.protectYourCastle.utils.SerializerUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Game {

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(ItemStack.class, new SerializerUtils.ItemStackSerializer())
            .registerTypeHierarchyAdapter(ItemStack.class, new SerializerUtils.ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new SerializerUtils.ItemStackDeserializer())
            .registerTypeHierarchyAdapter(ItemStack.class, new SerializerUtils.ItemStackDeserializer())
            .registerTypeAdapter(Location.class, new SerializerUtils.LocationSerializer())
            .registerTypeAdapter(Location.class, new SerializerUtils.LocationDeserializer())
            .registerTypeAdapter(SavedLocation.class, new SerializerUtils.SavedLocationSerializer())
            .registerTypeAdapter(SavedLocation.class, new SerializerUtils.SavedLocationDeserializer())
            .registerTypeAdapter(Area.class, new Area.AreaSerializer())
            .registerTypeAdapter(Area.class, new Area.AreaDeserializer())
            .registerTypeAdapter(GameParameters.class, new GameParameters.GameParametersJsonSerializer())
            .registerTypeAdapter(GameParameters.class, new GameParameters.GameParametersJsonDeserializer())
            .create();

    private static Game currentGame;
    private static String loadedWorld = null;

    private Status gameStatus;
    private int time;
    private final Map<Team.TeamColor, Integer> points = new HashMap<>();
    private final Map<Team.TeamColor, UUID> bannerHolders = new HashMap<>();
    private GameStatistics gameStatistics;
    private List<Trader.GameWeapon> selectedWeapons = new ArrayList<>();

    private GameRunnable runnable;
    private World world;
    private GameScoreboard scoreboard;
    private MovementTraceRecorder movementTraceRecorder;

    public Game() {
    }

    public static boolean createNew() {
        if(currentGame == null || currentGame.gameStatus == Status.STOPPED){
            currentGame = new Game();
            return true;
        }
        return false;
    }

    public static World loadWorld(String worldName){
        if(loadedWorld != null){
            System.err.println("Un monde est déjà chargé, impossible d'en charger un autre tant que la partie n'est pas terminée !");
            return null;
        }

        File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + worldName);
        File gameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder().getParentFile().getParentFile(), worldName);
        if(gameWorldFolder.exists()) {
            try (Stream<Path> paths = Files.walk(gameWorldFolder.toPath())) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(!sourceGameWorldFolder.exists() || !sourceGameWorldFolder.isDirectory()){
            return null;
        }

        FileManager.copy(sourceGameWorldFolder, gameWorldFolder);
        FileManager.copy(new File(sourceGameWorldFolder, "generators.json"), new File(ProtectYourCastleMain.getInstance().getDataFolder(), "generators.json"));
        FileManager.copy(new File(sourceGameWorldFolder, "teams.json"), new File(ProtectYourCastleMain.getInstance().getDataFolder(), "teams.json"));
        FileManager.copy(new File(sourceGameWorldFolder, "traders.json"), new File(ProtectYourCastleMain.getInstance().getDataFolder(), "traders.json"));
        FileManager.copy(new File(sourceGameWorldFolder, "weapons.json"), new File(ProtectYourCastleMain.getInstance().getDataFolder(), "weapons.json"));
        File sourceTraderTypesFile = new File(sourceGameWorldFolder, "trader_types.json");
        File targetTraderTypesFile = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "trader_types.json");
        if(sourceTraderTypesFile.exists()){
            FileManager.copy(sourceTraderTypesFile, targetTraderTypesFile);
        } else {
            FileManager.write("{}", targetTraderTypesFile);
        }

        ProtectYourCastleMain.getInstance().loadGame();
        loadedWorld = worldName;

        WorldCreator creator = new WorldCreator(loadedWorld);
        return creator.createWorld();
    }

    public static void unloadWorld(World world, boolean save) {
        Bukkit.unloadWorld(world, save);

        if(save) {
            File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + loadedWorld);
            File gameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder().getParentFile().getParentFile(), loadedWorld);

            if(sourceGameWorldFolder.exists()) {
                try (Stream<Path> paths = Files.walk(sourceGameWorldFolder.toPath())) {
                    paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            FileManager.copy(gameWorldFolder, sourceGameWorldFolder);
            ProtectYourCastleMain.getInstance().saveGame();

            FileManager.copy(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "generators.json"), new File(sourceGameWorldFolder, "generators.json"));
            FileManager.copy(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "teams.json"), new File(sourceGameWorldFolder, "teams.json"));
            FileManager.copy(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "traders.json"), new File(sourceGameWorldFolder, "traders.json"));
            FileManager.copy(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "weapons.json"), new File(sourceGameWorldFolder, "weapons.json"));
            FileManager.copy(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "trader_types.json"), new File(sourceGameWorldFolder, "trader_types.json"));
        }

        loadedWorld = null;
        System.out.println("Monde " + world.getName() + " déchargé !");
    }

    public static World createWorld(String worldName) {
        System.out.println(loadedWorld);
        if(loadedWorld != null){
            System.err.println("Un monde est déjà chargé, impossible d'en charger un autre tant que la partie n'est pas terminée !");
            return null;
        }
        loadedWorld = worldName;

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);

        return creator.createWorld();
    }

    public static Game getCurrentGame() {
        return currentGame;
    }

    public static String getLoadedWorld() {
        return loadedWorld;
    }


    public void prepare(String worldName){
        if(loadedWorld != null){
            System.err.println("Un monde est déjà chargé, impossible d'en charger un autre tant que la partie n'est pas terminée !");
            return;
        }

        this.world = loadWorld(worldName);
        if(this.world == null){
            System.err.println("Le monde source " + worldName + " n'existe pas !");
            return;
        }

        this.gameStatus = Status.PREPARING;

        if(GameParameters.ENABLE_RANDOM_WEAPONS.get() && !Trader.weapons.isEmpty()){
            Random random = new Random();
            this.selectedWeapons = Trader.weapons.get(random.nextInt(Trader.weapons.size()));
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            // TODO Ouvrir le menu de sélection d'équipe
        }
    }

    public void launch(){
        if(gameStatus == Status.PREPARING){
            for(Team team : Team.getTeams()){
                this.points.put(team.getColor(), 0);
                ProtectYourCastleMain.voiceChatBridge.createTeamGroup(team);
                for(UUID member : new ArrayList<>(team.getMembers())){
                    if(Bukkit.getPlayer(member) == null){
                        team.leaveTeam(member);
                    }
                }
            }

            this.runnable = new GameRunnable(this);
            this.runnable.runTaskTimer(ProtectYourCastleMain.getInstance(), 20, 20);

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(true);
            }

            scoreboard = new GameScoreboard();

            World world = this.world;
            world.setTime(6000);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setDifficulty(Difficulty.HARD);
            world.setThundering(false);
            world.setStorm(false);
            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(0, 0);
            worldBorder.setSize(2 * GameParameters.MAP_RADIUS.get() + 1);

            for(Player player : new ArrayList<>(Bukkit.getOnlinePlayers())){
                ScoreboardManager.showScoreboard(player, scoreboard);
                Team team = Team.getPlayerTeam(player.getUniqueId());
                player.getEnderChest().clear();
                if(team != null) {
                    spawnPlayer(player, team, true);
                }
            }

            this.gameStatus = Status.RUNNING;
            this.gameStatistics = new GameStatistics(System.currentTimeMillis(), -1, new HashMap<>(), Team.getPlayerTeamMapCopy());
            this.movementTraceRecorder = new MovementTraceRecorder(this, this.world);
            if(this.movementTraceRecorder.start()){
                ProtectYourCastleMain.getInstance().getLogger().info("Movement trace started: " + this.movementTraceRecorder.getOutputFile().getName());
            } else {
                this.movementTraceRecorder = null;
            }
        }
    }

    public GameStatistics stop(){
        if(gameStatus != Status.STOPPED) {
            if(this.runnable != null) {
                this.runnable.cancel();
                this.runnable = null;
            }
            this.stopMovementTraceRecorder();
            currentGame = null;
            loadedWorld = null;

            if(this.gameStatistics != null) {
                this.gameStatistics.setGameEnd(System.currentTimeMillis());
                GameStatistics.gameStatistics.add(this.gameStatistics);
                RankingDisplay.updateDisplays();
            }

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(false);
            }

            for(Player player : new ArrayList<>(Bukkit.getOnlinePlayers())){
                ScoreboardManager.removeScoreboard(player);

                AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if(attributeInstance != null){
                    attributeInstance.setBaseValue(attributeInstance.getDefaultValue());
                    for(AttributeModifier modifier : attributeInstance.getModifiers()){
                        attributeInstance.removeModifier(modifier);
                    }
                }

                player.getEnderChest().clear();
                player.getInventory().clear();
                CuriosBridge.clearPlayerCuriosInventory(player);
                player.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get() + .5d, GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get() + .5d));
                player.setGameMode(GameMode.SURVIVAL);

                ProtectYourCastleMain.voiceChatBridge.joinGlobalGroup(player);

                if(this.gameStatistics != null) {
                    GameStatsInventory inv = new GameStatsInventory(player, this.gameStatistics);
                    inv.openInventory();
                }
            }

            for(Team team : Team.getTeams()){
                ProtectYourCastleMain.voiceChatBridge.deleteTeamGroup(team);
            }

            if(world != null) {
                Bukkit.getScheduler().runTaskLater(ProtectYourCastleMain.getInstance(), () -> Bukkit.unloadWorld(world, false), 10L);
            }

            this.scoreboard = null;
            this.gameStatus = Status.STOPPED;
            return this.gameStatistics;
        }
        return null;
    }

    public void stopMovementTraceRecorder() {
        if(this.movementTraceRecorder != null){
            this.movementTraceRecorder.stop();
            ProtectYourCastleMain.getInstance().getLogger().info("Movement trace saved: " + this.movementTraceRecorder.getOutputFile().getAbsolutePath());
            this.movementTraceRecorder = null;
        }
    }

    public GameStatistics getGameStatistics() {
        return gameStatistics;
    }

    public void finish() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamemode spectator @a");
        Team.TeamColor winner = this.getWinner();

        for(Player player : Bukkit.getOnlinePlayers()){
            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team == null) continue;

            if(team.getColor() == winner){
                player.sendTitle("§2Victoire !", "§aVotre équipe a gagné avec " + this.getPoints(team.getColor()) + " points");
            } else{
                player.sendTitle("§4Défaite...", "§cVotre équipe a perdu avec " + this.getPoints(team.getColor()) + " points");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, .5f, .6f);
        }

        Bukkit.broadcastMessage("§6[Castle]§f ------ §eRésumé§f ------");
        for(Team.TeamColor color : Team.TeamColor.values()) {
            Bukkit.broadcastMessage(color.getChatColor() + color.name() + "§e a obtenu §6" + this.getPoints(color) + " points !");
        }

        this.stop();
    }

    public void addPoint(Team.TeamColor teamColor, Player placer, int amount){
        this.points.put(teamColor, this.getPoints(teamColor) + amount);
        if(this.gameStatistics != null) {
            this.gameStatistics.addStatistic(placer.getUniqueId(), GameStatistics.StatisticKey.POINTS_WON, amount);
        }
        Bukkit.broadcastMessage("§6[Castle] §aL'équipe " + teamColor.getChatColor() + teamColor.name() + "§a vient de gagner " + amount + " point grâce à " + placer.getName() + " !");
    }

    public List<ResourceGenerator> getGenerators() {
        return ResourceGenerator.getResourceGenerators();
    }

    public boolean isRunning() {
        return this.gameStatus == Status.RUNNING;
    }

    public boolean pickupBanner(Player player){
        Team team = Team.getPlayerTeam(player.getUniqueId());
        if(team == null){
            return true;
        }
        if(bannerHolders.containsKey(team.getColor())){
            return false;
        }

        bannerHolders.put(team.getColor(), player.getUniqueId());
        return true;
    }

    public void placeBanner(Team team, Player player) {
        this.bannerHolders.remove(team.getColor());
        Game.currentGame.addPoint(team.getColor(), player, 1);
    }

    public int getPoints(Team.TeamColor color){
        return this.points.getOrDefault(color, 0);
    }

    public boolean hasWinner(){
        return this.getWinner() != null;
    }

    public Team.TeamColor getWinner() {
        int maxPoints = 0;
        Team.TeamColor winner = null;
        for(Team.TeamColor teamColor : Team.TeamColor.values()){
            int points = getPoints(teamColor);
            if(maxPoints < points) {
                maxPoints = points;
                winner = teamColor;
            } else if(maxPoints == points){
                winner = null;
            }
        }
        return winner;
    }

    public void spawnPlayer(Player player, Team team, boolean isFirstSpawn) {
        if(isFirstSpawn) {
            player.getInventory().clear();
            CuriosBridge.clearPlayerCuriosInventory(player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke " + player.getName() + " everything");
        }
        player.teleport(team.getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setSaturation(20);
        player.setFoodLevel(20);

        AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if(attributeInstance != null){
            for(AttributeModifier modifier : attributeInstance.getModifiers()){
                attributeInstance.removeModifier(modifier);
            }
            int teamSize = team.getMembers().size();
            int maxTeamSize = Team.getTeams().stream().map(t -> t.getMembers().size()).max(Comparator.comparingInt(value -> value)).orElse(teamSize);
            if(teamSize < maxTeamSize && GameParameters.ENABLE_BOOST_FOR_SMALL_TEAM.get()){
                attributeInstance.addModifier(new AttributeModifier("boost_for_smaller_team", GameParameters.HEALTH_BOOST_FOR_SMALL_TEAM.get(), AttributeModifier.Operation.ADD_NUMBER));
            }
        }

        AttributeInstance knockbackResistanceAttribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if(knockbackResistanceAttribute != null){
            knockbackResistanceAttribute.setBaseValue(GameParameters.BASE_KNOCKBACK_RESISTANCE.get());
        }

        if(player.getInventory().getHelmet() == null || player.getInventory().getHelmet().getType() == Material.AIR) player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        if(player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType() == Material.AIR) player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        if(player.getInventory().getLeggings() == null || player.getInventory().getLeggings().getType() == Material.AIR) player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        if(player.getInventory().getBoots() == null || player.getInventory().getBoots().getType() == Material.AIR) player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        player.getInventory().addItem(new ItemBuilder(Material.IRON_SWORD).addEnchant(Enchantment.DAMAGE_ALL, 20).build(), new ItemBuilder(Material.BREAD, 4).build());
    }

    public void addKill(@NotNull Player killer, boolean critical) {
        this.gameStatistics.increaseStatistic(killer.getUniqueId(), GameStatistics.StatisticKey.KILLS);
        if(critical){
            this.gameStatistics.increaseStatistic(killer.getUniqueId(), GameStatistics.StatisticKey.CRITICAL_KILLS);
        }
    }

    public void addBannerBroken(Player player) {
        this.gameStatistics.increaseStatistic(player.getUniqueId(), GameStatistics.StatisticKey.BANNERS_BROKEN);
    }

    public void addDeath(Player player){
        this.gameStatistics.increaseStatistic(player.getUniqueId(), GameStatistics.StatisticKey.DEATHS);
    }

    public void increaseDistanceInPlane(Player player, double distance) {
        this.gameStatistics.addStatistic(player.getUniqueId(), GameStatistics.StatisticKey.DISTANCE_WITH_PLANE, (int) (100 * distance));
    }

    public void increaseDistanceWalked(Player player, double distance) {
        this.gameStatistics.addStatistic(player.getUniqueId(), GameStatistics.StatisticKey.DISTANCE_WALKED, (int) (100 * distance));
    }

    public void increaseDamageDealt(UUID playerUUID, float damages) {
        this.gameStatistics.addStatistic(playerUUID, GameStatistics.StatisticKey.DAMAGE_DEALT, (int) (damages * 100));
    }

    public int getTime() {
        return time;
    }

    public Status getGameStatus() {
        return gameStatus;
    }

    public Map<Team.TeamColor, Integer> getPoints() {
        return points;
    }

    public Map<Team.TeamColor, UUID> getBannerHolders() {
        return bannerHolders;
    }

    public Player getBannerHolder(Team.TeamColor teamColor) {
        UUID uuid = bannerHolders.get(teamColor);
        if(uuid != null){
            return Bukkit.getPlayer(uuid);
        }
        return null;
    }

    public boolean isBannerHolder(Player player){
        return bannerHolders.containsValue(player.getUniqueId());
    }

    public List<Trader.GameWeapon> getSelectedWeapons() {
        return selectedWeapons;
    }

    public GameRunnable getRunnable() {
        return runnable;
    }

    public World getWorld() {
        return world;
    }

    public GameScoreboard getScoreboard() {
        return scoreboard;
    }

    public void increaseTime() {
        this.time++;
    }

    public enum Status{

        PREPARING,
        RUNNING,
        PAUSED,
        STOPPED;

    }
}
