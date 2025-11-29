package fr.tt54.protectYourCastle.inventories;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TradeListInventory extends PageableInventory<Trader.NPCTrade>{

    private final Trader trader;

    public TradeListInventory(Player player, int page, Trader trader) {
        super("Liste des échanges", player, page);
        this.trader = trader;
    }

    @Override
    protected ItemStack getItemFromObject(Trader.NPCTrade trade) {
        return trade.getReward().clone();
    }

    @Override
    protected List<Trader.NPCTrade> getObjectsList() {
        return trader.getTrades();
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {
        inv.setItem(9 * 5 + 8, new ItemBuilder(Material.DIAMOND, "§aCréer un trade").build());
    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, Trader.NPCTrade trade) {
        EditTradeInventory inv = new EditTradeInventory(player, this.trader, trade, this);
        inv.openInventory();
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {
        if(event.getInventory() == event.getClickedInventory() && event.getSlot() == 9 * 5 + 8){
            AddTradeInventory inv = new AddTradeInventory(player, this.trader, this);
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
