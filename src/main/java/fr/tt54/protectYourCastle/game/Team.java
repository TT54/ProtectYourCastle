package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.Location;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class Team {

    private static Map<TeamColor, Team> teams = new HashMap<>();
    private static final Map<UUID, TeamColor> playerTeam = new HashMap<>();

    private static final Type teamsType = new TypeToken<Map<TeamColor, Team>>() {}.getType();

    public static void load(){
        teams.clear();
        playerTeam.clear();

        File teamsFile = FileManager.getFileWithoutCreating("teams.json", ProtectYourCastleMain.getInstance());

        if (!teamsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("teams.json", false);
        }

        teams = Game.gson.fromJson(FileManager.read(teamsFile), teamsType);

        for(Team team : teams.values()){
            for(UUID player : team.members){
                playerTeam.put(player, team.color);
            }
        }

        for(TeamColor teamColor : TeamColor.values()){
            if(!teams.containsKey(teamColor)){
                teams.put(teamColor, new Team(teamColor, null, null, null, new HashSet<>()));
            }
        }
    }

    public static void save(){
        File teamsFile = FileManager.getFile("teams.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(teams), teamsFile);
    }

    public static Collection<Team> getTeams() {
        return teams.values();
    }

    public static Team getPlayerTeam(UUID player){
        return teams.get(playerTeam.get(player));
    }

    private final TeamColor color;
    private Location spawnLocation;
    private Location totemLocation;
    private Area base;
    private Set<UUID> members;

    public Team(TeamColor color, Location spawnLocation, Location totemLocation, Area base, Set<UUID> members) {
        this.color = color;
        this.spawnLocation = spawnLocation;
        this.totemLocation = totemLocation;
        this.base = base;
        this.members = members;
    }

    public TeamColor getColor() {
        return color;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getTotemLocation() {
        return totemLocation;
    }

    public void setTotemLocation(Location totemLocation) {
        this.totemLocation = totemLocation;
    }

    public Area getBase() {
        return base;
    }

    public void setBase(Area base) {
        this.base = base;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void joinTeam(UUID player){
        if(!playerTeam.containsKey(player)) {
            this.members.add(player);
            playerTeam.put(player, this.color);
        }
    }

    public void leaveTeam(UUID player){
        if(this.members.remove(player)){
            playerTeam.remove(player);
        }
    }

    public enum TeamColor{
        RED,
        YELLOW;
    }

}
