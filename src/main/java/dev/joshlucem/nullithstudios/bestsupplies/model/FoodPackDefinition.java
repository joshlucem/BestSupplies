package dev.joshlucem.nullithstudios.bestsupplies.model;

import org.bukkit.Material;

import java.util.List;

public class FoodPackDefinition {
    
    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> items;

    public FoodPackDefinition(String id, String displayName, Material icon, List<String> items) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getItems() {
        return items;
    }
}
