package fr.tt54.protectYourCastle.inventories.trades.weapons;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.ConfirmationInventory;
import fr.tt54.protectYourCastle.inventories.PageableInventory;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WeaponsBundleListInventory extends PageableInventory<List<Trader.GameWeapon>> {

    public WeaponsBundleListInventory(Player player, int page) {
        super("Liste des bundles", player, page);
    }

    @Override
    protected ItemStack getItemFromObject(List<Trader.GameWeapon> weapons) {
        Trader.GameWeapon first = weapons.stream().filter(w -> w != null && w.getGunTrade() != null && w.getGunTrade().getReward() != null).findFirst().orElse(null);
        return weapons.isEmpty() ?
                new ItemBuilder(Material.BARRIER, "§cVide").build()
                : first == null
                ? new ItemBuilder(Material.BARRIER, "§cBundle invalide").setLore("§eClique gauche pour éditer", "§eClique droit pour supprimer").build()
                :
                new ItemBuilder(first.getGunTrade().getReward().clone())
                        .setName("§aBundle de " + weapons.size() + " arme" + (weapons.size() > 1 ? "s" : ""))
                        .setLore("§eClique gauche pour éditer", "§eClique droit pour supprimer")
                        .build();
    }

    @Override
    protected List<List<Trader.GameWeapon>> getObjectsList() {
        return Trader.weapons;
    }

    @Override
    protected void generateOverlayInv(Inventory inv) {
        inv.setItem(9 * 5 + 8, new ItemBuilder(Material.BUNDLE, "§aCréer un bundle").build());
    }

    @Override
    protected void onObjectClicked(InventoryClickEvent event, List<Trader.GameWeapon> bundle) {
        if(event.isRightClick()){
            ConfirmationInventory inv = new ConfirmationInventory("Suppression", player,
                    List.of("§eÊtes-vous sûr de vouloir supprimer ce bundle ?"),
                    List.of("§cRetour"),
                    () -> {
                        Trader.weapons.remove(bundle);
                        this.openInventory();
                    },
                    this::openInventory);
            inv.openInventory();
        } else {
            EditWeaponsBundleInventory inv = new EditWeaponsBundleInventory(player, 1, bundle, this);
            inv.openInventory();
        }
    }

    @Override
    protected void onInvClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()){
            if(event.getSlot() == 9 * 5 + 8){
                List<Trader.GameWeapon> bundle = new ArrayList<>();
                Trader.weapons.add(bundle);
                EditWeaponsBundleInventory inv = new EditWeaponsBundleInventory(player, 1, bundle, this);
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
