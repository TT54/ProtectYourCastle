package fr.tt54.protectYourCastle.inventories.traders.weapons;

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

public class WeaponInventory extends CorePersonalInventory {

    private final boolean editing;
    private final Trader.GameWeapon displayedWeapon;
    private final List<Trader.GameWeapon> bundle;
    private final CorePersonalInventory previousInventory;

    private boolean overpoweredWeapon;

    public WeaponInventory(Player player, boolean editing, Trader.GameWeapon displayedWeapon, List<Trader.GameWeapon> bundle, CorePersonalInventory previousInventory) {
        super(editing ? "Edition d'une arme" : "Ajout d'une arme", player);
        this.editing = editing;
        this.displayedWeapon = displayedWeapon;
        this.bundle = bundle;
        this.previousInventory = previousInventory;
        this.overpoweredWeapon = displayedWeapon.isOverPowered();
    }

    public WeaponInventory(Player player, List<Trader.GameWeapon> bundle, CorePersonalInventory previousInventory) {
        this(player, false, new Trader.GameWeapon(new Trader.NPCTrade(new ArrayList<>(), null), new Trader.NPCTrade(new ArrayList<>(), null), false), bundle, previousInventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inv = createBaseInventory(5);

        inv.setItem(9 + 2, new ItemBuilder(Material.BOW, "§aArme").build());
        inv.setItem(3 * 9 + 2, new ItemBuilder(Material.ARROW, "§aMunitions").build());

        inv.setItem(9 + 4, this.displayedWeapon.getGunTrade().getInput().isEmpty() ? DefaultItems.AIR.build() : this.displayedWeapon.getGunTrade().getInput().get(0).clone());
        inv.setItem(9 + 5, this.displayedWeapon.getGunTrade().getInput().size() < 2 ? DefaultItems.AIR.build() : this.displayedWeapon.getGunTrade().getInput().get(1).clone());
        inv.setItem(9 + 7, this.displayedWeapon.getGunTrade().getReward() == null ? DefaultItems.AIR.build() : this.displayedWeapon.getGunTrade().getReward().clone());

        inv.setItem(2 * 9 + 4, this.overpoweredWeapon ? new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aArme OP").build() : new ItemBuilder(Material.RED_STAINED_GLASS_PANE, "§cArme non OP").build());

        inv.setItem(3 * 9 + 4, this.displayedWeapon.getAmmoTrade().getInput().isEmpty() ? DefaultItems.AIR.build() : this.displayedWeapon.getAmmoTrade().getInput().get(0).clone());
        inv.setItem(3 * 9 + 5, this.displayedWeapon.getAmmoTrade().getInput().size() < 2 ? DefaultItems.AIR.build() : this.displayedWeapon.getAmmoTrade().getInput().get(1).clone());
        inv.setItem(3 * 9 + 7, this.displayedWeapon.getAmmoTrade().getReward() == null ? DefaultItems.AIR.build() : this.displayedWeapon.getAmmoTrade().getReward().clone());

        inv.setItem(9 * 4 + 8, new ItemBuilder(Material.LIME_WOOL, "§aValider").build());
        inv.setItem(9 * 4, DefaultItems.BACK.build());

        return inv;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == event.getInventory()){
            int slot = event.getSlot();
            ItemStack is = event.getCurrentItem();
            if(is != null && is.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                event.setCancelled(true);
                return;
            }

            if(slot != 9 + 4 && slot != 9 + 5 && slot != 9 + 7 &&
               slot != 3 * 9 + 4 && slot != 3 * 9 + 5 && slot != 3 * 9 + 7){
                event.setCancelled(true);
            }

            if(slot == 9 * 4 + 8){
                List<ItemStack> gunInputs = new ArrayList<>();
                ItemStack gunInput1 = event.getInventory().getItem(9 + 4);
                ItemStack gunInput2 = event.getInventory().getItem(9 + 5);
                ItemStack gunReward = event.getInventory().getItem(9 + 7);

                List<ItemStack> ammoInputs = new ArrayList<>();
                ItemStack ammoInput1 = event.getInventory().getItem(3 * 9 + 4);
                ItemStack ammoInput2 = event.getInventory().getItem(3 * 9 + 5);
                ItemStack ammoReward = event.getInventory().getItem(3 * 9 + 7);

                if(gunInput1 != null && !gunInput1.getType().isAir())
                    gunInputs.add(gunInput1.clone());
                if(gunInput2 != null && !gunInput2.getType().isAir())
                    gunInputs.add(gunInput2.clone());

                if(ammoInput1 != null && !ammoInput1.getType().isAir())
                    ammoInputs.add(ammoInput1.clone());
                if(ammoInput2 != null && !ammoInput2.getType().isAir())
                    ammoInputs.add(ammoInput2.clone());

                if(gunInputs.isEmpty()){
                    player.sendMessage("§cVous devez définir un prix pour l'arme !");
                    return;
                } else if(gunReward == null || gunReward.getType().isAir()){
                    bundle.remove(this.displayedWeapon);
                    this.previousInventory.openInventory();
                    player.sendMessage("§aL'arme a bien été retirée");
                    return;
                } else if(ammoInputs.isEmpty()){
                    player.sendMessage("§cVous devez définir un prix pour les munitions !");
                    return;
                } else if(ammoReward == null || ammoReward.getType().isAir()){
                    player.sendMessage("§cVous devez définir une munition !");
                    return;
                }

                if(this.editing){
                    this.displayedWeapon.getGunTrade().setInput(gunInputs);
                    this.displayedWeapon.getGunTrade().setReward(gunReward.clone());
                    this.displayedWeapon.getAmmoTrade().setInput(ammoInputs);
                    this.displayedWeapon.getAmmoTrade().setReward(ammoReward.clone());
                    this.displayedWeapon.setOverPowered(this.overpoweredWeapon);
                    player.sendMessage("§aL'arme a bien été modifiée !");
                    this.previousInventory.openInventory();
                } else{
                    bundle.add(new Trader.GameWeapon(
                            new Trader.NPCTrade(gunInputs, gunReward.clone()),
                            new Trader.NPCTrade(ammoInputs, ammoReward.clone()),
                            this.overpoweredWeapon
                    ));
                    player.sendMessage("§aL'arme a bien été ajoutée !");
                    this.openInventory();
                }
            } else if(slot == 9 * 4) {
                this.previousInventory.openInventory();
            } else if(slot == 2 * 9 + 4) {
                this.overpoweredWeapon = !this.overpoweredWeapon;
                event.getInventory().setItem(2 * 9 + 4, this.overpoweredWeapon ? new ItemBuilder(Material.LIME_STAINED_GLASS_PANE, "§aArme OP").build() : new ItemBuilder(Material.RED_STAINED_GLASS_PANE, "§cArme non OP").build());
            } else if(slot == 9 + 7){
                // On pré-rempli les autres slots avec les armes déjà créées
                ItemStack newGun = event.getCursor() == null ? null : event.getCursor().clone();
                if(newGun != null && newGun.getType() != Material.AIR){
                    boolean found = false;
                    for(List<Trader.GameWeapon> bundle : Trader.weapons){
                        for(Trader.GameWeapon weapon : bundle){
                            if(newGun.getType() == weapon.getGunTrade().getReward().getType()){
                                event.getInventory().setItem(9 + 4, !weapon.getGunTrade().getInput().isEmpty() ? weapon.getGunTrade().getInput().get(0).clone() : DefaultItems.AIR.build());
                                event.getInventory().setItem(9 + 5, weapon.getGunTrade().getInput().size() > 1 ? weapon.getGunTrade().getInput().get(1).clone() : DefaultItems.AIR.build());

                                event.getInventory().setItem(9 * 3 + 4, !weapon.getAmmoTrade().getInput().isEmpty() ? weapon.getAmmoTrade().getInput().get(0).clone() : DefaultItems.AIR.build());
                                event.getInventory().setItem(9 * 3 + 5, weapon.getAmmoTrade().getInput().size() > 1 ? weapon.getAmmoTrade().getInput().get(1).clone() : DefaultItems.AIR.build());
                                event.getInventory().setItem(9 * 3 + 7, weapon.getAmmoTrade().getReward() != null ? weapon.getAmmoTrade().getReward().clone() : DefaultItems.AIR.build());
                                found = true;
                                break;
                            }
                        }
                    }
                    player.sendMessage(found ? "§aPrix pré-rempli depuis une arme existante !" : "§cAucune arme existante ne correspond à cet objet, les prix ne seront pas pré-remplis.");
                }
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
        event.setCancelled(true);
    }
}
