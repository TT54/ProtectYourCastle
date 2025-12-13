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

public record GameStatistics(long beginTime, long endTime, Map<UUID, Team.TeamColor> players, Map<Team.TeamColor, Integer> scores, Map<UUID, Integer> kills, Map<UUID, Integer> deaths, Map<UUID, Integer> pointsPerPlayer, Map<UUID, Integer> bannerBrokenPerPlayer) {

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

    public int getKills(UUID playerUUID){
        return this.kills.getOrDefault(playerUUID, 0);
    }

    public int getDeaths(UUID playerUUID){
        return this.deaths.getOrDefault(playerUUID, 0);
    }

    public int getPointsPerPlayer(UUID playerUUID){
        return this.pointsPerPlayer.getOrDefault(playerUUID, 0);
    }

    public int getBannersBroken(UUID playerUUID){
        return this.bannerBrokenPerPlayer.getOrDefault(playerUUID, 0);
    }

    public int getKills(Team.TeamColor team){
        return this.getTeamScore(team, this.kills);
    }

    public int getDeaths(Team.TeamColor team){
        return this.getTeamScore(team, this.deaths);
    }

    public int getBannersBroken(Team.TeamColor team){
        return this.getTeamScore(team, this.bannerBrokenPerPlayer);
    }

    public int getTeamScore(Team.TeamColor team, Map<UUID, Integer> map){
        int score = 0;
        for(UUID uuid : this.players.keySet()){
            if(this.players.get(uuid) == team) score += map.getOrDefault(uuid, 0);
        }
        return score;
    }

    public UUID getBestKiller(){
        return getBest(this.kills);
    }

    public UUID getMoreDeaths(){
        return getBest(this.deaths);
    }

    public UUID getBestScorer(){
        return getBest(this.pointsPerPlayer);
    }

    public UUID getBestBannerBreaker(){
        return getBest(this.bannerBrokenPerPlayer);
    }

    private UUID getBest(Map<UUID, Integer> map){
        UUID best = null;
        for(UUID uuid : this.players.keySet()){
            if(best == null || map.getOrDefault(uuid, 0) > map.getOrDefault(best, 0)){
                best = uuid;
            }
        }
        return best;
    }

    public Team.TeamColor getWinner(){
        Team.TeamColor best = Team.TeamColor.RED;
        for(Team.TeamColor teamColor : this.scores.keySet()){
            if(this.scores.getOrDefault(teamColor, 0) > this.scores.getOrDefault(best, 0)){
                best = teamColor;
            }
        }
        return best;
    }

    private Team.TeamColor getBestTeam(Map<UUID, Integer> map){
        Team.TeamColor best = Team.TeamColor.RED;
        int bestScore = 0;
        for(Team.TeamColor teamColor : Team.TeamColor.values()){
            int score = getTeamScore(teamColor, map);
            if(score > bestScore){
                best = teamColor;
                bestScore = score;
            }
        }
        return best;
    }

    public int getPoints(Team.TeamColor winner) {
        return this.scores.getOrDefault(winner, 0);
    }
}
