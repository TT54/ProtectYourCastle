package fr.tt54.protectYourCastle.inventories.traders.trades;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.PageableInventory;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TradesListInventory extends PageableInventory<Trader.TradesBase> {

    public TradesListInventory(Player player, int page) {
        super("Liste des traders", player, page);
    }

    @Override
    protected ItemStack getItemFromObject(Trader.TradesBase tradesBase) {
        return tradesBase.getTrades().isEmpty() ? new ItemStack(Material.BARRIER) : new ItemBuilder(tradesBase.getTrades().get(0).getReward().clone(), "§e" + tradesBase.getName()).build();
    }

    @Override
    protected List<Trader.TradesBase> getObjectsList() {
        return Trader.getAllTradesBases();
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {

    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, Trader.TradesBase tradesBase) {
        TradeListInventory inv = new TradeListInventory(player, 1, tradesBase);
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
