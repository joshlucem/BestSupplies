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

import java.util.ArrayList;
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

    protected void init(String title, int rows) {
        Component titleComponent = Text.parse(title);
        this.inventory = Bukkit.createInventory(this, rows * 9, titleComponent);
        rebuild();
    }

    protected final void rebuild() {
        clickActions.clear();
        if (inventory != null) {
            inventory.clear();
        }
        build();
    }

    protected abstract void build();

    public void updateCountdowns() {
    }

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

    public void open() {
        player.openInventory(inventory);
    }

    protected void setItem(int slot, ItemStack item, ClickAction action) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
            if (action != null) {
                clickActions.put(slot, action);
            } else {
                clickActions.remove(slot);
            }
        }
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    protected void setSlots(int[] slots, ItemStack item) {
        for (int slot : slots) {
            setItem(slot, item);
        }
    }

    protected void fillEmpty(Material material) {
        ItemStack filler = ItemParser.createFiller(material);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected void fillBorder(Material material) {
        ItemStack border = ItemParser.createFiller(material);
        int size = inventory.getSize();
        int rows = size / 9;

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }

        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, border);
        }

        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    protected ItemStack createBackButton() {
        return ItemParser.createItem(
                Material.ARROW,
                plugin.getConfigManager().getMessage("gui.back-item"),
                null
        );
    }

    protected ItemStack createCloseButton() {
        return ItemParser.createItem(
                Material.BARRIER,
                plugin.getConfigManager().getMessage("gui.close-item"),
                null
        );
    }

    protected ItemStack createIconItem(String namespacedId,
                                       Material fallback,
                                       String displayName,
                                       List<String> lore,
                                       Map<String, String> placeholders) {
        ItemStack base = ItemParser.getItemsAdderItem(namespacedId);
        if (base == null) {
            base = new ItemStack(fallback);
        }
        return ItemParser.createItem(base, displayName, lore, placeholders);
    }

    protected ItemStack createIconItem(String namespacedId,
                                       Material fallback,
                                       String displayName,
                                       List<String> lore) {
        return createIconItem(namespacedId, fallback, displayName, lore, null);
    }

    protected List<String> getMessageList(String path, Map<String, String> placeholders) {
        List<String> lines = plugin.getConfigManager().getMessageList(path);
        if (lines.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return lines;
        }

        List<String> processed = new ArrayList<>(lines.size());
        for (String line : lines) {
            String value = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                value = value.replace(entry.getKey(), entry.getValue());
            }
            processed.add(value);
        }
        return processed;
    }

    protected void sendNoPermission() {
        Text.sendPrefixed(
                player,
                plugin.getConfigManager().getMessage("general.no-permission"),
                plugin.getConfigManager()
        );
    }

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

    @FunctionalInterface
    public interface ClickAction {
        void execute(InventoryClickEvent event);
    }
}
