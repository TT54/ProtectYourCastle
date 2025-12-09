package fr.tt54.protectYourCastle.utils;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.UUID;

public class Area {

    private final UUID world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Area(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world.getUID();
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public Area(UUID world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public Area(World world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world.getUID();
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.minY = Integer.MIN_VALUE;
        this.maxY = Integer.MAX_VALUE;
    }

    public Area(Location loc1, Location loc2, boolean useY){
        this.world = loc1.getWorld().getUID();
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        this.minY = useY ? Math.min(loc1.getBlockY(), loc2.getBlockY()) : Integer.MIN_VALUE;
        this.maxY = useY ? Math.max(loc1.getBlockY(), loc2.getBlockY()) : Integer.MAX_VALUE;
    }

    public boolean contains(Location location){
        return this.minX <= location.getBlockX() && location.getBlockX() <= this.maxX &&
                this.minY <= location.getBlockY() && location.getBlockY() <= this.maxY &&
                this.minZ <= location.getBlockZ() && location.getBlockZ() <= this.maxZ;
    }

    public World getWorld() {
        return Bukkit.getWorld(world);
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

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int[] getCornersArray() {
        return new int[] {minX, minZ, maxX, maxZ};
    }

    public static class AreaSerializer implements JsonSerializer<Area>{

        @Override
        public JsonElement serialize(Area area, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = new JsonObject();
            object.add("world", new JsonPrimitive(area.world.toString()));
            object.add("minX", new JsonPrimitive(area.getMinX()));
            object.add("minY", new JsonPrimitive(area.getMinY()));
            object.add("minZ", new JsonPrimitive(area.getMinZ()));
            object.add("maxX", new JsonPrimitive(area.getMaxX()));
            object.add("maxY", new JsonPrimitive(area.getMaxY()));
            object.add("maxZ", new JsonPrimitive(area.getMaxZ()));
            return object;
        }
    }

    public static class AreaDeserializer implements JsonDeserializer<Area> {

        @Override
        public Area deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            UUID world = UUID.fromString(object.get("world").getAsString());
            int minX = object.get("minX").getAsInt();
            int minY = !object.has("minY") ? -1000 : object.get("minY").getAsInt();
            int minZ = object.get("minZ").getAsInt();
            int maxX = object.get("maxX").getAsInt();
            int maxY = !object.has("maxY") ? 1000 : object.get("maxY").getAsInt();
            int maxZ = object.get("maxZ").getAsInt();
            return new Area(world, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
