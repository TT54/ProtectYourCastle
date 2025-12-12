package fr.tt54.protectYourCastle.inventories.trades;

import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.inventories.CorePersonalInventory;
import fr.tt54.protectYourCastle.utils.DefaultItems;
import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AddTradeInventory extends CorePersonalInventory {

    private final Trader trader;
    private final CorePersonalInventory previousInv;

    public AddTradeInventory(Player player, Trader trader, CorePersonalInventory previousInv) {
        super("Ajouter un trade", player);
        this.trader = trader;
        this.previousInv = previousInv;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(3);

        inv.setItem(9 + 2, new ItemStack(Material.AIR));
        inv.setItem(9 + 3, new ItemStack(Material.AIR));
        inv.setItem(9 + 6, new ItemStack(Material.AIR));

        inv.setItem(9 * 2, DefaultItems.BACK.build());
        inv.setItem(9 * 2 + 8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aValider").build());

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()) {
            if(event.getSlot() == 9 * 2){
                previousInv.openInventory();
            } else if (event.getSlot() == 9 * 2 + 8) {
                ItemStack item1 = event.getInventory().getItem(9 + 2);
                ItemStack item2 = event.getInventory().getItem(9 + 3);
                ItemStack result = event.getInventory().getItem(9 + 6);

                List<ItemStack> inputs = new ArrayList<>();
                if(item1 != null && item1.getType() != Material.AIR) inputs.add(item1);
                if(item2 != null && item2.getType() != Material.AIR) inputs.add(item2);

                if(inputs.isEmpty()){
                    player.sendMessage("§cImpossible de créer un trade vide !");
                    return;
                }

                if(result == null || result.getType() == Material.AIR){
                    player.sendMessage("§cImpossible de créer un trade vide !");
                    return;
                }

                Trader.NPCTrade trade = new Trader.NPCTrade(inputs, result);
                this.trader.addTrade(trade);
                this.openInventory();
            } else if (event.getSlot() != 9 + 2 && event.getSlot() != 9 + 3 && event.getSlot() != 9 + 6) {
                event.setCancelled(true);
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
