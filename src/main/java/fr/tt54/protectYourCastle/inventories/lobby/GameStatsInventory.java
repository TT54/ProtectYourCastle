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

        UUID bestKiller = gameStats.getBestKiller();
        UUID moreDeaths = gameStats.getMoreDeaths();
        UUID bestScorer = gameStats.getBestScorer();
        UUID bestBannerBreaker = gameStats.getBestBannerBreaker();
        Team.TeamColor winner = gameStats.getWinner();

        inv.setItem(9 + 2, drawBestPlayerStats(bestKiller, gameStats.getKills(bestKiller), "Kills", gameStats.getKills(this.player.getUniqueId())));
        inv.setItem(9 + 3, drawBestPlayerStats(moreDeaths, gameStats.getDeaths(moreDeaths), "Morts", gameStats.getDeaths(this.player.getUniqueId())));
        inv.setItem(9 + 4,
                new ItemBuilder(winner.getBanner(), "§eVictoire " + winner.getChatColor() + winner.name() + " : §e" + gameStats.getPoints(winner))
                .setLore("§7----------")
                .addLoreLine(Stream.of(Team.TeamColor.values()).map(color -> color.getChatColor() + color.name() + " :§7 " + gameStats.getPoints(color)).toList())
                .build()
        );
        inv.setItem(9 + 5, drawBestPlayerStats(bestScorer, gameStats.getPointsPerPlayer(bestScorer), "Points Gagnés", gameStats.getPointsPerPlayer(this.player.getUniqueId())));
        inv.setItem(9 + 6, drawBestPlayerStats(bestBannerBreaker, gameStats.getBannersBroken(bestBannerBreaker), "Bannières cassées", gameStats.getBannersBroken(this.player.getUniqueId())));

        inv.setItem(9 * 3 + 3, getTeamInfoItem(Team.TeamColor.YELLOW));
        inv.setItem(9 * 3 + 4, new ItemBuilder(Material.PAPER, "§eInformations")
                .setLore("§7----------",
                        "§fDurée : §7" + TimeUnit.getShortFormattedTimeLeft((int) ((gameStats.endTime() - gameStats.beginTime()) / 1000), TimeUnit.HOURS))
                .addLoreLine()
                .build()
        );
        inv.setItem(9 * 3 + 5, getTeamInfoItem(Team.TeamColor.RED));

        return inv;
    }

    public ItemStack getTeamInfoItem(Team.TeamColor teamColor){
        return new ItemBuilder(teamColor.getBanner(), teamColor.getChatColor() + teamColor.name())
                .setLore("§7----------")
                .addLoreLine(this.gameStats.players().keySet().stream().filter(uuid -> this.gameStats.players().get(uuid) == teamColor).map(uuid -> "§f - " + teamColor.getChatColor() + Bukkit.getOfflinePlayer(uuid).getName()).toList())
                .build();
    }

    public ItemStack drawBestPlayerStats(UUID bestUUID, int score, String label, int playerScore){
        OfflinePlayer p = Bukkit.getOfflinePlayer(bestUUID);
        return new ItemBuilder(Material.PLAYER_HEAD, "§eTop §6" + label + "§e : §6§l" + p.getName())
                .setHeadOwner(p)
                .setLore("§6" + label + " §e--> §f" + score, "§7----------", "§f" + this.player.getName(), "§7" + label + " --> §f" + playerScore)
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
