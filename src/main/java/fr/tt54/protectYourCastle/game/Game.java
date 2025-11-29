package fr.tt54.protectYourCastle.game;

import com.google.gson.*;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.runnable.GameRunnable;
import fr.tt54.protectYourCastle.utils.Area;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

public class Game {

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(Location.class, new LocationSerializer())
            .registerTypeAdapter(Location.class, new LocationDeserializer())
            .registerTypeAdapter(Area.class, new Area.AreaSerializer())
            .registerTypeAdapter(Area.class, new Area.AreaDeserializer())
            .create();

    public static Game currentGame;

    private Status gameStatus;
    private transient GameRunnable runnable;

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

            this.gameStatus = Status.STOPPED;
        }
    }

    public List<ResourceGenerator> getGenerators() {
        return ResourceGenerator.getResourceGenerators();
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
}
