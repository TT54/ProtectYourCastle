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

public class EditTradeInventory extends CorePersonalInventory {

    private static final int SLOT_INPUT_1 = 9 + 2;
    private static final int SLOT_INPUT_2 = 9 + 3;
    private static final int SLOT_TIMING = 9 + 4;
    private static final int SLOT_REWARD = 9 + 6;
    private static final int SLOT_BACK = 9 * 2;
    private static final int SLOT_VALIDATE = 9 * 2 + 8;

    private final Trader trader;
    private final Trader.NPCTrade trade;
    private final CorePersonalInventory previousInv;
    private int availableAfterMinutes;

    public EditTradeInventory(Player player, Trader trader, Trader.NPCTrade trade, CorePersonalInventory previousInv) {
        super("Editer un trade", player);
        this.trader = trader;
        this.trade = trade;
        this.previousInv = previousInv;
        this.availableAfterMinutes = this.trade.getAvailableAfterMinutes();
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(3);

        List<ItemStack> inputs = this.trade.getInput() == null ? new ArrayList<>() : this.trade.getInput();
        inv.setItem(SLOT_INPUT_1, !inputs.isEmpty() && inputs.get(0) != null ? inputs.get(0).clone() : new ItemStack(Material.AIR));
        inv.setItem(SLOT_INPUT_2, inputs.size() >= 2 && inputs.get(1) != null ? inputs.get(1).clone() : new ItemStack(Material.AIR));
        inv.setItem(SLOT_REWARD, this.trade.getReward() == null ? new ItemStack(Material.AIR) : this.trade.getReward().clone());
        inv.setItem(SLOT_TIMING, TradeTimingUtils.buildTimingItem(this.availableAfterMinutes));

        inv.setItem(SLOT_BACK, DefaultItems.BACK.build());
        inv.setItem(SLOT_VALIDATE, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aValider").build());

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() != event.getInventory() && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if(event.getClickedInventory() == event.getInventory()) {
            if(event.getSlot() == SLOT_BACK){
                event.setCancelled(true);

                previousInv.openInventory();
            } else if (event.getSlot() == SLOT_VALIDATE) {
                event.setCancelled(true);

                ItemStack item1 = event.getInventory().getItem(SLOT_INPUT_1);
                ItemStack item2 = event.getInventory().getItem(SLOT_INPUT_2);
                ItemStack result = event.getInventory().getItem(SLOT_REWARD);

                List<ItemStack> inputs = new ArrayList<>();
                if(item1 != null && item1.getType() != Material.AIR) inputs.add(item1.clone());
                if(item2 != null && item2.getType() != Material.AIR) inputs.add(item2.clone());

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
                this.trade.setReward(result.clone());
                this.trade.setAvailableAfterMinutes(this.availableAfterMinutes);

                this.previousInv.openInventory();
            } else if (event.getSlot() == SLOT_TIMING) {
                event.setCancelled(true);
                if(event.isRightClick()) {
                    this.openCustomTimingInput();
                } else {
                    this.availableAfterMinutes = TradeTimingUtils.nextPreset(this.availableAfterMinutes);
                    event.getInventory().setItem(SLOT_TIMING, TradeTimingUtils.buildTimingItem(this.availableAfterMinutes));
                }
            } else if (event.getSlot() != SLOT_INPUT_1 && event.getSlot() != SLOT_INPUT_2 && event.getSlot() != SLOT_REWARD) {
                event.setCancelled(true);
            }
        }
    }

    private void openCustomTimingInput() {
        TradeTimingChatInputListener.requestInput(
                this.player,
                this.availableAfterMinutes,
                minutes -> {
                    this.availableAfterMinutes = minutes;
                    this.openInventory();
                },
                this::openInventory
        );
        this.player.closeInventory();
    }

    @Override
    public void onInventoryOpen() {

    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {

    }

    @Override
    public void onInventoryDrag(InventoryDragEvent event) {
        for(int rawSlot : event.getRawSlots()){
            if(rawSlot >= event.getInventory().getSize()) continue;
            if(rawSlot != SLOT_INPUT_1 && rawSlot != SLOT_INPUT_2 && rawSlot != SLOT_REWARD){
                event.setCancelled(true);
                return;
            }
        }
    }
}
