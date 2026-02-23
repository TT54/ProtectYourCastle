package fr.tt54.protectYourCastle.inventories.lobby;

import fr.tt54.protectYourCastle.game.GameParameters;
import fr.tt54.protectYourCastle.game.GameStatistics;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
import fr.tt54.protectYourCastle.utils.DefaultItems;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import fr.tt54.protectYourCastle.utils.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Stream;

public class GameStatsInventory extends CorePersonalInventory {

    private final GameStatistics gameStats;
    private final int[] slots = {
            9 + 2       , 9 + 3     , 9 + 5     , 9 + 6,
            2 * 9 + 2   , 2 * 9 + 3 , 9 * 2 + 5 , 9 * 2 + 6,
    };

    public GameStatsInventory(Player player, GameStatistics gameStats) {
        super("Statistiques", player);
        this.gameStats = gameStats;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(5);

        if(GameParameters.DISPLAY_SCORE.get()){
            inv.setItem(4, this.getPlayersScoreItem());
        }

        Team.TeamColor winner = gameStats.getWinner();
        inv.setItem(9 + 4,
                new ItemBuilder(winner.getBanner(), "§eVictoire " + winner.getChatColor() + winner.name() + " : §e" + gameStats.getTeamStatistic(winner, GameStatistics.StatisticKey.POINTS_WON))
                        .setLore("§7----------")
                        .addLoreLine(Stream.of(Team.TeamColor.values()).map(color -> color.getChatColor() + color.name() + " :§7 " + gameStats.getTeamStatistic(color, GameStatistics.StatisticKey.POINTS_WON)).toList())
                        .build()
        );

        for(int i = 0; i < GameStatistics.StatisticKey.values().length; i++){
            if(i >= slots.length) break;
            inv.setItem(slots[i], this.drawBestPlayerStats(GameStatistics.StatisticKey.values()[i]));
        }

        inv.setItem(9 * 3 + 3, getTeamInfoItem(Team.TeamColor.YELLOW));
        inv.setItem(9 * 3 + 4, new ItemBuilder(Material.PAPER, "§eInformations")
                .setLore("§7----------",
                        "§fDurée : §7" + TimeUnit.getShortFormattedTimeLeft((int) ((gameStats.getGameEnd() - gameStats.getGameBegin()) / 1000), TimeUnit.HOURS))
                .addLoreLine()
                .build()
        );
        inv.setItem(9 * 3 + 5, getTeamInfoItem(Team.TeamColor.RED));

        inv.setItem(9 * 4, DefaultItems.BACK.build());

        return inv;
    }

    private ItemStack getPlayersScoreItem() {
        DecimalFormat format = new DecimalFormat("#");
        List<Map.Entry<UUID, Double>> sortedScores = this.gameStats.getPlayerScores().entrySet().stream().sorted(Comparator.comparingDouble(value -> -value.getValue())).toList();
        return new ItemBuilder(Material.DIAMOND, "§bMVP : §6§l" + Bukkit.getOfflinePlayer(sortedScores.get(0).getKey()).getName())
                .addLoreLine(sortedScores.stream().map(entry -> "§7 - " + this.gameStats.getPlayerTeam(entry.getKey()).getChatColor() + Bukkit.getOfflinePlayer(entry.getKey()).getName() + "§7 : §f" + format.format(entry.getValue())).toList())
                .build();
    }

    public ItemStack getTeamInfoItem(Team.TeamColor teamColor){
        return new ItemBuilder(teamColor.getBanner(), teamColor.getChatColor() + teamColor.name())
                .setLore("§7----------")
                .addLoreLine(this.gameStats.getPlayers().stream().filter(uuid -> this.gameStats.getPlayerTeam(uuid) == teamColor).map(uuid -> "§f - " + teamColor.getChatColor() + Bukkit.getOfflinePlayer(uuid).getName()).toList())
                .build();
    }

    public ItemStack drawBestPlayerStats(GameStatistics.StatisticKey key){
        List<UUID> results = this.gameStats.getPlayers().stream().sorted(Comparator.comparingInt(uuid -> -this.gameStats.getPlayerStatistic(uuid, key))).toList();
        UUID bestUUID = results.get(0);
        OfflinePlayer p = Bukkit.getOfflinePlayer(bestUUID);
        int bestStat = this.gameStats.getPlayerStatistic(bestUUID, key);
        if(bestStat == 0) return new ItemBuilder(Material.BARRIER, "§cAucune donnée pour : " + key.getDisplayName()).build();
        return new ItemBuilder(Material.PLAYER_HEAD, "§eTop §6" + key.getDisplayName() + "§e : §6§l" + p.getName())
                .setHeadOwner(p)
                .setLore(results.stream()
                        .map(uuid -> this.gameStats.getPlayerTeam(uuid).getChatColor() + Bukkit.getOfflinePlayer(uuid).getName() + " §e--> §f" + (this.gameStats.getPlayerStatistic(uuid, key) / key.getDivisionDisplayFactor()) + key.getSuffix())
                        .toList())
                .build();
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if(event.getClickedInventory() == event.getInventory() && event.getSlot() == 9 * 4){
            GameStatsListInventory inv = new GameStatsListInventory(this.player, 1);
            inv.openInventory();
        }
    }

    @Override
    public void onInventoryOpen() {

    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {

    }

    @Override
    public void onInventoryDrag(InventoryDragEvent event) {

    }
}
