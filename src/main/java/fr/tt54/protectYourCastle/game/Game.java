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
import org.bukkit.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            .create();
    public static int GAME_DURATION = 60 * 60;
    public static final int RESPAWN_DELAY = 20;

    public static Game currentGame;

    private Status gameStatus;
    public int time;
    public Map<Team.TeamColor, Integer> points = new HashMap<>();
    public Map<Team.TeamColor, UUID> bannerHolder = new HashMap<>();

    private transient GameRunnable runnable;
    private transient World gameWorld;
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
        gameWorld = creator.createWorld();

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

            World world = Bukkit.getWorlds().get(0);
            world.setTime(0);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setThundering(false);
            world.setStorm(false);

            for(Player player : Bukkit.getOnlinePlayers()){
                ScoreboardManager.showScoreboard(player, scoreboard);
                Team team = Team.getPlayerTeam(player.getUniqueId());
                if(team != null) {
                    spawnPlayer(player, team);
                }
            }

            this.gameStatus = Status.RUNNING;
        }
    }

    public void stop(){
        if(this.runnable != null && gameStatus != Status.STOPPED) {
            this.runnable.cancel();
            this.runnable = null;

            for(ResourceGenerator generator : this.getGenerators()){
                generator.getLocation().getChunk().setForceLoaded(false);
            }

            for(Player player : Bukkit.getOnlinePlayers()){
                ScoreboardManager.removeScoreboard(player);
            }

            this.scoreboard = null;
            this.gameStatus = Status.STOPPED;
        }
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
        this.points.put(teamColor, this.points.getOrDefault(teamColor, 0) + 1);
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

    public void spawnPlayer(Player player, Team team) {
        player.getInventory().clear();
        player.teleport(team.getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setSaturation(20);
        player.setFoodLevel(20);

        ItemStack helmet = colorArmor(new ItemStack(Material.LEATHER_HELMET), team.getColor().getArmorColor());
        ItemStack chestplate = colorArmor(new ItemStack(Material.LEATHER_CHESTPLATE), team.getColor().getArmorColor());
        ItemStack leggings = colorArmor(new ItemStack(Material.LEATHER_LEGGINGS), team.getColor().getArmorColor());
        ItemStack boots = colorArmor(new ItemStack(Material.LEATHER_BOOTS), team.getColor().getArmorColor());

        player.getInventory().setArmorContents(new ItemStack[]{boots, leggings, chestplate, helmet});

        player.getInventory().addItem(new ItemBuilder(Material.STONE_SWORD).build(), new ItemBuilder(Material.BREAD, 4).build());
    }

    public static ItemStack colorArmor(ItemStack armor, Color color){
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
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
