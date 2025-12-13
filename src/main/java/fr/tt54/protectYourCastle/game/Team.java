package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.Area;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import fr.tt54.protectYourCastle.utils.SavedLocation;
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
                teams.put(teamColor, new Team(teamColor, null, null, null, null, null, null, new HashSet<>()));
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

    public static Map<UUID, TeamColor> getPlayerTeamMapCopy(){
        return new HashMap<>(playerTeam);
    }

    public static Team getTeam(TeamColor teamColor) {
        return teams.get(teamColor);
    }

    public static void fillWithScores() {
        Team team1 = getTeam(TeamColor.RED);
        double score1 = team1.getMembersScoreSum();
        Team team2 = getTeam(TeamColor.YELLOW);
        double score2 = team2.getMembersScoreSum();

        final int membersLimit = (Bukkit.getOnlinePlayers().size() + 1) / 2;

        List<UUID> toAdd = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).filter(uuid -> !team1.getMembers().contains(uuid) && !team2.getMembers().contains(uuid)).sorted(Comparator.comparingDouble(GameStatistics::getPlayerTotalScore)).toList());
        while(!toAdd.isEmpty()){
            UUID player = toAdd.remove(toAdd.size() - 1);
            double score = GameStatistics.getPlayerTotalScore(player);
            if(score1 <= score2 && team1.getMembers().size() < membersLimit){
                score1 += score;
                team1.joinTeam(player);
            } else if(team2.getMembers().size() < membersLimit){
                score2 += score;
                team2.joinTeam(player);
            } else {
                score1 += score;
                team1.joinTeam(player);
            }
        }
    }

    private final TeamColor color;
    private SavedLocation spawnLocation;
    private SavedLocation bannerLocation;
    private SavedLocation rollbackLocation;
    private SavedLocation drawbridgeLocation;
    private Area base;
    private Area protectedSpawn;
    private final Set<UUID> members;

    private transient UUID voiceChatGroupUUID;

    public Team(TeamColor color, SavedLocation spawnLocation, SavedLocation bannerLocation, SavedLocation rollbackLocation, SavedLocation drawbridgeLocation, Area base, Area protectedSpawn, Set<UUID> members) {
        this.color = color;
        this.spawnLocation = spawnLocation;
        this.bannerLocation = bannerLocation;
        this.rollbackLocation = rollbackLocation;
        this.drawbridgeLocation = drawbridgeLocation;
        this.base = base;
        this.protectedSpawn = protectedSpawn;
        this.members = members;
    }

    public TeamColor getColor() {
        return color;
    }

    public Location getSpawnLocation() {
        return spawnLocation == null ? null : spawnLocation.toLocation();
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = SavedLocation.fromLocation(spawnLocation);
    }

    public Location getBannerLocation() {
        return bannerLocation == null ? null : bannerLocation.toLocation();
    }

    public void setBannerLocation(Location bannerLocation) {
        this.bannerLocation = SavedLocation.fromLocation(bannerLocation);
    }

    public Area getBase() {
        return base;
    }

    public void setBase(Area base) {
        this.base = base;
    }

    public Area getProtectedSpawn() {
        return protectedSpawn;
    }

    public void setProtectedSpawn(Area protectedSpawn) {
        this.protectedSpawn = protectedSpawn;
    }

    public Location getDrawbridgeLocation() {
        return drawbridgeLocation == null ? null : drawbridgeLocation.toLocation();
    }

    public void setDrawbridgeLocation(Location drawbridgeLocation) {
        this.drawbridgeLocation = SavedLocation.fromLocation(drawbridgeLocation);
    }

    public Location getRollbackLocation() {
        return rollbackLocation == null ? null : rollbackLocation.toLocation();
    }

    public void setRollbackLocation(Location rollbackLocation) {
        this.rollbackLocation = SavedLocation.fromLocation(rollbackLocation);
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

    public UUID getVoiceChatGroupUUID() {
        return voiceChatGroupUUID;
    }

    public void setVoiceChatGroupUUID(UUID voiceChatGroupUUID) {
        this.voiceChatGroupUUID = voiceChatGroupUUID;
    }

    public double getMembersScoreSum(){
        double score = 0;
        for(UUID member : this.getMembers()){
            score += GameStatistics.getPlayerTotalScore(member);
        }
        return score;
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
