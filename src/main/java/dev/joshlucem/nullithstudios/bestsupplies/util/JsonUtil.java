package dev.joshlucem.nullithstudios.bestsupplies.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {}

    /**
     * Serialize items to JSON string for storage
     */
    public static String serializeItems(List<ItemStack> items) {
        JsonArray array = new JsonArray();
        
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                JsonObject obj = new JsonObject();
                obj.addProperty("material", item.getType().name());
                obj.addProperty("amount", item.getAmount());
                
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (meta.hasCustomModelData()) {
                        obj.addProperty("customModelData", meta.getCustomModelData());
                    }
                    if (meta.hasDisplayName()) {
                        // Store as plain text for simplicity
                        obj.addProperty("displayName", Text.stripColor(meta.displayName().toString()));
                    }
                }
                
                array.add(obj);
            }
        }
        
        return GSON.toJson(array);
    }

    /**
     * Deserialize items from JSON string
     */
    public static List<ItemStack> deserializeItems(String json) {
        List<ItemStack> items = new ArrayList<>();
        
        if (json == null || json.isEmpty()) {
            return items;
        }
        
        try {
            JsonArray array = GSON.fromJson(json, JsonArray.class);
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                Material material;
                try {
                    material = Material.valueOf(obj.get("material").getAsString());
                } catch (Exception e) {
                    continue;
                }
                
                int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1;
                
                ItemStack item = new ItemStack(material, amount);
                
                if (obj.has("customModelData")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(obj.get("customModelData").getAsInt());
                        item.setItemMeta(meta);
                    }
                }
                
                items.add(item);
            }
        } catch (Exception e) {
            // Return empty list on parse error
        }
        
        return items;
    }

    /**
     * Serialize a cheque to JSON for pending storage
     */
    public static String serializeCheque(String chequeId, double amount, String weekKey) {
        JsonObject obj = new JsonObject();
        obj.addProperty("chequeId", chequeId);
        obj.addProperty("amount", amount);
        obj.addProperty("weekKey", weekKey);
        return GSON.toJson(obj);
    }

    /**
     * Deserialize cheque data from JSON
     */
    public static ChequePayload deserializeCheque(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            return new ChequePayload(
                obj.get("chequeId").getAsString(),
                obj.get("amount").getAsDouble(),
                obj.get("weekKey").getAsString()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simple cheque payload record
     */
    public record ChequePayload(String chequeId, double amount, String weekKey) {}

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Parse JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }
}
