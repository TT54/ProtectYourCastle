package fr.tt54.protectYourCastle.utils;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.UUID;

public class Area {

    private final World world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    public Area(World world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public Area(Location loc1, Location loc2){
        this.world = loc1.getWorld();
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    public boolean contains(Location location){
        return this.minX <= location.getBlockX() && location.getBlockX() <= this.maxX &&
                this.minZ <= location.getBlockZ() && location.getBlockZ() <= this.maxZ;
    }

    public World getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int[] getCornersArray() {
        return new int[] {minX, minZ, maxX, maxZ};
    }

    public static class AreaSerializer implements JsonSerializer<Area>{

        @Override
        public JsonElement serialize(Area area, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = new JsonObject();
            object.add("world", new JsonPrimitive(area.getWorld().getUID().toString()));
            object.add("minX", new JsonPrimitive(area.getMinX()));
            object.add("minZ", new JsonPrimitive(area.getMinZ()));
            object.add("maxX", new JsonPrimitive(area.getMaxX()));
            object.add("maxZ", new JsonPrimitive(area.getMaxZ()));
            return object;
        }
    }

    public static class AreaDeserializer implements JsonDeserializer<Area> {

        @Override
        public Area deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            World world = Bukkit.getWorld(UUID.fromString(object.get("world").getAsString()));
            int minX = object.get("minX").getAsInt();
            int minZ = object.get("minZ").getAsInt();
            int maxX = object.get("maxX").getAsInt();
            int maxZ = object.get("maxZ").getAsInt();
            return new Area(world, minX, minZ, maxX, maxZ);
        }
    }
}
