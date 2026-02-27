package fr.tt54.protectYourCastle.inventories.trades;

import fr.tt54.protectYourCastle.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class TradeTimingUtils {

    private static final int PRESET_STEP_MINUTES = 20;
    private static final int PRESET_MAX_MINUTES = 120;

    private TradeTimingUtils() {
    }

    public static int sanitizeMinutes(int minutes) {
        return Math.max(0, minutes);
    }

    public static int nextPreset(int currentMinutes) {
        int current = sanitizeMinutes(currentMinutes);
        int roundedBase = (current / PRESET_STEP_MINUTES) * PRESET_STEP_MINUTES;
        int next = roundedBase + PRESET_STEP_MINUTES;
        return next > PRESET_MAX_MINUTES ? 0 : next;
    }

    public static String formatDelay(int minutes) {
        int sanitized = sanitizeMinutes(minutes);
        if(sanitized == 0){
            return "Tout de suite";
        }
        return "Apres " + sanitized + " min";
    }

    public static ItemStack buildTimingItem(int minutes) {
        int sanitized = sanitizeMinutes(minutes);
        return new ItemBuilder(Material.CLOCK, "§eArrivee du trade: §f" + formatDelay(sanitized))
                .setLore(
                        "§7Clic gauche: categories (0,20,40...)",
                        "§7Clic droit: saisir en minutes dans le chat",
                        "§8Valeur actuelle: §6" + formatDelay(sanitized)
                )
                .build();
    }
}
