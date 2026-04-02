package dev.joshlucem.nullithstudios.bestsupplies.model;

import org.bukkit.Material;

import java.util.List;

public class DailyRewardDefinition {
    
    private final Material icon;
    private final double silverAmount;
    private final double goldAmount;
    private final List<String> items;
    private final List<String> commands;
    private final String displayName;
    private final List<String> description;

    public DailyRewardDefinition(Material icon, double silverAmount, double goldAmount, List<String> items,
                                  List<String> commands, String displayName, List<String> description) {
        this.icon = icon;
        this.silverAmount = silverAmount;
        this.goldAmount = goldAmount;
        this.items = items;
        this.commands = commands;
        this.displayName = displayName;
        this.description = description;
    }

    public Material getIcon() {
        return icon;
    }

    public double getSilverAmount() {
        return silverAmount;
    }

    public double getGoldAmount() {
        return goldAmount;
    }

    public List<String> getItems() {
        return items;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public boolean hasSilver() {
        return silverAmount > 0;
    }

    public boolean hasGold() {
        return goldAmount > 0;
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
}
