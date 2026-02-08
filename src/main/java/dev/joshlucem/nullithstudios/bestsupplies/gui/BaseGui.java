package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseGui implements InventoryHolder {

    protected final BestSupplies plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, ClickAction> clickActions;

    public BaseGui(BestSupplies plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.clickActions = new HashMap<>();
    }

    /**
     * Initialize and build the GUI
     */
    protected void init(String title, int rows) {
        Component titleComponent = Text.parse(title);
        this.inventory = Bukkit.createInventory(this, rows * 9, titleComponent);
        build();
    }

    /**
     * Build the GUI contents
     */
    protected abstract void build();

    /**
     * Update countdown displays (called periodically)
     */
    public void updateCountdowns() {
        // Override in subclasses that need countdown updates
    }

    /**
     * Handle click events
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        ClickAction action = clickActions.get(slot);
        if (action != null) {
            action.execute(event);
        }
    }

    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Set an item in a slot with a click action
     */
    protected void setItem(int slot, ItemStack item, ClickAction action) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
            if (action != null) {
                clickActions.put(slot, action);
            }
        }
    }

    /**
     * Set an item in a slot without a click action
     */
    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    /**
     * Fill empty slots with decoration
     */
    protected void fillEmpty(Material material) {
        ItemStack filler = ItemParser.createFiller(material);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * Fill border with decoration
     */
    protected void fillBorder(Material material) {
        ItemStack border = ItemParser.createFiller(material);
        int size = inventory.getSize();
        int rows = size / 9;

        // Top row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }

        // Bottom row
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, border);
        }

        // Left and right columns
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    /**
     * Create a back button
     */
    protected ItemStack createBackButton() {
        return ItemParser.createItem(
            Material.ARROW,
            plugin.getConfigManager().getMessage("gui.back-item"),
            null
        );
    }

    /**
     * Create a close button
     */
    protected ItemStack createCloseButton() {
        return ItemParser.createItem(
            Material.BARRIER,
            plugin.getConfigManager().getMessage("gui.close-item"),
            null
        );
    }

    /**
     * Get slot from config or use default
     */
    protected int getSlot(String guiName, String slotName, int defaultSlot) {
        int slot = plugin.getConfigManager().getGuiSlot(guiName, slotName);
        return slot >= 0 ? slot : defaultSlot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Functional interface for click actions
     */
    @FunctionalInterface
    public interface ClickAction {
        void execute(InventoryClickEvent event);
    }
}
