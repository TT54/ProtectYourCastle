package fr.tt54.protectYourCastle.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public record SavedLocation(UUID world, double x, double y, double z, float yaw, float pitch) {

    private SavedLocation(Location location) {
        this(location.getWorld() == null ? null : location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static SavedLocation fromLocation(Location location){
        return location == null ? null : new SavedLocation(location);
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z, this.yaw, this.pitch);
    }
}
