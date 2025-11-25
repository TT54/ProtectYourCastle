package fr.tt54.protectYourCastle.inventories;

import fr.impyria.core.utils.items.DefaultItems;
import fr.impyria.core.utils.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PageableInventory<O> extends CorePersonalInventory {

    private final Map<Integer, O> objectsMap = new HashMap<>();
    protected int page;

    public PageableInventory(String title, Player player, int page) {
        super(title, player);
        this.page = page;
    }

    @Override
    public final @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(6);
        objectsMap.clear();

        for (int i = 1; i < 8; i++) {
            for (int j = 1; j < 5; j++) {
                inv.setItem(i + 9 * j, DefaultItems.AIR.build());
            }
        }

        List<O> objects = new ArrayList<>(this.getObjectsList());
        if (7 * 4 * (page - 1) < objects.size()) {
            objects = objects.subList(7 * 4 * (page - 1), Math.min(7 * 4 * page, objects.size()));
        } else {
            objects = new ArrayList<>();
        }

        for (int i = 0; i < objects.size(); i++) {
            O o = objects.get(i);
            int slot = 9 + 1 + i % 7 + 9 * (i / 7);

            ItemStack item = this.getItemFromObject(o);
            inv.setItem(slot, item);
            objectsMap.put(slot, o);
        }

        if (page > 1) {
            inv.setItem(9 * 5 + 3, DefaultItems.BACK_ARROW.build());
        }
        if (page <= (this.getObjectsList().size() - 1) / (7 * 4)) {
            inv.setItem(9 * 5 + 5, DefaultItems.NEXT_ARROW.build());
        }

        ItemBuilder pageItem = DefaultItems.pageItem.clone().replaceInName("%page%", this.page + "");
        inv.setItem(9 * 5 + 4, pageItem.build());


        this.generateOverlayInv(inv);


        return inv;
    }

    protected abstract ItemStack getItemFromObject(O o);

    protected abstract List<O> getObjectsList();

    protected abstract void generateOverlayInv(Inventory inv);

    protected abstract void onObjectClicked(InventoryClickEvent event, O o);

    protected abstract void onInvClick(InventoryClickEvent event);

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getInventory() == event.getClickedInventory() && event.getCurrentItem() != null) {
            int slot = event.getSlot();

            O o = objectsMap.get(slot);
            if (o != null) {
                this.onObjectClicked(event, o);
            } else if (event.getCurrentItem().getType() == Material.ARROW) {
                if (slot == 9 * 5 + 3) {
                    this.page--;
                    this.openInventory();
                } else if (slot == 9 * 5 + 5) {
                    this.page++;
                    this.openInventory();
                }
            }
        }

        this.onInvClick(event);
    }
}
