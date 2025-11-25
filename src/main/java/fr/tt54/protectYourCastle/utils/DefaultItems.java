package fr.tt54.protectYourCastle.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public class DefaultItems {

    public static final ItemBuilder BACK_ARROW = new ItemBuilder(Material.ARROW, 1, "§7Page précédente");
    public static final ItemBuilder NEXT_ARROW = new ItemBuilder(Material.ARROW, 1, "§7Page Suivante");

    public static final ItemBuilder GRAY_GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE, 1, " ");
    public static final ItemBuilder WHITE_GLASS_PANE = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE, 1, " ");

    public static final ItemBuilder BUY = new ItemBuilder(Material.DIAMOND, 1, "§2Acheter");
    public static final ItemBuilder SELL = new ItemBuilder(Material.CHEST, 1, "§2Vendre");

    public static final ItemBuilder BACK = new ItemBuilder(Material.ARROW, 1, "§cRetour");

    public static final ItemBuilder AIR = new ItemBuilder(Material.AIR);

    public static ItemBuilder pageItem = (new ItemBuilder(Material.SUNFLOWER, 1, "§ePage §6%page%"));
}
