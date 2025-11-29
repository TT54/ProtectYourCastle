package fr.tt54.protectYourCastle;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.game.Game;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResourceGenerator {

    private static List<ResourceGenerator> resourceGenerators = new ArrayList<>();

    private static final Type generatorsType = new TypeToken<List<ResourceGenerator>>() {}.getType();


    public static void addGenerator(ResourceGenerator generator){
        resourceGenerators.add(generator);
    }

    public static List<ResourceGenerator> getResourceGenerators() {
        return resourceGenerators;
    }

    public static void load(){
        resourceGenerators.clear();

        File generatorsFile = FileManager.getFileWithoutCreating("generators.json", ProtectYourCastleMain.getInstance());

        if (!generatorsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("generators.json", false);
        }

        resourceGenerators = Game.gson.fromJson(FileManager.read(generatorsFile), generatorsType);
    }

    public static void save(){
        File generatorsFile = FileManager.getFile("generators.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(resourceGenerators), generatorsFile);
    }

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
