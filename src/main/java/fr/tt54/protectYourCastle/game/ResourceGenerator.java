package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.SavedLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
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

        List<ResourceGenerator> loadedGenerators = Game.gson.fromJson(FileManager.read(generatorsFile), generatorsType);
        resourceGenerators = loadedGenerators != null ? loadedGenerators : new ArrayList<>();
    }

    public static void save(){
        File generatorsFile = FileManager.getFile("generators.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(resourceGenerators), generatorsFile);
    }

    private Material material;
    private long cooldown; // En secondes
    private long timeBeforeNextDrop;
    private SavedLocation location;

    public ResourceGenerator(Material material, long cooldown, long timeBeforeNextDrop, SavedLocation location) {
        this.material = material;
        this.cooldown = cooldown;
        this.timeBeforeNextDrop = timeBeforeNextDrop;
        this.location = location;
    }

    public static void spawnResourceGenerator(Material material, long cooldown, long timeBeforeNextDrop, Location location) {
        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(location.clone().add(0, 1.5, 0), EntityType.TEXT_DISPLAY);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(true);
        textDisplay.setText("§eGénérateur de " + material.name().toLowerCase() + "\n" + "§aProchain spawn dans : §b" + timeBeforeNextDrop + "s");

        ResourceGenerator generator = new ResourceGenerator(material, cooldown, timeBeforeNextDrop, SavedLocation.fromLocation(location));
        addGenerator(generator);
    }

    public static boolean removeResourceGenerator(ResourceGenerator generator) {
        Entity textDisplay = generator.getLocation().getWorld().getNearbyEntities(
                generator.getLocation().add(0, 1.5, 0),
                .1, .1, .1, e -> e instanceof TextDisplay).stream().findFirst().orElse(null);
        if(textDisplay != null) {
            textDisplay.remove();
        }
        return resourceGenerators.remove(generator);
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
        return location.toLocation();
    }

    public void setLocation(Location location) {
        this.location = SavedLocation.fromLocation(location);
    }

    public void generate(int amount){
        if(this.getLocation().getWorld() == null) return;
        this.timeBeforeNextDrop--;
        if(this.timeBeforeNextDrop == 0){
            this.timeBeforeNextDrop = this.cooldown;
            this.getLocation().getWorld().dropItem(this.getLocation(), new ItemStack(this.material, amount));
        }
        this.updateName();
    }

    public void updateName(){
        if(this.getLocation().getWorld() == null) return;
        for(TextDisplay display : this.getLocation().getWorld().getNearbyEntities(this.getLocation().add(0, 1.5, 0), .1, .1, .1, e -> e instanceof TextDisplay).stream().map(e -> (TextDisplay) e).toList()){
            display.setText("§eGénérateur de " + material.name().toLowerCase() + "\n" + "§aProchain spawn dans : §b" + timeBeforeNextDrop + "s");
        }
    }
}
