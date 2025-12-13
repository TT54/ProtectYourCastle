package fr.tt54.protectYourCastle.inventories;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConfirmationInventory extends CorePersonalInventory {

    private final List<String> validateLore;
    private final List<String> cancelLore;
    private final Runnable onValidation;
    private final Runnable onCancel;

    public ConfirmationInventory(String title, Player player, List<String> validateLore, List<String> cancelLore, Runnable onValidation, Runnable onCancel) {
        super(title, player);
        this.validateLore = validateLore;
        this.cancelLore = cancelLore;
        this.onValidation = onValidation;
        this.onCancel = onCancel;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(this.getTitle(), 3);

        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName("§2Confirmer");
        meta.setLore(validateLore);
        confirm.setItemMeta(meta);
        inv.setItem(9 + 5, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        meta = cancel.getItemMeta();
        meta.setDisplayName("§4Annuler");
        meta.setLore(cancelLore);
        cancel.setItemMeta(meta);
        inv.setItem(9 + 3, cancel);

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getInventory()) {
            if (event.getSlot() == 9 + 3) {
                event.getWhoClicked().closeInventory();
                onCancel.run();
            } else if (event.getSlot() == 9 + 5) {
                event.getWhoClicked().closeInventory();
                onValidation.run();
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
