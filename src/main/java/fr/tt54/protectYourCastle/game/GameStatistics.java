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
    private static final Map<UUID, Double> playerCurrentElo = new HashMap<>();
    private static final Map<UUID, Double> playerTotalScore = new HashMap<>();
    private static final Map<UUID, Double> playerBestElo = new HashMap<>();

    public static void load(){
        playerGamesScore.clear();
        playerCurrentElo.clear();
        playerTotalScore.clear();
        File statisticsFile = FileManager.getFileWithoutCreating("statistics.json", ProtectYourCastleMain.getInstance());

        if (!statisticsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("statistics.json", false);
        }

        gameStatistics = Game.gson.fromJson(FileManager.read(statisticsFile), statisticsType);
        recalculateAllPlayersScores();
    }

    public static void recalculateAllPlayersScores(){
        playerGamesScore.clear();
        for(GameStatistics statistics : gameStatistics){
            if(statistics.playerScores == null || statistics.playerScores.isEmpty()) {
                statistics.playerScores = new HashMap<>();
            }
            for(UUID uuid : statistics.getPlayers()){
                statistics.calculatePlayerScore(uuid);
            }
        }
    }

    public static List<UUID> getRegisteredPlayers(){
        return playerCurrentElo.keySet().stream().sorted(Comparator.comparingDouble(uuid -> -getPlayerCurrentScore(uuid))).toList();
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
        double bestGame = 0;
        double worstGame = Double.MAX_VALUE;
        for(int i = 0; i < Math.min(scores.size(), GameParameters.SCORES_USED.get()); i++){
            score += scores.get(scores.size() - 1 - i);
            if(scores.get(scores.size() - 1 - i) > bestGame) bestGame = scores.get(scores.size() - 1 - i);
            if(scores.get(scores.size() - 1 - i) < worstGame) worstGame = scores.get(scores.size() - 1 - i);
        }
        score -= (1 - GameParameters.BEST_GAME_FACTOR.get()) * bestGame + (1 - GameParameters.WORST_GAME_FACTOR.get()) * worstGame;
        playerCurrentElo.put(playerUUID, score);
        playerTotalScore.put(playerUUID, scores.stream().reduce(0d, Double::sum));
        playerBestElo.put(playerUUID, Math.max(playerBestElo.getOrDefault(playerUUID, 0d), score));
    }

    public static double getPlayerCurrentScore(UUID playerUUID){
        return playerCurrentElo.getOrDefault(playerUUID, 0d);
    }

    public static double getPlayerTotalScore(UUID playerUUID){
        return playerTotalScore.getOrDefault(playerUUID, 0d);
    }

    public static Map<UUID, Double> getPlayersCurrentScores(){
        return playerCurrentElo;
    }

    public static Map<UUID, Double> getPlayersTotalScores() {
        return playerTotalScore;
    }

    public static Map<UUID, Double> getPlayerBestElo() {
        return playerBestElo;
    }

    public static Map<UUID, Double> getPLayersTotalStatistic(StatisticKey key){
        Map<UUID, Double> totalStatistic = new HashMap<>();
        for(GameStatistics statistics : gameStatistics){
            for(UUID player : statistics.getPlayers()){
                totalStatistic.put(player, totalStatistic.getOrDefault(player, 0d) + statistics.getPlayerStatistic(player, key));
            }
        }
        return totalStatistic;
    }

    public static Map<UUID, Double> getPlayersWins() {
        Map<UUID, Double> wins = new HashMap<>();
        for(GameStatistics statistics : gameStatistics){
            if(statistics.getWinner() == null) continue;
            for(UUID player : statistics.getPlayers()){
                wins.put(player, wins.getOrDefault(player, 0d) + (statistics.getPlayerTeam(player) == statistics.getWinner() ? 1 : 0));
            }
        }
        return wins;
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

    public void addStatistic(UUID playerUUID, StatisticKey key, int value) {
        this.setPlayerStatistic(playerUUID, key, this.getPlayerStatistic(playerUUID, key) + value);
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
        return this.getStatistics(key).entrySet().stream().sorted(Comparator.comparingInt(entry -> -entry.getValue())).toList().get(0).getKey();
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
        int teamPoints = this.getTeamStatistic(this.getPlayerTeam(playerUUID), StatisticKey.POINTS_WON);
        double bannersRatio = this.getPlayerStatisticsRatio(playerUUID, StatisticKey.BANNERS_BROKEN);

        double expectedDeaths = Math.max(0, this.getPlayerStatisticsRatio(playerUUID, StatisticKey.DEATHS) - 1d / this.getPlayers().size());

        score += GameParameters.PERSONAL_SCORE_KILLS_COEFF.get() * (GameParameters.PERSONAL_SCORE_KILLS_BASE.get() + this.getPlayerStatisticsRatio(playerUUID, StatisticKey.KILLS));
        score -= GameParameters.PERSONAL_SCORE_DEATHS_COEFF.get() * expectedDeaths;
        score += GameParameters.TEAM_BANNERS_BROKEN_COEFF.get() * this.getTeamStatistic(this.getPlayerTeam(playerUUID), StatisticKey.BANNERS_BROKEN);
        score *= 1d + bannersRatio / GameParameters.BANNER_RATIO_POINTS_REDUCTION.get();
        score *= 1d + (pointsWon + teamPoints) / GameParameters.SCORE_POINTS_REDUCTION.get();

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

    public Map<UUID, Integer> getStatistics(StatisticKey key) {
        return this.values.getOrDefault(key, new HashMap<>());
    }

    public enum StatisticKey {

        KILLS("Kills"),
        DEATHS("Morts"),
        BANNERS_BROKEN("Bannières cassées"),
        POINTS_WON("Points gagnés"),
        CRITICAL_KILLS("Kills importants"),
        DAMAGE_DEALT("Dégâts infligés", 100),
        DISTANCE_WALKED("Distance marchée", 100),
        DISTANCE_WITH_PLANE("Distance en avion", 100);

        private final String displayName;
        private final int divisionDisplayFactor;

        StatisticKey(String displayName) {
            this(displayName, 1);
        }

        StatisticKey(String displayName, int divisionDisplayFactor) {
            this.displayName = displayName;
            this.divisionDisplayFactor = divisionDisplayFactor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDivisionDisplayFactor() {
            return divisionDisplayFactor;
        }
    }

}
