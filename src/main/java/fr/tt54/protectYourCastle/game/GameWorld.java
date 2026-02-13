package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class GameWorld {

    public static final Type generatorsType = new TypeToken<List<ResourceGenerator>>() {}.getType();
    public static final Type teamsType = new TypeToken<Map<Team.TeamColor, Team>>() {}.getType();
    public static final Type traderType = new TypeToken<Map<UUID, Trader>>() {}.getType();
    public static final Type weaponsType = new TypeToken<List<Trader.GameWeapon>>() {}.getType();


    private final String worldName;
    private final Map<Team.TeamColor, Team> loadedTeams = new HashMap<>();
    private final Map<UUID, Trader> traders = new HashMap<>();
    private final List<Trader.GameWeapon> weapons = new ArrayList<>();
    private final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    private World world;

    public GameWorld(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Charge le monde de jeu
     * @return true si le monde a correctement été chargé, false sinon
     */
    public boolean load(){
        this.loadTeams();
        this.loadTraders();
        this.loadResourceGenerators();
        this.world = this.loadWorld();
        return this.world != null;
    }

    public void saveData(){
        this.saveTeams();
        this.saveTraders();
        this.saveResourceGenerators();
    }

    public void unload(boolean save){
        this.unloadWorld(save);
        if(save) {
            this.saveData();
        }
    }

    private File loadFile(@NotNull String fileName){
        File teamsFile = FileManager.getFileWithoutCreating("worlds/" + this.worldName + "/" + fileName, ProtectYourCastleMain.getInstance());
        if (!teamsFile.exists()) {
            FileManager.saveResource("teams.json", "worlds/" + this.worldName + "/" + fileName, ProtectYourCastleMain.getInstance());
        }
        return teamsFile;
    }

    private void loadTeams(){
        this.loadedTeams.clear();
        this.loadedTeams.putAll(Game.gson.fromJson(FileManager.read(loadFile("teams.json")), teamsType));
        // TODO Trouver une solution pour bien conserver les joueurs d'une team à l'autre (quand on les recharge)
        // Le plus simple serait de ne simplement pas stocker les joueurs d'une team dans l'objet Team
    }

    private void loadTraders(){
        this.traders.clear();
        this.weapons.clear();

        this.traders.putAll(Game.gson.fromJson(FileManager.read(loadFile("traders.json")), traderType));
        this.weapons.addAll(Game.gson.fromJson(FileManager.read(loadFile("weapons.json")), weaponsType));
    }

    private void loadResourceGenerators(){
        this.resourceGenerators.clear();
        this.resourceGenerators.addAll(Game.gson.fromJson(FileManager.read(loadFile("generators.json")), generatorsType));
    }

    private void saveTeams(){
        FileManager.write(Game.gson.toJson(this.loadedTeams), loadFile("teams.json"));
    }

    private void saveTraders(){
        FileManager.write(Game.gson.toJson(this.traders), loadFile("traders.json"));
        FileManager.write(Game.gson.toJson(this.weapons), loadFile("weapons.json"));
    }

    private void saveResourceGenerators(){
        FileManager.write(Game.gson.toJson(this.resourceGenerators), loadFile("generators.json"));
    }

    private World loadWorld(){
        File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + this.worldName);
        File gameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder().getParentFile().getParentFile(), this.worldName);
        if(gameWorldFolder.exists()) {
            try (Stream<Path> paths = Files.walk(gameWorldFolder.toPath())) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(!sourceGameWorldFolder.exists() || !sourceGameWorldFolder.isDirectory()){
            return null;
        }

        WorldCreator creator = new WorldCreator(this.worldName);
        return creator.createWorld();
    }

    private void unloadWorld(boolean save) {
        if(this.world == null){
            System.err.println("Le monde " + this.worldName + " a tenté d'être déchargé alors qu'il n'était pas chargé");
            return;
        }

        Bukkit.unloadWorld(this.world, save);
        this.world = null;

        if(save) {
            File sourceGameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder(), "worlds/" + this.worldName);
            File gameWorldFolder = new File(ProtectYourCastleMain.getInstance().getDataFolder().getParentFile().getParentFile(), this.worldName);

            if(sourceGameWorldFolder.exists()) {
                try (Stream<Path> paths = Files.walk(sourceGameWorldFolder.toPath())) {
                    paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            FileManager.copy(gameWorldFolder, sourceGameWorldFolder);
        }
        System.out.println("Monde " + this.world.getName() + " déchargé !");
    }
}
