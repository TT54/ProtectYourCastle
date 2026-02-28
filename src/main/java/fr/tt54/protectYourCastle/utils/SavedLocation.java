package fr.tt54.protectYourCastle.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record SavedLocation(UUID world, double x, double y, double z, float yaw, float pitch) {

    private SavedLocation(Location location) {
        this(location.getWorld() == null ? null : location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static SavedLocation fromLocation(Location location){
        return location == null ? null : new SavedLocation(location);
    }

    public Location toLocation() {
        World targetWorld = this.world == null ? null : Bukkit.getWorld(this.world);
        if(targetWorld == null && !Bukkit.getWorlds().isEmpty()){
            targetWorld = Bukkit.getWorlds().get(0);
        }
        return new Location(targetWorld, this.x, this.y, this.z, this.yaw, this.pitch);
    }
}
