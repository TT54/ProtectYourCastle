package fr.tt54.protectYourCastle.inventories.traders.npc;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class EditTraderNPCInventory extends CorePersonalInventory {

    private final Trader trader;

    public EditTraderNPCInventory(Player player, Trader trader) {
        super("Edition de " + trader.getDisplayName(), player);
        this.trader = trader;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(3);

        inv.setItem(9 + 4, this.trader.isWeaponTrader() ?
                new ItemBuilder(Material.BOW, "§aMarchand d'armes").build() :
                new ItemBuilder(Material.EMERALD, "§aMarchand d'objets").build()
        );

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if(event.getClickedInventory() == event.getInventory()){
            if(event.getSlot() == 9 + 4){
                this.trader.setWeaponTrader(!this.trader.isWeaponTrader());
                this.openInventory();
            }
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
