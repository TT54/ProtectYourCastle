package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class GameWorld {

    public static final Type generatorsType = new TypeToken<List<ResourceGenerator>>() {}.getType();
    public static final Type teamsType = new TypeToken<Map<Team.TeamColor, Team>>() {}.getType();
    public static final Type traderType = new TypeToken<Map<UUID, Trader>>() {}.getType();
    public static final Type weaponsType = new TypeToken<List<Trader.GameWeapon>>() {}.getType();


    private final String name;
    private final Map<Team.TeamColor, Team> loadedTeams = new HashMap<>();
    private final Map<UUID, Trader> traders = new HashMap<>();
    private final List<Trader.GameWeapon> weapons = new ArrayList<>();
    private final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    private World world;

    public GameWorld(String name) {
        this.name = name;
    }

    public void load(){
        this.loadTeams();
        this.loadTraders();
        this.loadResourceGenerators();
    }

    public void save(){
        this.saveTeams();
        this.saveTraders();
        this.saveResourceGenerators();
    }

    private File loadFile(@NotNull String fileName){
        File teamsFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/" + fileName, ProtectYourCastleMain.getInstance());
        if (!teamsFile.exists()) {
            FileManager.saveResource("teams.json", "worlds/" + this.name + "/" + fileName, ProtectYourCastleMain.getInstance());
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
}
