package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.World;

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

    private void loadTeams(){
        this.loadedTeams.clear();

        File teamsFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/teams.json", ProtectYourCastleMain.getInstance());
        if (!teamsFile.exists()) {
            FileManager.saveResource("teams.json", "worlds/" + this.name + "teams.json", ProtectYourCastleMain.getInstance());
        }
        this.loadedTeams.putAll(Game.gson.fromJson(FileManager.read(teamsFile), teamsType));
        // TODO Trouver une solution pour bien conserver les joueurs d'une team à l'autre (quand on les recharge)
        // Le plus simple serait de ne simplement pas stocker les joueurs d'une team dans l'objet Team
    }

    private void loadTraders(){
        this.traders.clear();
        this.weapons.clear();

        File tradersFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/traders.json", ProtectYourCastleMain.getInstance());
        if (!tradersFile.exists()) {
            FileManager.saveResource("traders.json", "worlds/" + this.name + "traders.json", ProtectYourCastleMain.getInstance());
        }
        this.traders.putAll(Game.gson.fromJson(FileManager.read(tradersFile), traderType));

        File weaponsFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/weapons.json", ProtectYourCastleMain.getInstance());
        if (!weaponsFile.exists()) {
            FileManager.saveResource("weapons.json", "worlds/" + this.name + "weapons.json", ProtectYourCastleMain.getInstance());
        }
        this.weapons.addAll(Game.gson.fromJson(FileManager.read(weaponsFile), weaponsType));
    }

    private void loadResourceGenerators(){
        this.resourceGenerators.clear();

        File generatorsFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/generators.json", ProtectYourCastleMain.getInstance());
        if (!generatorsFile.exists()) {
            FileManager.saveResource("generators.json", "worlds/" + this.name + "generators.json", ProtectYourCastleMain.getInstance());
        }
        this.resourceGenerators.addAll(Game.gson.fromJson(FileManager.read(generatorsFile), generatorsType));
    }

    private void saveTeams(){
        File teamsFile = FileManager.getFile("worlds/" + this.name + "/teams.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(this.loadedTeams), teamsFile);
    }

    private void saveTraders(){
        File tradersFile = FileManager.getFile("worlds/" + this.name + "/traders.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(this.traders), tradersFile);

        File weaponsFile = FileManager.getFileWithoutCreating("worlds/" + this.name + "/weapons.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(this.weapons), weaponsFile);
    }

    private void saveResourceGenerators(){
        File generatorsFile = FileManager.getFile("worlds/" + this.name + "/generators.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(this.resourceGenerators), generatorsFile);
    }
}
