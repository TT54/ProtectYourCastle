package fr.tt54.protectYourCastle.inventories.lobby;

import fr.tt54.protectYourCastle.game.GameStatistics;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class GameStatsInventory extends CorePersonalInventory {

    private final GameStatistics gameStats;

    public GameStatsInventory(Player player, GameStatistics gameStats) {
        super("Statistiques", player);
        this.gameStats = gameStats;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(5);

        Team.TeamColor winner = gameStats.getWinner();
        inv.setItem(9 + 4,
                new ItemBuilder(winner.getBanner(), "§eVictoire " + winner.getChatColor() + winner.name() + " : §e" + gameStats.getTeamStatistic(winner, GameStatistics.StatisticKey.POINTS_WON))
                        .setLore("§7----------")
                        .addLoreLine(Stream.of(Team.TeamColor.values()).map(color -> color.getChatColor() + color.name() + " :§7 " + gameStats.getTeamStatistic(color, GameStatistics.StatisticKey.POINTS_WON)).toList())
                        .build()
        );

        List<Integer> slots = new ArrayList<>(List.of(9 + 6, 9 + 5, 9 + 3, 9 + 2));
        for(GameStatistics.StatisticKey key : GameStatistics.StatisticKey.values()){
            inv.setItem(slots.remove(slots.size() - 1), this.drawBestPlayerStats(key));
        }

        inv.setItem(9 * 3 + 3, getTeamInfoItem(Team.TeamColor.YELLOW));
        inv.setItem(9 * 3 + 4, new ItemBuilder(Material.PAPER, "§eInformations")
                .setLore("§7----------",
                        "§fDurée : §7" + TimeUnit.getShortFormattedTimeLeft((int) ((gameStats.getGameEnd() - gameStats.getGameBegin()) / 1000), TimeUnit.HOURS))
                .addLoreLine()
                .build()
        );
        inv.setItem(9 * 3 + 5, getTeamInfoItem(Team.TeamColor.RED));

        return inv;
    }

    public ItemStack getTeamInfoItem(Team.TeamColor teamColor){
        return new ItemBuilder(teamColor.getBanner(), teamColor.getChatColor() + teamColor.name())
                .setLore("§7----------")
                .addLoreLine(this.gameStats.getPlayers().stream().filter(uuid -> this.gameStats.getPlayerTeam(uuid) == teamColor).map(uuid -> "§f - " + teamColor.getChatColor() + Bukkit.getOfflinePlayer(uuid).getName()).toList())
                .build();
    }

    public ItemStack drawBestPlayerStats(GameStatistics.StatisticKey key){
        UUID bestUUID = this.gameStats.getBestPlayer(key);
        OfflinePlayer p = Bukkit.getOfflinePlayer(bestUUID);
        return new ItemBuilder(Material.PLAYER_HEAD, "§eTop §6" + key.getDisplayName() + "§e : §6§l" + p.getName())
                .setHeadOwner(p)
                .setLore("§6" + key.getDisplayName() + " §e--> §f" + this.gameStats.getPlayerStatistic(bestUUID, key), "§7----------", "§f" + this.player.getName(), "§7" + key.getDisplayName() + " --> §f" + this.gameStats.getPlayerStatistic(this.player.getUniqueId(), key))
                .build();
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
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
