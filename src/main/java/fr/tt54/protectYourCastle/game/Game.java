package fr.tt54.protectYourCastle.game;

import com.google.gson.*;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.runnable.GameRunnable;
import fr.tt54.protectYourCastle.scoreboard.GameScoreboard;
import fr.tt54.protectYourCastle.scoreboard.ScoreboardManager;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public static final int GAME_DURATION = 60 * 30;

    public static Game currentGame;

    private Status gameStatus;
    public int time;
    public Map<Team.TeamColor, Integer> points = new HashMap<>();
    public Map<Team.TeamColor, UUID> bannerHolder = new HashMap<>();

    private transient GameRunnable runnable;
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

        for(Team.TeamColor color : Team.TeamColor.values()){
            this.points.put(color, 0);
        }

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

            for(Player player : Bukkit.getOnlinePlayers()){
                ScoreboardManager.showScoreboard(player, scoreboard);
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
        // TODO annoncer le gagnant etc.
        this.stop();
    }

    public void addPoint(Team.TeamColor teamColor, Player placer, int amount){
        this.points.put(teamColor, this.points.getOrDefault(teamColor, 0) + 1);
        Bukkit.broadcastMessage("§aL'équipe " + teamColor.getChatColor() + teamColor.name() + "§a vient de gagner " + amount + " point grâce à " + placer.getName() + " !");
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
            player.sendMessage("§aVous avez ramené votre bannière chez vous");
        }
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
