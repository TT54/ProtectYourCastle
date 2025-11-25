package fr.tt54.protectYourCastle;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ResourceGenerator {

    private Material material;
    private long cooldown; // En secondes
    private long timeBeforeNextDrop;
    private Location location;

    public ResourceGenerator(Material material, long cooldown, long timeBeforeNextDrop, Location location) {
        this.material = material;
        this.cooldown = cooldown;
        this.timeBeforeNextDrop = timeBeforeNextDrop;
        this.location = location;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public long getTimeBeforeNextDrop() {
        return timeBeforeNextDrop;
    }

    public void setTimeBeforeNextDrop(long timeBeforeNextDrop) {
        this.timeBeforeNextDrop = timeBeforeNextDrop;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void generate(){
        this.timeBeforeNextDrop--;
        if(this.timeBeforeNextDrop == 0){
            this.timeBeforeNextDrop = this.cooldown;
            this.getLocation().getWorld().dropItem(this.getLocation(), new ItemStack(this.material));
        }
    }
}
