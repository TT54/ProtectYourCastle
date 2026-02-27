package fr.tt54.protectYourCastle.game;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

public final class MovementModeClassifier {

    private static final Set<String> FLYING_NAMESPACES = Set.of(
            "immersive_aircraft",
            "man_of_many_planes",
            "manofmanyplanes",
            "man_of_plane"
    );

    private MovementModeClassifier() {
    }

    public static MovementMode classify(Player player) {
        if(player == null) {
            return MovementMode.FOOT;
        }

        if(player.isGliding()) {
            return MovementMode.GLIDING;
        }

        Entity vehicle = player.getVehicle();
        if(vehicle == null) {
            return MovementMode.FOOT;
        }

        String vehicleType = vehicle.getType().name().toLowerCase(Locale.ROOT);
        String keyNamespace = getKeyNamespace(vehicle);
        String keyPath = getKeyPath(vehicle);
        String className = vehicle.getClass().getName().toLowerCase(Locale.ROOT);

        if(isCreateTrain(vehicleType, keyNamespace, keyPath, className)) {
            return MovementMode.TRAIN;
        }

        if(isBoat(vehicleType, keyPath)) {
            return MovementMode.BOAT;
        }

        if(isMountType(vehicleType)) {
            return MovementMode.MOUNT;
        }

        if(isFlyingVehicle(vehicle, player, vehicleType, keyNamespace, keyPath, className)) {
            return MovementMode.FLYING_VEHICLE;
        }

        return MovementMode.OTHER_VEHICLE;
    }

    private static boolean isCreateTrain(String vehicleType, String keyNamespace, String keyPath, String className) {
        if(vehicleType.contains("minecart")) {
            return true;
        }

        if(keyNamespace.contains("create")
                && (keyPath.contains("train")
                || keyPath.contains("carriage")
                || keyPath.contains("bogey")
                || keyPath.contains("contraption"))) {
            return true;
        }

        if(className.contains("simibubi.create")
                && (className.contains("train")
                || className.contains("carriage")
                || className.contains("bogey")
                || className.contains("contraption"))) {
            return true;
        }

        return className.contains("create") && className.contains("carriage");
    }

    private static boolean isBoat(String vehicleType, String keyPath) {
        return vehicleType.contains("boat") || keyPath.contains("boat");
    }

    private static boolean isMountType(String vehicleType) {
        return vehicleType.contains("horse")
                || vehicleType.contains("llama")
                || vehicleType.contains("camel")
                || vehicleType.contains("donkey")
                || vehicleType.contains("mule")
                || vehicleType.equals("pig")
                || vehicleType.equals("strider")
                || vehicleType.equals("ravager");
    }

    private static boolean isFlyingVehicle(Entity vehicle, Player player, String vehicleType, String keyNamespace, String keyPath, String className) {
        if(FLYING_NAMESPACES.contains(keyNamespace)) {
            return true;
        }

        if(keyPath.contains("air")
                || keyPath.contains("plane")
                || keyPath.contains("aircraft")
                || keyPath.contains("heli")
                || keyPath.contains("drone")
                || keyPath.contains("jet")
                || keyPath.contains("gyro")
                || keyPath.contains("quadcopter")
                || keyPath.contains("airship")) {
            return true;
        }

        if(className.contains("immersive_aircraft")
                || className.contains("manofmanyplanes")
                || className.contains("man_of_many_planes")
                || className.contains("aircraft")
                || className.contains("helicopter")
                || className.contains("quadcopter")) {
            return true;
        }

        if(player.isFlying()) {
            return true;
        }

        if(!vehicle.isOnGround()) {
            return vehicleType.contains("air")
                    || vehicleType.contains("plane")
                    || vehicleType.contains("aircraft")
                    || vehicleType.contains("heli")
                    || vehicleType.contains("drone")
                    || vehicleType.contains("jet")
                    || vehicleType.contains("gyro")
                    || vehicleType.contains("quadcopter")
                    || vehicleType.contains("airship");
        }

        return false;
    }

    private static String getKeyNamespace(Entity entity) {
        try {
            NamespacedKey key = entity.getType().getKey();
            if(key == null || key.getNamespace() == null) {
                return "";
            }
            return key.getNamespace().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String getKeyPath(Entity entity) {
        try {
            NamespacedKey key = entity.getType().getKey();
            if(key == null || key.getKey() == null) {
                return "";
            }
            return key.getKey().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    public enum MovementMode {
        FOOT,
        FLYING_VEHICLE,
        TRAIN,
        BOAT,
        MOUNT,
        GLIDING,
        OTHER_VEHICLE
    }
}
