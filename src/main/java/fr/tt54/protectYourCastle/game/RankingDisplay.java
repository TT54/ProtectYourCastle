package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class RankingDisplay {

    private static Map<UUID, RankingDisplay> displays;
    private static final Type displaysType = new TypeToken<Map<UUID, RankingDisplay>>() {}.getType();

    public static void load(){
        File displaysFile = FileManager.getFileWithoutCreating("displays.json", ProtectYourCastleMain.getInstance());

        if (!displaysFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("displays.json", false);
        }

        try {
            displays = Game.gson.fromJson(FileManager.read(displaysFile), displaysType);
        } catch (Throwable throwable){
            ProtectYourCastleMain.getInstance().getLogger().warning("displays.json invalide, fallback sur une liste vide: " + throwable.getClass().getSimpleName());
            displays = null;
        }
        if(displays == null){
            displays = new HashMap<>();
        }
        displays.values().removeIf(rankingDisplay -> rankingDisplay.rankingDisplayType == null);
    }

    public static void save(){
        File displaysFile = FileManager.getFile("displays.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(displays), displaysFile);
    }

    public static RankingDisplay spawnDisplay(RankingDisplayType type, Location location){
        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        RankingDisplay display = new RankingDisplay(textDisplay.getUniqueId(), type);
        display.update();
        displays.put(display.displayTextUUID, display);
        return display;
    }

    public static void updateDisplays(){
        int removedDisplays = updateDisplaysInternal();
        if(removedDisplays > 0){
            ProtectYourCastleMain.getInstance().getLogger().warning(removedDisplays + " display(s) de classement introuvable(s) ont ete supprimes de displays.json");
            save();
        }
    }

    public static boolean removeDisplay(TextDisplay textDisplay) {
        RankingDisplay display = displays.remove(textDisplay.getUniqueId());
        if(display != null) {
            textDisplay.remove();
            return true;
        }
        return false;
    }

    private final UUID displayTextUUID;
    private final RankingDisplayType rankingDisplayType;

    private RankingDisplay(UUID displayTextUUID, RankingDisplayType rankingDisplayType) {
        this.displayTextUUID = displayTextUUID;
        this.rankingDisplayType = rankingDisplayType;
    }

    private static int updateDisplaysInternal(){
        int removedDisplays = 0;
        Iterator<Map.Entry<UUID, RankingDisplay>> iterator = displays.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<UUID, RankingDisplay> entry = iterator.next();
            RankingDisplay display = entry.getValue();
            if(display == null || !display.update()){
                iterator.remove();
                removedDisplays++;
            }
        }
        return removedDisplays;
    }

    public boolean update(){
        Entity entity = Bukkit.getEntity(this.displayTextUUID);
        if(!(entity instanceof TextDisplay textDisplay)){
            return false;
        }

        textDisplay.setBillboard(Display.Billboard.VERTICAL);
        textDisplay.setSeeThrough(false);

        Map<UUID, Double> scores = rankingDisplayType.getScoresSupplier().get();
        List<UUID> players = scores.keySet().stream().sorted(Comparator.comparingDouble(uuid -> -scores.get(uuid))).toList();
        StringBuilder text = new StringBuilder("§eTop " + rankingDisplayType.getDisplayName() + " :\n");
        for(int i = 0; i < Math.min(10, players.size()); i++){
            UUID playerUUID = players.get(i);
            String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
            if(playerName == null || playerName.isBlank()){
                playerName = playerUUID.toString().substring(0, 8);
            }
            text.append("§")
                    .append(i == 0 ? "6" : i == 1 ? "a" : i == 2 ? "a" : "f")
                    .append(i + 1)
                    .append(". ")
                    .append(playerName)
                    .append(" : §e")
                    .append(scores.get(playerUUID).intValue())
                    .append("\n");
        }
        textDisplay.setText(text.toString());
        return true;
    }

    public enum RankingDisplayType {
        TOP_WINS("Victoires", GameStatistics::getPlayersWins),
        TOP_KILLS("Kills", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.KILLS)),
        TOP_POINTS("Points Gagnés", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.POINTS_WON)),
        TOP_BANNERS("Bannières Cassées", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.BANNERS_BROKEN)),
        TOP_TOTAL_SCORE("Score Total", GameStatistics::getPlayersTotalScores),
        TOP_BEST_ELO("Meilleur Elo", GameStatistics::getPlayerBestElo),
        TOP_ELO("Elo", GameStatistics::getPlayersCurrentScores);

        private final String displayName;
        private final Supplier<Map<UUID, Double>> scoresSupplier;

        RankingDisplayType(String displayName, Supplier<Map<UUID, Double>> scoresSupplier) {
            this.displayName = displayName;
            this.scoresSupplier = scoresSupplier;
            scoresSupplier.get();
        }

        public Supplier<Map<UUID, Double>> getScoresSupplier() {
            return scoresSupplier;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
