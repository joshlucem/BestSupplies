package dev.joshlucem.nullithstudios.bestsupplies.listener;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChequeListener implements Listener {

    private final BestSupplies plugin;

    public ChequeListener(BestSupplies plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if this is a cheque item
        if (!plugin.getBankService().isChequeItem(item)) {
            return;
        }

        // Cancel the event to prevent placing blocks, etc.
        event.setCancelled(true);

        // Try to redeem the cheque
        plugin.getBankService().redeemCheque(player, item);
    }
}
