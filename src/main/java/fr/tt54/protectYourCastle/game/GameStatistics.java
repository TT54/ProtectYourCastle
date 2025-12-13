package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class GameStatistics {

    public static List<GameStatistics> gameStatistics = new ArrayList<>();
    private static final Type statisticsType = new TypeToken<List<GameStatistics>>() {}.getType();

    private static final Map<UUID, List<Double>> playerGamesScore = new HashMap<>();
    private static final Map<UUID, Double> playerCurrentScore = new HashMap<>();

    public static void load(){
        File statisticsFile = FileManager.getFileWithoutCreating("statistics.json", ProtectYourCastleMain.getInstance());

        if (!statisticsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("statistics.json", false);
        }

        gameStatistics = Game.gson.fromJson(FileManager.read(statisticsFile), statisticsType);

        for(GameStatistics statistics : gameStatistics){
            if(statistics.playerScores == null || statistics.playerScores.isEmpty()) {
                statistics.playerScores = new HashMap<>();
                for(UUID uuid : statistics.getPlayers()){
                    statistics.calculatePlayerScore(uuid);
                }
            }

            for(UUID uuid : statistics.getPlayers()){
                addPlayerGameScore(uuid, statistics.getPlayerScore(uuid));
            }
        }
    }

    public static void save(){
        File statisticsFile = FileManager.getFile("statistics.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(gameStatistics), statisticsFile);
    }

    public static List<Double> getPlayerGamesScore(UUID playerUUID){
        return playerGamesScore.getOrDefault(playerUUID, new ArrayList<>());
    }

    private static void addPlayerGameScore(UUID playerUUID, double score){
        List<Double> scores = getPlayerGamesScore(playerUUID);
        scores.add(score);
        playerGamesScore.put(playerUUID, scores);
        recalculatePlayerScore(playerUUID);
    }

    private static void recalculatePlayerScore(UUID playerUUID){
        List<Double> scores = getPlayerGamesScore(playerUUID);
        double score = 0;
        for(int i = 0; i < Math.min(scores.size(), GameParameters.SCORES_USED.get()); i++){
            score += scores.get(scores.size() - 1 - i);
        }
        playerCurrentScore.put(playerUUID, score);
    }

    public static double getPlayerTotalScore(UUID playerUUID){
        return playerCurrentScore.getOrDefault(playerUUID, 0d);
    }

    private final long gameBegin;
    private long gameEnd;
    private final Map<StatisticKey, Map<UUID, Integer>> values;
    private final Map<UUID, Team.TeamColor> playerTeam;
    private Map<UUID, Double> playerScores = new HashMap<>();

    private Team.TeamColor winner = null;

    public GameStatistics(long gameBegin, long gameEnd, Map<StatisticKey, Map<UUID, Integer>> values, Map<UUID, Team.TeamColor> playerTeam) {
        this.gameBegin = gameBegin;
        this.gameEnd = gameEnd;
        this.values = values;
        this.playerTeam = playerTeam;
    }

    public int getPlayerStatistic(UUID playerUUID, StatisticKey key){
        return this.values.getOrDefault(key, new HashMap<>()).getOrDefault(playerUUID, 0);
    }

    public double getPlayerStatisticsRatio(UUID playerUUID, StatisticKey key){
        double total = 0;
        for(UUID uuid : this.getPlayers()){
            total += this.getPlayerStatistic(uuid, key);
        }
        return total == 0 ? 0 : this.getPlayerStatistic(playerUUID, key) / total;
    }

    public void setPlayerStatistic(UUID playerUUID, StatisticKey key, int value){
        Map<UUID, Integer> keyMap = this.values.getOrDefault(key, new HashMap<>());
        keyMap.put(playerUUID, value);
        this.values.put(key, keyMap);
    }

    public void increaseStatistic(UUID playerUUID, StatisticKey key){
        this.setPlayerStatistic(playerUUID, key, this.getPlayerStatistic(playerUUID, key) + 1);
    }

    public int getTeamStatistic(Team.TeamColor color, StatisticKey key){
        int value = 0;
        for(UUID playerUUID : this.playerTeam.keySet()){
            if(this.playerTeam.get(playerUUID) == color){
                value += this.getPlayerStatistic(playerUUID, key);
            }
        }
        return value;
    }

    public UUID getBestPlayer(StatisticKey key){
        UUID best = null;
        for(UUID uuid : this.playerTeam.keySet()){
            if(best == null || this.getPlayerStatistic(uuid, key) > this.getPlayerStatistic(best, key)) best = uuid;
        }
        return best;
    }

    public Team.TeamColor getBestTeam(StatisticKey key){
        Team.TeamColor best = Team.TeamColor.RED;
        for(Team.TeamColor color : Team.TeamColor.values()){
            if(getTeamStatistic(color, key) > getTeamStatistic(best, key)) best = color;
        }
        return best;
    }

    public long getGameBegin() {
        return gameBegin;
    }

    public long getGameEnd() {
        return gameEnd;
    }

    public void setGameEnd(long gameEnd) {
        this.gameEnd = gameEnd;
        this.winner = this.getBestTeam(StatisticKey.POINTS_WON);
        for(UUID player : this.getPlayers()){
            this.calculatePlayerScore(player);
        }
    }

    public double getPlayerScore(UUID playerUUID){
        return this.playerScores.getOrDefault(playerUUID, 0d);
    }

    public Map<UUID, Double> getPlayerScores() {
        return playerScores;
    }

    private void calculatePlayerScore(UUID playerUUID){
        double score = this.getPlayerTeam(playerUUID) == this.getWinner() ? GameParameters.PERSONAL_SCORE_WIN.get() : 0;
        int pointsWon = this.getPlayerStatistic(playerUUID, StatisticKey.POINTS_WON);
        double bannersRatio = this.getPlayerStatisticsRatio(playerUUID, StatisticKey.BANNERS_BROKEN);

        double expectedKills = 1d / this.getPlayers().size() + this.getPlayerStatisticsRatio(playerUUID, StatisticKey.KILLS);
        double expectedDeaths = 1d / this.getPlayers().size() + this.getPlayerStatisticsRatio(playerUUID, StatisticKey.DEATHS);

        score += GameParameters.PERSONAL_SCORE_KILLS_COEFF.get() * (.2 + expectedKills);
        score -= GameParameters.PERSONAL_SCORE_DEATHS_COEFF.get() * Math.max(0, expectedDeaths);
        score += GameParameters.PERSONAL_SCORE_POINTS_COEFF.get() * pointsWon;

        score *= 1 + bannersRatio / 2;
        this.playerScores.put(playerUUID, score);
        addPlayerGameScore(playerUUID, score);
    }

    public Team.TeamColor getWinner() {
        return this.winner;
    }

    public Set<UUID> getPlayers() {
        return this.playerTeam.keySet();
    }

    public Team.TeamColor getPlayerTeam(UUID playerUUID) {
        return this.playerTeam.get(playerUUID);
    }

    public enum StatisticKey {

        KILLS("Kills"),
        DEATHS("Morts"),
        BANNERS_BROKEN("Bannières cassées"),
        POINTS_WON("Points gagnés");

        private final String displayName;

        StatisticKey(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
