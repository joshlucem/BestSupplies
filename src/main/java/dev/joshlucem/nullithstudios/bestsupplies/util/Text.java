package dev.joshlucem.nullithstudios.bestsupplies.util;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    // Pattern for hex colors like <#RRGGBB>
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    
    // Pattern for gradient <gradient:#color1:#color2>text</gradient>
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");

    private Text() {}

    /**
     * Parse a string with MiniMessage format and return an Adventure Component
     */
    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        try {
            return MINI_MESSAGE.deserialize(message);
        } catch (Exception e) {
            // Fallback to legacy color parsing
            return LEGACY_SERIALIZER.deserialize(parseLegacy(message));
        }
    }

    /**
     * Parse a string with MiniMessage format, replacing placeholders
     */
    public static Component parse(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        String processed = message;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processed = processed.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return parse(processed);
    }

    /**
     * Parse legacy color codes (&c, &l, etc.)
     */
    public static String parseLegacy(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Parse to legacy string (for inventory titles in older systems)
     */
    public static String parseToLegacy(String message) {
        if (message == null) return "";
        
        try {
            Component component = MINI_MESSAGE.deserialize(message);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            return parseLegacy(message);
        }
    }

    /**
     * Send a message to a player with MiniMessage formatting
     */
    public static void send(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        player.sendMessage(parse(message));
    }

    /**
     * Send a message to a player with placeholders
     */
    public static void send(Player player, String message, Map<String, String> placeholders) {
        if (player == null || message == null || message.isEmpty()) return;
        player.sendMessage(parse(message, placeholders));
    }

    /**
     * Send a prefixed message to a player
     */
    public static void sendPrefixed(Player player, String message, ConfigManager configManager) {
        if (player == null || message == null || message.isEmpty()) return;
        String prefix = configManager.getPrefix();
        player.sendMessage(parse(prefix + message));
    }

    /**
     * Send a prefixed message with placeholders
     */
    public static void sendPrefixed(Player player, String message, Map<String, String> placeholders, ConfigManager configManager) {
        if (player == null || message == null || message.isEmpty()) return;
        String prefix = configManager.getPrefix();
        player.sendMessage(parse(prefix + message, placeholders));
    }

    /**
     * Parse a list of strings with MiniMessage format
     */
    public static List<Component> parseList(List<String> messages) {
        List<Component> components = new ArrayList<>();
        if (messages != null) {
            for (String msg : messages) {
                components.add(parse(msg));
            }
        }
        return components;
    }

    /**
     * Parse a list of strings with placeholders
     */
    public static List<Component> parseList(List<String> messages, Map<String, String> placeholders) {
        List<Component> components = new ArrayList<>();
        if (messages != null) {
            for (String msg : messages) {
                components.add(parse(msg, placeholders));
            }
        }
        return components;
    }

    /**
     * Strip all color codes and formatting from a string
     */
    public static String stripColor(String message) {
        if (message == null) return "";
        // Remove MiniMessage tags
        String stripped = message.replaceAll("<[^>]+>", "");
        // Remove legacy color codes
        stripped = ChatColor.stripColor(parseLegacy(stripped));
        return stripped;
    }

    /**
     * Get the day name in Spanish
     */
    public static String getDayNameSpanish(java.time.DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    /**
     * Format a number as currency
     */
    public static String formatMoney(double amount) {
        if (amount == (long) amount) {
            return String.format("%,d", (long) amount);
        }
        return String.format("%,.2f", amount);
    }
}
