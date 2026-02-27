package fr.tt54.protectYourCastle.inventories.traders.weapons;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
import fr.tt54.protectYourCastle.inventories.PageableInventory;
import fr.tt54.protectYourCastle.utils.DefaultItems;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EditWeaponsBundleInventory extends PageableInventory<Trader.GameWeapon> {

    private final List<Trader.GameWeapon> bundle;
    private final CorePersonalInventory previousInv;

    public EditWeaponsBundleInventory(Player player, int page, List<Trader.GameWeapon> bundle, CorePersonalInventory previousInv) {
        super("Edition d'un bundle", player, page);
        this.bundle = bundle;
        this.previousInv = previousInv;
    }

    @Override
    protected ItemStack getItemFromObject(Trader.GameWeapon weapon) {
        return weapon.getGunTrade().getReward().clone();
    }

    @Override
    protected List<Trader.GameWeapon> getObjectsList() {
        return bundle;
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {
        inv.setItem(9 * 5, DefaultItems.BACK.build());
        inv.setItem(9 * 5 + 8, new ItemBuilder(Material.BOW, "§aAjouter une arme").build());
    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, Trader.GameWeapon weapon) {
        WeaponInventory inv = new WeaponInventory(player, true, weapon, this.bundle, this);
        inv.openInventory();
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()){
            if(event.getSlot() == 9 * 5 + 8){
                WeaponInventory inv = new WeaponInventory(player, this.bundle, this);
                inv.openInventory();
            } else if(event.getSlot() == 9 * 5){
                previousInv.openInventory();
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
