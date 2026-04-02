package dev.joshlucem.nullithstudios.bestsupplies.util;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemParser {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private ItemParser() {
    }

    public static ItemStack parseItem(String itemString) {
        if (itemString == null || itemString.isEmpty()) {
            return null;
        }

        String raw = itemString.trim();

        if (raw.startsWith("IA:")) {
            return parseItemsAdderNotation(raw);
        }

        String[] parts = raw.split(":");

        if (parts.length >= 1 && isMaterial(parts[0])) {
            return parseMaterialNotation(parts);
        }

        if (parts.length >= 2) {
            String namespacedId = parts[0] + ":" + parts[1];
            int amount = 1;
            if (parts.length >= 3) {
                try {
                    amount = Math.max(1, Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                }
            }

            ItemStack iaItem = getItemsAdderItem(namespacedId);
            if (iaItem != null) {
                iaItem.setAmount(amount);
                return iaItem;
            }
        }

        BestSupplies.getInstance().getLogger().warning("Formato de item invalido: " + itemString);
        return null;
    }

    private static ItemStack parseMaterialNotation(String[] parts) {
        Material material;
        try {
            material = Material.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            BestSupplies.getInstance().getLogger().warning("Material invalido: " + parts[0]);
            return null;
        }

        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack item = new ItemStack(material, amount);

        if (parts.length >= 3) {
            try {
                int customModelData = Integer.parseInt(parts[2]);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(customModelData);
                    item.setItemMeta(meta);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return item;
    }

    private static ItemStack parseItemsAdderNotation(String raw) {
        String[] parts = raw.split(":");
        if (parts.length < 3) {
            return null;
        }

        String namespacedId = parts[1] + ":" + parts[2];
        int amount = 1;
        if (parts.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[3]));
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack iaItem = getItemsAdderItem(namespacedId);
        if (iaItem != null) {
            iaItem.setAmount(amount);
            return iaItem;
        }

        return new ItemStack(Material.PAPER, amount);
    }

    private static boolean isMaterial(String value) {
        try {
            Material.valueOf(value.toUpperCase());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

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

    public static String describeItemString(String itemString) {
        if (itemString == null || itemString.isBlank()) {
            return "";
        }

        ItemStack item = parseItem(itemString);
        if (item == null) {
            return itemString;
        }

        String itemName = null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            itemName = PLAIN_TEXT.serialize(meta.displayName()).trim();
        }

        if (itemName == null || itemName.isBlank()) {
            itemName = BestSupplies.getInstance().getConfigManager().getItemName(item.getType());
        }

        return item.getAmount() + "x " + itemName;
    }

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        return createItem(material, 1, displayName, lore, null);
    }

    public static ItemStack createItem(Material material, String displayName, List<String> lore, Map<String, String> placeholders) {
        return createItem(material, 1, displayName, lore, placeholders);
    }

    public static ItemStack createItem(Material material, int amount, String displayName, List<String> lore, Map<String, String> placeholders) {
        return applyDisplay(new ItemStack(material, amount), displayName, lore, placeholders);
    }

    public static ItemStack createItem(ItemStack baseItem, String displayName, List<String> lore) {
        return createItem(baseItem, displayName, lore, null);
    }

    public static ItemStack createItem(ItemStack baseItem, String displayName, List<String> lore, Map<String, String> placeholders) {
        ItemStack item = baseItem == null ? new ItemStack(Material.PAPER) : baseItem.clone();
        return applyDisplay(item, displayName, lore, placeholders);
    }

    private static ItemStack applyDisplay(ItemStack item, String displayName, List<String> lore, Map<String, String> placeholders) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
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

            if (displayName != null) {
                String name = applyPlaceholders(displayName, placeholders);
                Component nameComponent = Text.parse(name).decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComponent);
            }

            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    String processedLine = applyPlaceholders(line, placeholders);
                    Component loreComponent = Text.parse(processedLine).decoration(TextDecoration.ITALIC, false);
                    loreComponents.add(loreComponent);
                }
                meta.lore(loreComponents);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return text;
        }

        String value = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue());
        }
        return value;
    }

    public static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
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

    private static boolean isItemsAdderAvailable() {
        return BestSupplies.getInstance().getServer().getPluginManager().isPluginEnabled("ItemsAdder")
                && BestSupplies.getInstance().getConfigManager().isItemsAdderEnabled();
    }

    public static ItemStack getItemsAdderItem(String namespacedId) {
        if (!isItemsAdderAvailable() || namespacedId == null || namespacedId.isBlank()) {
            return null;
        }

        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, namespacedId);

            if (customStack != null) {
                ItemStack stack = (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
                return stack != null ? stack.clone() : null;
            }
        } catch (Exception e) {
            BestSupplies.getInstance().debug("Error cargando item de ItemsAdder: " + namespacedId);
        }

        return null;
    }

    public static boolean hasInventorySpace(org.bukkit.entity.Player player, List<ItemStack> items) {
        int emptySlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots >= items.size();
    }

    public static boolean hasAnyInventorySpace(org.bukkit.entity.Player player) {
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

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
