package fr.tt54.protectYourCastle.inventories;

import fr.tt54.protectYourCastle.game.Trader;
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

public class EditTradeInventory extends CorePersonalInventory{

    private final Trader trader;
    private final Trader.NPCTrade trade;
    private final CorePersonalInventory previousInv;

    public EditTradeInventory(Player player, Trader trader, Trader.NPCTrade trade, CorePersonalInventory previousInv) {
        super("Ajouter un trade", player);
        this.trader = trader;
        this.trade = trade;
        this.previousInv = previousInv;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(3);

        inv.setItem(9 + 2, !this.trade.getInput().isEmpty() ? this.trade.getInput().get(0).clone() : new ItemStack(Material.AIR));
        inv.setItem(9 + 3, this.trade.getInput().size() >= 2 ? this.trade.getInput().get(1).clone() : new ItemStack(Material.AIR));
        inv.setItem(9 + 6, this.trade.getReward().clone());

        inv.setItem(9 * 2, DefaultItems.BACK.build());
        inv.setItem(9 * 2 + 8, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aValider").build());

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()) {
            if(event.getSlot() == 9 * 2){
                event.setCancelled(true);

                previousInv.openInventory();
            } else if (event.getSlot() == 9 * 2 + 8) {
                event.setCancelled(true);

                ItemStack item1 = event.getInventory().getItem(9 + 2);
                ItemStack item2 = event.getInventory().getItem(9 + 3);
                ItemStack result = event.getInventory().getItem(9 + 6);

                List<ItemStack> inputs = new ArrayList<>();
                if(item1 != null && item1.getType() != Material.AIR) inputs.add(item1);
                if(item2 != null && item2.getType() != Material.AIR) inputs.add(item2);

                if(result == null || result.getType() == Material.AIR){
                    if(inputs.isEmpty()){
                        this.trader.removeTrade(this.trade);
                        player.sendMessage("§cTrade supprimé");
                        this.previousInv.openInventory();
                        return;
                    }
                    player.sendMessage("§cImpossible de créer un trade vide !");
                    return;
                }

                if(inputs.isEmpty()){
                    player.sendMessage("§cImpossible de créer un trade vide !");
                    return;
                }

                this.trade.setInput(inputs);
                this.trade.setReward(result);
                this.trader.buildMerchantMenu();

                this.previousInv.openInventory();
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
