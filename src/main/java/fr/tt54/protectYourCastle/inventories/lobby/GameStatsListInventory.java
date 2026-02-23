package fr.tt54.protectYourCastle.inventories.lobby;

import fr.tt54.protectYourCastle.game.GameStatistics;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.inventories.PageableInventory;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import fr.tt54.protectYourCastle.utils.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameStatsListInventory extends PageableInventory<GameStatistics> {

    public GameStatsListInventory(Player player, int page) {
        super("Statistiques", player, page);
    }

    @Override
    protected ItemStack getItemFromObject(GameStatistics gameStatistics) {
        Instant instant = Instant.ofEpochMilli(gameStatistics.getGameBegin());
        LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        LocalTime localTime = LocalTime.ofInstant(instant, ZoneId.systemDefault());
        DecimalFormat format = new DecimalFormat("#");
        return new ItemBuilder(gameStatistics.getWinner() == null ? Material.PAPER : gameStatistics.getWinner().getBanner(), "§ePartie " + localDate.getDayOfMonth() + "/" + localDate.getMonthValue() + "/" + localDate.getYear() + " " + localTime.getHour() + ":" + localTime.getMinute())
                .addLoreLine(
                        "",
                        Team.TeamColor.RED.getChatColor() + Team.TeamColor.RED.name() + " "
                                + (gameStatistics.getWinner() == Team.TeamColor.RED ? "§f§l" : "§7")
                                + gameStatistics.getTeamStatistic(Team.TeamColor.RED, GameStatistics.StatisticKey.POINTS_WON)
                                + " §8- "
                                + (gameStatistics.getWinner() == Team.TeamColor.YELLOW ? "§f§l" : "§7")
                                + gameStatistics.getTeamStatistic(Team.TeamColor.YELLOW, GameStatistics.StatisticKey.POINTS_WON)
                                + " " + Team.TeamColor.YELLOW.getChatColor() + Team.TeamColor.YELLOW.name(),
                        "§7----------"
                )
                .addLoreLine(gameStatistics.getPlayers().stream().sorted(Comparator.comparingDouble(uuid -> -gameStatistics.getPlayerScore(uuid))).map(uuid -> "§f - " + gameStatistics.getPlayerTeam(uuid).getChatColor() + Bukkit.getOfflinePlayer(uuid).getName() + " §8 (" + format.format(gameStatistics.getPlayerScore(uuid)) + ")").toList())
                .addLoreLine("§7----------", "§eDurée : §f" + TimeUnit.getShortFormattedTimeLeft((int) (gameStatistics.getGameEnd() - gameStatistics.getGameBegin()) / 1000, TimeUnit.HOURS))
                .build();
    }

    @Override
    protected List<GameStatistics> getObjectsList() {
        List<GameStatistics> gameStatistics = new ArrayList<>(GameStatistics.gameStatistics);
        Collections.reverse(gameStatistics);
        return gameStatistics;
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {

    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, GameStatistics gameStatistics) {
        GameStatsInventory inv = new GameStatsInventory(this.player, gameStatistics);
        inv.openInventory();
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {

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
