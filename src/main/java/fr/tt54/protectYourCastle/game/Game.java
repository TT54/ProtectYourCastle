package fr.tt54.protectYourCastle.game;

import com.google.gson.*;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.runnable.GameRunnable;
import fr.tt54.protectYourCastle.scoreboard.GameScoreboard;
import fr.tt54.protectYourCastle.scoreboard.ScoreboardManager;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import fr.tt54.protectYourCastle.utils.ItemSerialization;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Game {

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackDeserializer())
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackDeserializer())
            .registerTypeAdapter(Location.class, new LocationSerializer())
            .registerTypeAdapter(Location.class, new LocationDeserializer())
            .registerTypeAdapter(Area.class, new Area.AreaSerializer())
            .registerTypeAdapter(Area.class, new Area.AreaDeserializer())
            .registerTypeAdapter(GameParameters.class, new GameParameters.GameParametersJsonSerializer())
            .registerTypeAdapter(GameParameters.class, new GameParameters.GameParametersJsonDeserializer())
            .create();

    public static Game currentGame;

    private Status gameStatus;
    public int time;
    public Map<Team.TeamColor, Integer> points = new HashMap<>();
    public Map<Team.TeamColor, UUID> bannerHolder = new HashMap<>();
    public Map<UUID, Integer> kills = new HashMap<>();
    public Map<UUID, Integer> deaths = new HashMap<>();
    public Map<UUID, Integer> pointsPerPlayer = new HashMap<>();
    public Map<UUID, Integer> bannerBrokenPerPlayer = new HashMap<>();

    private transient GameRunnable runnable;
    public transient World gameWorld;
    public transient GameScoreboard scoreboard;

    public Game() {
    }

    public static boolean createNew() {
        if(currentGame == null || currentGame.gameStatus == Status.STOPPED){
            currentGame = new Game();
            return true;
        }
        return false;
    }

    public void prepare(){
        this.gameStatus = Status.PREPARING;

        for(Team team : Team.getTeams()){
            this.points.put(team.getColor(), 0);
            ProtectYourCastleMain.voiceChatBridge.createTeamGroup(team);
        }

        File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "game_world");
        File gameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder().getParentFile().getParentFile(), "game_world");
        if(gameWorldFolder.exists()) {
            try (Stream<Path> paths = Files.walk(gameWorldFolder.toPath())) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileManager.copy(sourceGameWorldFolder, gameWorldFolder);

        WorldCreator creator = new WorldCreator("game_world");
        this.gameWorld = creator.createWorld();

        for(Player player : Bukkit.getOnlinePlayers()){
            // TODO Ouvrir le menu de sélection d'équipe
        }
    }

    public void launch(){
        if(gameStatus == Status.PREPARING){
            this.runnable = new GameRunnable(this);
            this.runnable.runTaskTimer(ProtectYourCastleMain.getInstance(), 20, 20);

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(true);
            }

            scoreboard = new GameScoreboard();

            World world = this.gameWorld;
            world.setTime(6000);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setThundering(false);
            world.setStorm(false);
            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(0, 0);
            worldBorder.setSize(GameParameters.MAP_RADIUS.get());

            for(Player player : Bukkit.getOnlinePlayers()){
                ScoreboardManager.showScoreboard(player, scoreboard);
                Team team = Team.getPlayerTeam(player.getUniqueId());
                if(team != null) {
                    spawnPlayer(player, team, true);
                }
            }

            this.gameStatus = Status.RUNNING;
        }
    }

    public GameStatistics stop(){
        if(this.runnable != null && gameStatus != Status.STOPPED) {
            this.runnable.cancel();
            this.runnable = null;

            GameStatistics statistics = this.getStatistics();
            GameStatistics.gameStatistics.add(statistics);

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(false);
            }

            for(Player player : Bukkit.getOnlinePlayers()){
                ScoreboardManager.removeScoreboard(player);

                player.getInventory().clear();
                // TODO Clear Curios inventory
                player.teleport(new Location(Bukkit.getWorlds().get(0), GameParameters.LOBBY_X.get() + .5d, GameParameters.LOBBY_Y.get(), GameParameters.LOBBY_Z.get() + .5d));
                player.setGameMode(GameMode.SURVIVAL);
            }

            Bukkit.unloadWorld(gameWorld, false);

            this.scoreboard = null;
            this.gameStatus = Status.STOPPED;
            return statistics;
        }
        return null;
    }

    public GameStatistics getStatistics(){
        Map<UUID, Team.TeamColor> playersTeam = new HashMap<>();
        Map<Team.TeamColor, Integer> scores = new HashMap<>();
        for(Team team : Team.getTeams()){
            for(UUID player : team.getMembers()){
                playersTeam.put(player, team.getColor());
            }
            scores.put(team.getColor(), this.getPoints(team.getColor()));
        }

        return new GameStatistics(playersTeam, scores, new HashMap<>(this.kills), new HashMap<>(this.deaths), new HashMap<>(this.pointsPerPlayer), new HashMap<>(this.bannerBrokenPerPlayer));
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
        this.points.put(teamColor, this.getPoints(teamColor) + 1);
        this.pointsPerPlayer.put(placer.getUniqueId(), this.getPlayerPointsCollected(placer.getUniqueId()) + 1);
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
        if(bannerHolder.containsKey(team.getColor())){
            return false;
        }

        bannerHolder.put(team.getColor(), player.getUniqueId());
        return true;
    }

    public void placeBanner(Team team, Player player, ItemStack is) {
        this.bannerHolder.remove(team.getColor());
        if(Team.getBannerOwner(is) != team.getColor()) {
            Game.currentGame.addPoint(team.getColor(), player, 1);
        } else{
            player.sendMessage("§6[Castle] §aVous avez ramené votre bannière chez vous");
        }
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
            // TODO Clear curios inventory
        }
        player.teleport(team.getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setSaturation(20);
        player.setFoodLevel(20);

        if(player.getInventory().getHelmet() != null) player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        if(player.getInventory().getChestplate() != null) player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        if(player.getInventory().getLeggings() != null) player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        if(player.getInventory().getBoots() != null) player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        player.getInventory().addItem(new ItemBuilder(Material.IRON_SWORD).build(), new ItemBuilder(Material.BREAD, 4).build());
    }

    public static ItemStack colorArmor(ItemStack armor, Color color){
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }

    public void addKill(@NotNull Player killer) {
        this.kills.put(killer.getUniqueId(), 1 + this.getPlayerKills(killer.getUniqueId()));
    }

    public int getPlayerKills(UUID playerUUID){
        return this.kills.getOrDefault(playerUUID, 0);
    }

    public int getPlayerPointsCollected(UUID playerUUID){
        return this.pointsPerPlayer.getOrDefault(playerUUID, 0);
    }

    public int getBannerBroken(UUID playerUUID){
        return this.bannerBrokenPerPlayer.getOrDefault(playerUUID, 0);
    }

    public void addBannerBroken(Player player) {
        this.bannerBrokenPerPlayer.put(player.getUniqueId(), 1 + this.getBannerBroken(player.getUniqueId()));
    }

    public int getDeaths(UUID playerUUID){
        return this.deaths.getOrDefault(playerUUID, 0);
    }

    public void addDeath(Player player){
        this.deaths.put(player.getUniqueId(), this.getDeaths(player.getUniqueId()));
    }

    public enum Status{

        PREPARING,
        RUNNING,
        PAUSED,
        STOPPED;

    }

    public static class LocationSerializer implements JsonSerializer<Location>{

        @Override
        public JsonElement serialize(Location location, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = new JsonObject();
            object.add("world", new JsonPrimitive(location.getWorld().getUID().toString()));
            object.add("x", new JsonPrimitive(location.getX()));
            object.add("y", new JsonPrimitive(location.getY()));
            object.add("z", new JsonPrimitive(location.getZ()));
            object.add("yaw", new JsonPrimitive(location.getYaw()));
            object.add("pitch", new JsonPrimitive(location.getPitch()));
            return object;
        }
    }

    public static class LocationDeserializer implements JsonDeserializer<Location> {

        @Override
        public Location deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            World world = Bukkit.getWorld(UUID.fromString(object.get("world").getAsString()));
            double x = object.get("x").getAsDouble();
            double y = object.get("y").getAsDouble();
            double z = object.get("z").getAsDouble();
            float yaw = object.get("yaw").getAsFloat();
            float pitch = object.get("pitch").getAsFloat();
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    public static class ItemStackSerializer implements JsonSerializer<ItemStack>{

        @Override
        public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext jsonSerializationContext) {
            String value = ItemSerialization.serialize(itemStack);
            return new JsonPrimitive(value == null ? "" : value);
        }
    }

    public static class ItemStackDeserializer implements JsonDeserializer<ItemStack>{

        @Override
        public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String value = jsonElement.getAsString();
            return value.isEmpty() ? null : ItemSerialization.deserialize(value);
        }
    }
}
