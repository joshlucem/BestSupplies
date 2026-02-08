package dev.joshlucem.nullithstudios.bestsupplies.listener;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final BestSupplies plugin;

    public PlayerListener(BestSupplies plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Schedule async check for streak
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Check and update streak
                plugin.getDailyService().checkAndUpdateStreak(player);

                // Notify if there are pending deliveries
                int pendingCount = plugin.getPendingService().getPendingCount(player);
                if (pendingCount > 0) {
                    var placeholders = java.util.Map.of("%count%", String.valueOf(pendingCount));
                    dev.joshlucem.nullithstudios.bestsupplies.util.Text.sendPrefixed(
                        player,
                        plugin.getConfigManager().getMessage("pending.items-pending", placeholders),
                        plugin.getConfigManager()
                    );
                }
            }
        }, 40L); // 2 seconds delay
    }
}
