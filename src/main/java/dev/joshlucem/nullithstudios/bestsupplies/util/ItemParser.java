package dev.joshlucem.nullithstudios.bestsupplies.util;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemParser {

    private ItemParser() {}

    /**
     * Parse an item string to ItemStack
     * Format: "MATERIAL:amount" or "MATERIAL:amount:customModelData"
     * Example: "DIAMOND:5" or "PAPER:1:1001"
     */
    public static ItemStack parseItem(String itemString) {
        if (itemString == null || itemString.isEmpty()) {
            return null;
        }

        String[] parts = itemString.split(":");
        if (parts.length < 1) {
            return null;
        }

        // Check for ItemsAdder format
        if (itemString.startsWith("IA:") && isItemsAdderAvailable()) {
            return parseItemsAdderItem(itemString);
        }

        // Parse material
        Material material;
        try {
            material = Material.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            BestSupplies.getInstance().getLogger().warning("Material inválido: " + parts[0]);
            return null;
        }

        // Parse amount (default 1)
        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        ItemStack item = new ItemStack(material, amount);

        // Parse custom model data (optional)
        if (parts.length >= 3) {
            try {
                int customModelData = Integer.parseInt(parts[2]);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(customModelData);
                    item.setItemMeta(meta);
                }
            } catch (NumberFormatException ignored) {}
        }

        return item;
    }

    /**
     * Parse multiple item strings to a list of ItemStacks
     */
    public static List<ItemStack> parseItems(List<String> itemStrings) {
        List<ItemStack> items = new ArrayList<>();
        if (itemStrings == null) {
            return items;
        }

        for (String itemString : itemStrings) {
            ItemStack item = parseItem(itemString);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Create an ItemStack with display name and lore
     */
    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        return createItem(material, 1, displayName, lore, null);
    }

    /**
     * Create an ItemStack with display name, lore, and placeholders
     */
    public static ItemStack createItem(Material material, String displayName, List<String> lore, Map<String, String> placeholders) {
        return createItem(material, 1, displayName, lore, placeholders);
    }

    /**
     * Create an ItemStack with all parameters
     * Automatically hides attributes/enchants and removes italic formatting
     */
    public static ItemStack createItem(Material material, int amount, String displayName, List<String> lore, Map<String, String> placeholders) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Hide all item flags (attributes, enchants, etc.)
            meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
            );
            
            // Set display name (remove italic)
            if (displayName != null) {
                String name = displayName;
                if (placeholders != null) {
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        name = name.replace(entry.getKey(), entry.getValue());
                    }
                }
                // Parse and remove italic decoration
                Component nameComponent = Text.parse(name).decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComponent);
            }

            // Set lore (remove italic from each line)
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    String processedLine = line;
                    if (placeholders != null) {
                        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                            processedLine = processedLine.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    // Parse and remove italic decoration
                    Component loreComponent = Text.parse(processedLine).decoration(TextDecoration.ITALIC, false);
                    loreComponents.add(loreComponent);
                }
                meta.lore(loreComponents);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a filler/decoration item (glass pane)
     * Automatically hides attributes and removes name
     */
    public static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            // Hide all item flags
            meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Serialize an ItemStack to a string for storage
     */
    public static String serializeItem(ItemStack item) {
        if (item == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name());
        sb.append(":").append(item.getAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            sb.append(":").append(meta.getCustomModelData());
        }

        return sb.toString();
    }

    /**
     * Check if ItemsAdder is available
     */
    private static boolean isItemsAdderAvailable() {
        return BestSupplies.getInstance().getServer().getPluginManager().isPluginEnabled("ItemsAdder")
                && BestSupplies.getInstance().getConfigManager().isItemsAdderEnabled();
    }

    /**
     * Parse ItemsAdder item
     * Format: "IA:namespace:item_id"
     */
    private static ItemStack parseItemsAdderItem(String itemString) {
        try {
            String[] parts = itemString.split(":", 3);
            if (parts.length < 3) {
                return null;
            }

            String namespacedId = parts[1] + ":" + parts[2];
            
            // Try to get ItemsAdder item using reflection
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, namespacedId);
            
            if (customStack != null) {
                return (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
            }
        } catch (Exception e) {
            BestSupplies.getInstance().debug("Error cargando item de ItemsAdder: " + itemString);
        }
        
        // Fallback to paper
        return new ItemStack(Material.PAPER, 1);
    }

    /**
     * Check if player has inventory space for items
     */
    public static boolean hasInventorySpace(org.bukkit.entity.Player player, List<ItemStack> items) {
        // Clone inventory to simulate adding items
        int emptySlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots >= items.size();
    }

    /**
     * Check if player has at least one empty slot
     */
    public static boolean hasAnyInventorySpace(org.bukkit.entity.Player player) {
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Give items to player, returns items that didn't fit
     */
    public static List<ItemStack> giveItems(org.bukkit.entity.Player player, List<ItemStack> items) {
        List<ItemStack> notGiven = new ArrayList<>();
        
        for (ItemStack item : items) {
            var leftover = player.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                notGiven.addAll(leftover.values());
            }
        }
        
        return notGiven;
    }
}
