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

    public void openHub(Player player) {
        closeCurrentGui(player);
        HubGui gui = new HubGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    public void openDaily(Player player) {
        closeCurrentGui(player);
        DailyGui gui = new DailyGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    public void openBank(Player player) {
        closeCurrentGui(player);
        BankGui gui = new BankGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    public void openFood(Player player) {
        closeCurrentGui(player);
        FoodGui gui = new FoodGui(plugin, player);
        registerGui(player, gui);
        gui.open();
        startUpdateTask(player, gui);
    }

    public void openStatus(Player player) {
        openHub(player);
    }

    public void openPending(Player player) {
        closeCurrentGui(player);
        PendingGui gui = new PendingGui(plugin, player);
        registerGui(player, gui);
        gui.open();
    }

    private void registerGui(Player player, BaseGui gui) {
        openGuis.put(player.getUniqueId(), gui);
    }

    public BaseGui getGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public BaseGui getGui(UUID uuid) {
        return openGuis.get(uuid);
    }

    public boolean hasGuiOpen(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    public void closeCurrentGui(Player player) {
        stopUpdateTask(player);
        openGuis.remove(player.getUniqueId());
    }

    public void handleClose(Player player, BaseGui closedGui) {
        BaseGui current = openGuis.get(player.getUniqueId());
        if (current != closedGui) {
            return;
        }
        stopUpdateTask(player);
        openGuis.remove(player.getUniqueId());
    }

    private void startUpdateTask(Player player, BaseGui gui) {
        stopUpdateTask(player);

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

    private void stopUpdateTask(Player player) {
        BukkitTask task = updateTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

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
