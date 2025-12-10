package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameStatistics(Map<UUID, Team.TeamColor> players, Map<Team.TeamColor, Integer> scores, Map<UUID, Integer> kills, Map<UUID, Integer> pointsPerPlayer) {

    public static List<GameStatistics> gameStatistics = new ArrayList<>();
    private static final Type statisticsType = new TypeToken<List<GameStatistics>>() {}.getType();

    public static void load(){
        File statisticsFile = FileManager.getFileWithoutCreating("statistics.json", ProtectYourCastleMain.getInstance());

        if (!statisticsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("statistics.json", false);
        }

        gameStatistics = Game.gson.fromJson(FileManager.read(statisticsFile), statisticsType);
    }

    public static void save(){
        File statisticsFile = FileManager.getFile("statistics.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(gameStatistics), statisticsFile);
    }

}
