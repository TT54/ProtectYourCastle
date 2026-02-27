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

public class TradeListInventory extends PageableInventory<Trader.NPCTrade> {

    private final Trader.TradesBase trades;

    public TradeListInventory(Player player, int page, Trader.TradesBase trades) {
        super("Liste des échanges", player, page);
        this.trades = trades;
    }

    @Override
    protected ItemStack getItemFromObject(Trader.NPCTrade trade) {
        return trade.getReward().clone();
    }

    @Override
    protected List<Trader.NPCTrade> getObjectsList() {
        return trades.getTrades();
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {
        inv.setItem(9 * 5 + 8, new ItemBuilder(Material.DIAMOND, "§aCréer un trade").build());
/*        inv.setItem(9 * 5 + 4, (trades.isWeaponTrader() ?
                new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aNPC vendeur d'armes") :
                new ItemBuilder(Material.RED_STAINED_GLASS_PANE, "§cNPC normal"))
                .build()
        );*/
    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, Trader.NPCTrade trade) {
        EditTradeInventory inv = new EditTradeInventory(player, this.trades, trade, this);
        inv.openInventory();
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {
        if(event.getInventory() == event.getClickedInventory()){
            if(event.getSlot() == 9 * 5 + 8) {
                AddTradeInventory inv = new AddTradeInventory(player, this.trades, this);
                inv.openInventory();
            } /*else if(event.getSlot() == 9 * 5 + 4){
                this.trades.setWeaponTrader(!this.trades.isWeaponTrader());
                this.openInventory();
            }*/
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
