package fr.tt54.protectYourCastle.inventories;

import fr.tt54.protectYourCastle.utils.DefaultItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class CorePersonalInventory {

    protected final Player player;
    private final String title;
    private Inventory openedInventory;

    public CorePersonalInventory(String title, Player player) {
        this.title = title;
        this.player = player;
    }

    public Inventory createBaseInventory(int rows) {
        return createBaseInventory(this.getTitle(), rows);
    }

    public static Inventory createBaseInventory(String name, int rows) {
        Inventory inventory = Bukkit.createInventory(null, 9 * rows, name);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, DefaultItems.GRAY_GLASS_PANE.build());
        }

        return inventory;
    }

    public static List<String> splitString(String entry, int charsPerLine) {
        List<String> list = new ArrayList<>();

        String[] words = entry.split(" ");

        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() < charsPerLine) {
                line.append(word);
                line.append(" ");
            } else {
                list.add(line.substring(0, line.length() - 1));
                line = new StringBuilder(word);
                line.append(" ");
            }
        }
        list.add(line.toString());

        return list;
    }

    public static List<String> splitString(String entry, int charsPerLine, String linePrefix) {
        List<String> list = new ArrayList<>();

        String[] words = entry.split(" ");

        StringBuilder line = new StringBuilder(linePrefix);
        for (String word : words) {
            if (line.length() < charsPerLine) {
                line.append(word);
                line.append(" ");
            } else {
                list.add(line.substring(0, line.length() - 1));
                line = new StringBuilder(linePrefix + word);
                line.append(" ");
            }
        }
        list.add(line.toString());

        return list;
    }

    @NotNull
    public abstract Inventory getInventory();

    public Inventory openInventory() {
        Inventory inv = this.getInventory();
        player.openInventory(inv);
        this.openedInventory = inv;
        CoreInventoryListener.openInv(player, this);
        this.onInventoryOpen();
        return inv;
    }

    public Inventory reopenInventory() {
        Inventory inv = this.openedInventory != null ? this.openedInventory : this.getInventory();
        player.openInventory(inv);
        this.openedInventory = inv;
        CoreInventoryListener.openInv(player, this);
        this.onInventoryOpen();
        return inv;
    }

    public Inventory getOpenedInventory() {
        return this.openedInventory;
    }

    public abstract void onInventoryClick(InventoryClickEvent event);

    public abstract void onInventoryOpen();

    public abstract void onInventoryClose(InventoryCloseEvent event);

    public abstract void onInventoryDrag(InventoryDragEvent event);

    public String getTitle() {
        return title;
    }
}
