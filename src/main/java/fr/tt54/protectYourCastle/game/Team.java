package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class Team {

    private static Map<TeamColor, Team> teams = new HashMap<>();
    private static final Map<UUID, TeamColor> playerTeam = new HashMap<>();
    private static final NamespacedKey BANNER_KEY = new NamespacedKey("castle", "banner");

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

    public static Team getTeam(TeamColor teamColor) {
        return teams.get(teamColor);
    }

    private final TeamColor color;
    private Location spawnLocation;
    private Location bannerLocation;
    private Area base;
    private final Set<UUID> members;

    public Team(TeamColor color, Location spawnLocation, Location bannerLocation, Area base, Set<UUID> members) {
        this.color = color;
        this.spawnLocation = spawnLocation;
        this.bannerLocation = bannerLocation;
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

    public Location getBannerLocation() {
        return bannerLocation;
    }

    public void setBannerLocation(Location bannerLocation) {
        this.bannerLocation = bannerLocation;
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
        Team team = getPlayerTeam(player);
        if(team != null) {
            team.leaveTeam(player);
        }

        if(this.members.add(player)) {
            playerTeam.put(player, this.color);
        }

        Player p = Bukkit.getPlayer(player);
        if(p != null){
            p.setPlayerListName(this.color.chatColor + "[" + this.color.name() + "] " + p.getName());
        }
    }

    public void leaveTeam(UUID player){
        if(this.members.remove(player)){
            playerTeam.remove(player);
        }

        Player p = Bukkit.getPlayer(player);
        if(p != null){
            p.setPlayerListName(p.getName());
        }
    }

    public ItemStack getBannerItem() {
        ItemStack is = new ItemBuilder(this.color.banner, this.color.chatColor + "Bannière de l'équipe " + this.color.name()).build();

        ItemMeta meta = is.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        dataContainer.set(BANNER_KEY, PersistentDataType.STRING, this.color.name());
        is.setItemMeta(meta);

        return is;
    }

    public static boolean isBannerItem(ItemStack is){
        if(is == null || is.getItemMeta() == null || is.getItemMeta().getPersistentDataContainer() == null) return false;
        return is.getItemMeta().getPersistentDataContainer().has(BANNER_KEY, PersistentDataType.STRING);
    }

    public static TeamColor getBannerOwner(ItemStack is){
        if(is == null || is.getItemMeta() == null || is.getItemMeta().getPersistentDataContainer() == null) return null;

        PersistentDataContainer dataContainer = is.getItemMeta().getPersistentDataContainer();
        if(dataContainer.has(BANNER_KEY, PersistentDataType.STRING)){
            try {
                return TeamColor.valueOf(dataContainer.get(BANNER_KEY, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignore){}
        }
        return null;
    }

    public enum TeamColor{
        RED(Material.RED_BANNER, "§4", Color.RED),
        YELLOW(Material.YELLOW_BANNER, "§6", Color.YELLOW);

        private final Material banner;
        private final String chatColor;
        private final Color color;

        TeamColor(Material banner, String chatColor, Color color) {
            this.banner = banner;
            this.chatColor = chatColor;
            this.color = color;
        }

        public Material getBanner() {
            return banner;
        }

        public String getChatColor() {
            return chatColor;
        }

        public Color getArmorColor() {
            return color;
        }
    }

}
