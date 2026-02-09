package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Comparator;
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

        displays = Game.gson.fromJson(FileManager.read(displaysFile), displaysType);

        for(RankingDisplay display : displays.values()){
            display.update();
        }
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
        for(RankingDisplay display : displays.values()){
            display.update();
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

    public void update(){
        TextDisplay textDisplay = (TextDisplay) Bukkit.getEntity(this.displayTextUUID);
        if(textDisplay == null){
            System.err.println("Impossible de trouver le display " + this.displayTextUUID + " pour le classement " + this.rankingDisplayType.getDisplayName());
            return;
        }

        textDisplay.setBillboard(Display.Billboard.VERTICAL);
        textDisplay.setSeeThrough(false);

        Map<UUID, Double> scores = rankingDisplayType.getScoresSupplier().get();
        List<UUID> players = scores.keySet().stream().sorted(Comparator.comparingDouble(uuid -> -scores.get(uuid))).toList();
        StringBuilder text = new StringBuilder("§eTop " + rankingDisplayType.getDisplayName() + " :\n");
        for(int i = 0; i < Math.min(10, players.size()); i++){
            UUID playerUUID = players.get(i);
            text.append("§")
                    .append(i == 0 ? "6" : i == 1 ? "a" : i == 2 ? "a" : "f")
                    .append(i + 1)
                    .append(". ")
                    .append(Bukkit.getOfflinePlayer(playerUUID).getName())
                    .append(" : §e")
                    .append(scores.get(playerUUID).intValue())
                    .append("\n");
        }
        textDisplay.setText(text.toString());
    }

    public enum RankingDisplayType {
        TOP_WINS("Victoires", GameStatistics::getPlayersWins),
        TOP_KILLS("Kills", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.KILLS)),
        TOP_POINTS("Points Gagnés", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.POINTS_WON)),
        TOP_BANNERS("Bannières Cassées", () -> GameStatistics.getPLayersTotalStatistic(GameStatistics.StatisticKey.BANNERS_BROKEN)),
        TOP_TOTAL_SCORE("Score Total", GameStatistics::getPlayersTotalScores),
        TOP_SCORE("Elo", GameStatistics::getPlayersCurrentScores);

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
