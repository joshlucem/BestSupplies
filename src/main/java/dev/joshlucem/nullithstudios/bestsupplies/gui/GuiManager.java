package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private final BestSupplies plugin;
    private final Map<UUID, BaseGui> openGuis;
    private final Map<UUID, BukkitTask> updateTasks;

    public GuiManager(BestSupplies plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
        this.updateTasks = new HashMap<>();
    }

    /**
     * Open the main hub GUI
     */
    public void openHub(Player player) {
        closeCurrentGui(player);
        HubGui gui = new HubGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    /**
     * Open the daily rewards GUI
     */
    public void openDaily(Player player) {
        closeCurrentGui(player);
        DailyGui gui = new DailyGui(plugin, player);
        registerGui(player, gui);
        gui.open();
        startUpdateTask(player, gui);
    }

    /**
     * Open the bank GUI
     */
    public void openBank(Player player) {
        closeCurrentGui(player);
        BankGui gui = new BankGui(plugin, player);
        registerGui(player, gui);
        gui.open();
        startUpdateTask(player, gui);
    }

    /**
     * Open the food GUI
     */
    public void openFood(Player player) {
        closeCurrentGui(player);
        FoodGui gui = new FoodGui(plugin, player);
        registerGui(player, gui);
        gui.open();
        startUpdateTask(player, gui);
    }

    /**
     * Open the status GUI
     */
    public void openStatus(Player player) {
        closeCurrentGui(player);
        StatusGui gui = new StatusGui(plugin, player);
        registerGui(player, gui);
        gui.open();
        startUpdateTask(player, gui);
    }

    /**
     * Open the pending GUI
     */
    public void openPending(Player player) {
        closeCurrentGui(player);
        PendingGui gui = new PendingGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    /**
     * Register a GUI for a player
     */
    private void registerGui(Player player, BaseGui gui) {
        openGuis.put(player.getUniqueId(), gui);
    }

    /**
     * Get the current GUI for a player
     */
    public BaseGui getGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    /**
     * Get the current GUI for a player by UUID
     */
    public BaseGui getGui(UUID uuid) {
        return openGuis.get(uuid);
    }

    /**
     * Check if player has a GUI open
     */
    public boolean hasGuiOpen(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    /**
     * Close the current GUI for a player
     */
    public void closeCurrentGui(Player player) {
        stopUpdateTask(player);
        openGuis.remove(player.getUniqueId());
    }

    /**
     * Handle GUI close event
     */
    public void handleClose(Player player) {
        stopUpdateTask(player);
        openGuis.remove(player.getUniqueId());
    }

    /**
     * Start a countdown update task for a GUI
     */
    private void startUpdateTask(Player player, BaseGui gui) {
        stopUpdateTask(player); // Cancel any existing task

        int interval = plugin.getConfigManager().getGuiUpdateInterval();
        
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !hasGuiOpen(player)) {
                stopUpdateTask(player);
                return;
            }

            BaseGui currentGui = getGui(player);
            if (currentGui != null && currentGui == gui) {
                currentGui.updateCountdowns();
            } else {
                stopUpdateTask(player);
            }
        }, interval, interval);

        updateTasks.put(player.getUniqueId(), task);
    }

    /**
     * Stop the update task for a player
     */
    private void stopUpdateTask(Player player) {
        BukkitTask task = updateTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Shutdown - cancel all tasks
     */
    public void shutdown() {
        for (BukkitTask task : updateTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        updateTasks.clear();
        openGuis.clear();
    }
}
