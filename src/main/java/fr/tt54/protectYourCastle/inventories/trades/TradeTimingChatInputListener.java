package fr.tt54.protectYourCastle.inventories.trades;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

public class TradeTimingChatInputListener implements Listener {

    private static final Map<UUID, PendingTradeTimingInput> pendingInputs = new ConcurrentHashMap<>();

    public static void requestInput(Player player, int currentMinutes, IntConsumer onInputAccepted, Runnable onInputCancelled) {
        pendingInputs.put(player.getUniqueId(), new PendingTradeTimingInput(onInputAccepted, onInputCancelled));
        player.sendMessage("§6[Castle] §fTiming actuel du trade: §e" + TradeTimingUtils.formatDelay(currentMinutes));
        player.sendMessage("§6[Castle] §fEntrez un nombre de minutes dans le chat (ex: 35) ou §cannuler");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingTradeTimingInput pendingInput = pendingInputs.get(player.getUniqueId());
        if(pendingInput == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage() == null ? "" : event.getMessage().trim();
        Bukkit.getScheduler().runTask(ProtectYourCastleMain.getInstance(), () -> this.handleChatInput(player, message, pendingInput));
    }

    private void handleChatInput(Player player, String message, PendingTradeTimingInput pendingInput) {
        PendingTradeTimingInput currentPendingInput = pendingInputs.get(player.getUniqueId());
        if(currentPendingInput != pendingInput) {
            return;
        }

        if(message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(player.getUniqueId());
            player.sendMessage("§7Saisie du timing annulee.");
            pendingInput.onInputCancelled.run();
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage("§cValeur invalide. Entrez un nombre entier en minutes, ou 'annuler'.");
            return;
        }

        if(minutes < 0) {
            player.sendMessage("§cLe temps doit etre >= 0 minute.");
            return;
        }

        pendingInputs.remove(player.getUniqueId());
        pendingInput.onInputAccepted.accept(minutes);
        player.sendMessage("§aLe trade sera disponible: §f" + TradeTimingUtils.formatDelay(minutes));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private static class PendingTradeTimingInput {

        private final IntConsumer onInputAccepted;
        private final Runnable onInputCancelled;

        private PendingTradeTimingInput(IntConsumer onInputAccepted, Runnable onInputCancelled) {
            this.onInputAccepted = onInputAccepted;
            this.onInputCancelled = onInputCancelled;
        }
    }
}
