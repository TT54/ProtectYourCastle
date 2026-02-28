package fr.tt54.protectYourCastle.utils;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

public class SerializerUtils {

    private static World resolveWorld(String worldValue){
        if(worldValue == null || worldValue.isBlank()) {
            return null;
        }

        try {
            return Bukkit.getWorld(UUID.fromString(worldValue));
        } catch (IllegalArgumentException ignored){
            return Bukkit.getWorld(worldValue);
        }
    }

    private static World getDefaultWorld(){
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private static double getDoubleOrDefault(JsonObject object, String key, double defaultValue){
        return object.has(key) ? object.get(key).getAsDouble() : defaultValue;
    }

    private static float getFloatOrDefault(JsonObject object, String key, float defaultValue){
        return object.has(key) ? object.get(key).getAsFloat() : defaultValue;
    }

    public static class LocationSerializer implements JsonSerializer<Location> {

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
            if(!jsonElement.isJsonObject()) {
                return null;
            }

            JsonObject object = jsonElement.getAsJsonObject();
            World world = null;

            if(object.has("world")) {
                world = resolveWorld(object.get("world").getAsString());
            }
            if(world == null && object.has("worldName")) {
                world = resolveWorld(object.get("worldName").getAsString());
            }
            if(world == null) {
                world = getDefaultWorld();
            }

            double x = getDoubleOrDefault(object, "x", 0d);
            double y = getDoubleOrDefault(object, "y", 0d);
            double z = getDoubleOrDefault(object, "z", 0d);
            float yaw = getFloatOrDefault(object, "yaw", 0f);
            float pitch = getFloatOrDefault(object, "pitch", 0f);
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
            if(value.isEmpty()) return null;
            try{
                return ItemSerialization.deserialize(value);
            }catch (Throwable ignored){
                return null;
            }
        }
    }

    public static class SavedLocationSerializer implements JsonSerializer<SavedLocation> {

        @Override
        public JsonElement serialize(SavedLocation savedLocation, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = new JsonObject();
            if(savedLocation.world() != null){
                object.add("world", new JsonPrimitive(savedLocation.world().toString()));
            } else {
                object.add("world", JsonNull.INSTANCE);
            }
            object.add("x", new JsonPrimitive(savedLocation.x()));
            object.add("y", new JsonPrimitive(savedLocation.y()));
            object.add("z", new JsonPrimitive(savedLocation.z()));
            object.add("yaw", new JsonPrimitive(savedLocation.yaw()));
            object.add("pitch", new JsonPrimitive(savedLocation.pitch()));
            return object;
        }
    }

    public static class SavedLocationDeserializer implements JsonDeserializer<SavedLocation> {

        @Override
        public SavedLocation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if(!jsonElement.isJsonObject()){
                return null;
            }

            JsonObject object = jsonElement.getAsJsonObject();
            World world = null;
            if(object.has("world") && !object.get("world").isJsonNull()){
                world = resolveWorld(object.get("world").getAsString());
            }
            if(world == null && object.has("worldName")){
                world = resolveWorld(object.get("worldName").getAsString());
            }

            UUID worldUUID = world == null ? null : world.getUID();
            if(worldUUID == null && object.has("world") && !object.get("world").isJsonNull()){
                try {
                    worldUUID = UUID.fromString(object.get("world").getAsString());
                } catch (IllegalArgumentException ignored){}
            }

            double x = getDoubleOrDefault(object, "x", 0d);
            double y = getDoubleOrDefault(object, "y", 0d);
            double z = getDoubleOrDefault(object, "z", 0d);
            float yaw = getFloatOrDefault(object, "yaw", 0f);
            float pitch = getFloatOrDefault(object, "pitch", 0f);

            return new SavedLocation(worldUUID, x, y, z, yaw, pitch);
        }
    }
}
