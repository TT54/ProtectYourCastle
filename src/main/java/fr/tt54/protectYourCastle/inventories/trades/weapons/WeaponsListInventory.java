package fr.tt54.protectYourCastle.inventories.trades.weapons;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
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

public class WeaponsListInventory extends PageableInventory<Trader.GameWeapon> {

    public WeaponsListInventory(Player player, int page) {
        super("Liste des armes", player, page);
    }

    @Override
    protected ItemStack getItemFromObject(Trader.GameWeapon weapon) {
        return weapon.getGunTrade().getReward().clone();
    }

    @Override
    protected List<Trader.GameWeapon> getObjectsList() {
        return Trader.weapons;
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {
        inv.setItem(9 * 5 + 8, new ItemBuilder(Material.BOW, "§aAjouter une arme").build());
    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, Trader.GameWeapon weapon) {
        WeaponInventory inv = new WeaponInventory(player, true, weapon, this);
        inv.openInventory();
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()){
            if(event.getSlot() == 9 * 5 + 8){
                WeaponInventory inv = new WeaponInventory(player, this);
                inv.openInventory();
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
