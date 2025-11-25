package fr.tt54.protectYourCastle.inventories;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoreInventoryListener implements Listener {

    private static final Map<UUID, CorePersonalInventory> openedInv = new HashMap<>();

    static {
        Bukkit.getPluginManager().registerEvents(new CoreInventoryListener(), ProtectYourCastleMain.getInstance());
    }

    public static void openInv(Player player, CorePersonalInventory coreInventory) {
        openedInv.put(player.getUniqueId(), coreInventory);
    }

    public static void closeInv(Player player) {
        openedInv.remove(player.getUniqueId());
    }


    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        CorePersonalInventory inv = openedInv.get(event.getWhoClicked().getUniqueId());
        if (inv != null && inv.getTitle().equalsIgnoreCase(event.getView().getTitle())) {
            inv.onInventoryClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        CorePersonalInventory inv = openedInv.get(event.getWhoClicked().getUniqueId());
        if (inv != null && inv.getTitle().equalsIgnoreCase(event.getView().getTitle())) {
            inv.onInventoryDrag(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        CorePersonalInventory inv = openedInv.get(event.getPlayer().getUniqueId());
        if (inv != null && inv.getTitle().equalsIgnoreCase(event.getView().getTitle())) {
            closeInv((Player) event.getPlayer());
            inv.onInventoryClose(event);
        }
    }
}
