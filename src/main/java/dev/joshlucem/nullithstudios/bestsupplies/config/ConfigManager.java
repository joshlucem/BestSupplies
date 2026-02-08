package dev.joshlucem.nullithstudios.bestsupplies.config;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.FoodPackDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.StreakMilestone;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;

public class ConfigManager {

    private final BestSupplies plugin;
    
    private FileConfiguration config;
    private FileConfiguration dailyConfig;
    private FileConfiguration ranksConfig;
    private FileConfiguration messagesConfig;
    
    private ZoneId timezone;
    private DayOfWeek weeklyResetDay;
    private int weeklyResetHour;
    private int weeklyResetMinute;
    private int guiUpdateInterval;
    private boolean useChequeItem;
    private boolean debugEnabled;
    private boolean placeholderApiEnabled;
    private boolean itemsAdderEnabled;
    
    private Map<DayOfWeek, DailyRewardDefinition> dailyRewards;
    private Map<Integer, StreakMilestone> streakMilestones;
    private List<String> rankPriority;
    private Map<String, RankDefinition> ranks;
    private Map<String, String> itemTranslations;

    public ConfigManager(BestSupplies plugin) {
        this.plugin = plugin;
        this.itemTranslations = new HashMap<>();
    }

    public void loadAll() {
        loadConfig();
        loadDailyConfig();
        loadRanksConfig();
        loadMessagesConfig();
        loadItemTranslations();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        String tz = config.getString("timezone", "America/Lima");
        try {
            timezone = ZoneId.of(tz);
        } catch (Exception e) {
            plugin.getLogger().warning("Zona horaria inválida: " + tz + ". Usando America/Lima");
            timezone = ZoneId.of("America/Lima");
        }
        
        String resetDayStr = config.getString("weekly-reset.day", "MONDAY");
        try {
            weeklyResetDay = DayOfWeek.valueOf(resetDayStr.toUpperCase());
        } catch (Exception e) {
            weeklyResetDay = DayOfWeek.MONDAY;
        }
        weeklyResetHour = config.getInt("weekly-reset.hour", 0);
        weeklyResetMinute = config.getInt("weekly-reset.minute", 0);
        
        guiUpdateInterval = config.getInt("gui-update-interval", 20);
        useChequeItem = config.getBoolean("use-cheque-item", true);
        debugEnabled = config.getBoolean("debug", false);
        placeholderApiEnabled = config.getBoolean("enable-placeholderapi", true);
        itemsAdderEnabled = config.getBoolean("enable-itemsadder-icons", false);
    }

    private void loadDailyConfig() {
        File file = new File(plugin.getDataFolder(), "daily.yml");
        if (!file.exists()) {
            plugin.saveResource("daily.yml", false);
        }
        dailyConfig = YamlConfiguration.loadConfiguration(file);
        
        // Load default values
        InputStream defStream = plugin.getResource("daily.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            dailyConfig.setDefaults(defConfig);
        }
        
        dailyRewards = new HashMap<>();
        ConfigurationSection rewardsSection = dailyConfig.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String dayKey : rewardsSection.getKeys(false)) {
                try {
                    DayOfWeek day = DayOfWeek.valueOf(dayKey.toUpperCase());
                    ConfigurationSection daySection = rewardsSection.getConfigurationSection(dayKey);
                    if (daySection != null) {
                        DailyRewardDefinition reward = parseDailyReward(daySection);
                        dailyRewards.put(day, reward);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Día inválido en daily.yml: " + dayKey);
                }
            }
        }
        
        streakMilestones = new HashMap<>();
        ConfigurationSection milestonesSection = dailyConfig.getConfigurationSection("streak-milestones");
        if (milestonesSection != null) {
            for (String milestoneKey : milestonesSection.getKeys(false)) {
                try {
                    int streak = Integer.parseInt(milestoneKey);
                    ConfigurationSection ms = milestonesSection.getConfigurationSection(milestoneKey);
                    if (ms != null) {
                        StreakMilestone milestone = new StreakMilestone(
                                streak,
                                ms.getDouble("bonus-money", 0),
                                ms.getStringList("bonus-items"),
                                ms.getString("message", "")
                        );
                        streakMilestones.put(streak, milestone);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Milestone inválido en daily.yml: " + milestoneKey);
                }
            }
        }
    }

    private DailyRewardDefinition parseDailyReward(ConfigurationSection section) {
        String iconStr = section.getString("icon", "CHEST");
        Material icon;
        try {
            icon = Material.valueOf(iconStr.toUpperCase());
        } catch (Exception e) {
            icon = Material.CHEST;
        }
        
        double money = section.getDouble("money", 0);
        List<String> items = section.getStringList("items");
        List<String> commands = section.getStringList("commands");
        String displayName = section.getString("display-name", "Recompensa");
        List<String> description = section.getStringList("description");
        
        return new DailyRewardDefinition(icon, money, items, commands, displayName, description);
    }

    private void loadRanksConfig() {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(file);
        
        InputStream defStream = plugin.getResource("ranks.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            ranksConfig.setDefaults(defConfig);
        }
        
        rankPriority = ranksConfig.getStringList("priority");
        
        ranks = new HashMap<>();
        ConfigurationSection ranksSection = ranksConfig.getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rankId : ranksSection.getKeys(false)) {
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankId);
                if (rankSection != null) {
                    RankDefinition rank = parseRank(rankId, rankSection);
                    ranks.put(rankId, rank);
                }
            }
        }
    }

    private RankDefinition parseRank(String id, ConfigurationSection section) {
        // Support both single permission and multiple permissions
        List<String> permissions = new ArrayList<>();
        
        // Check for 'permissions' list first
        if (section.contains("permissions")) {
            permissions.addAll(section.getStringList("permissions"));
        }
        // Fallback to single 'permission' string
        String singlePermission = section.getString("permission", "");
        if (!singlePermission.isEmpty() && !permissions.contains(singlePermission)) {
            permissions.add(singlePermission);
        }
        
        String displayName = section.getString("display-name", id);
        double weeklyMoney = section.getDouble("weekly-money", 0);
        String foodCooldownStr = section.getString("food-cooldown", "24h");
        long foodCooldownMs = parseDuration(foodCooldownStr);
        
        Map<String, FoodPackDefinition> packs = new HashMap<>();
        ConfigurationSection packsSection = section.getConfigurationSection("packs");
        if (packsSection != null) {
            for (String packId : packsSection.getKeys(false)) {
                ConfigurationSection packSection = packsSection.getConfigurationSection(packId);
                if (packSection != null) {
                    FoodPackDefinition pack = parseFoodPack(packId, packSection);
                    packs.put(packId, pack);
                }
            }
        }
        
        return new RankDefinition(id, permissions, displayName, weeklyMoney, foodCooldownMs, packs);
    }

    private FoodPackDefinition parseFoodPack(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        String iconStr = section.getString("icon", "BREAD");
        Material icon;
        try {
            icon = Material.valueOf(iconStr.toUpperCase());
        } catch (Exception e) {
            icon = Material.BREAD;
        }
        List<String> items = section.getStringList("items");
        
        return new FoodPackDefinition(id, displayName, icon, items);
    }

    private long parseDuration(String duration) {
        // Parse duration like "24h", "18h", "12h30m"
        long totalMs = 0;
        String remaining = duration.toLowerCase().trim();
        
        // Days
        int dIdx = remaining.indexOf('d');
        if (dIdx > 0) {
            try {
                int days = Integer.parseInt(remaining.substring(0, dIdx));
                totalMs += days * 24L * 60L * 60L * 1000L;
            } catch (NumberFormatException ignored) {}
            remaining = remaining.substring(dIdx + 1);
        }
        
        // Hours
        int hIdx = remaining.indexOf('h');
        if (hIdx > 0) {
            try {
                int hours = Integer.parseInt(remaining.substring(0, hIdx));
                totalMs += hours * 60L * 60L * 1000L;
            } catch (NumberFormatException ignored) {}
            remaining = remaining.substring(hIdx + 1);
        }
        
        // Minutes
        int mIdx = remaining.indexOf('m');
        if (mIdx > 0) {
            try {
                int minutes = Integer.parseInt(remaining.substring(0, mIdx));
                totalMs += minutes * 60L * 1000L;
            } catch (NumberFormatException ignored) {}
            remaining = remaining.substring(mIdx + 1);
        }
        
        // Seconds
        int sIdx = remaining.indexOf('s');
        if (sIdx > 0) {
            try {
                int seconds = Integer.parseInt(remaining.substring(0, sIdx));
                totalMs += seconds * 1000L;
            } catch (NumberFormatException ignored) {}
        }
        
        // Default to 24 hours if parsing fails
        if (totalMs == 0) {
            totalMs = 24L * 60L * 60L * 1000L;
        }
        
        return totalMs;
    }

    private void loadMessagesConfig() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
        
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defConfig);
        }
    }

    private void loadItemTranslations() {
        File file = new File(plugin.getDataFolder(), "messages-items.yml");
        if (!file.exists()) {
            plugin.saveResource("messages-items.yml", false);
        }
        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(file);
        
        itemTranslations = new HashMap<>();
        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String translation = itemsSection.getString(key);
                if (translation != null) {
                    itemTranslations.put(key.toUpperCase(), translation);
                }
            }
        }
        plugin.debug("Cargadas " + itemTranslations.size() + " traducciones de items");
    }

    /**
     * Get translated item name for a material
     * Returns the material name formatted if no translation exists
     */
    public String getItemName(String materialName) {
        if (materialName == null) return "";
        String upper = materialName.toUpperCase();
        if (itemTranslations.containsKey(upper)) {
            return itemTranslations.get(upper);
        }
        // Fallback: format material name (COOKED_BEEF -> Cooked Beef)
        return formatMaterialName(upper);
    }

    /**
     * Get translated item name for a Material
     */
    public String getItemName(Material material) {
        if (material == null) return "";
        return getItemName(material.name());
    }

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "<red>Mensaje no encontrado: " + path + "</red>");
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }

    public List<String> getMessageList(String path) {
        return messagesConfig.getStringList(path);
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    // Config getters
    public ZoneId getTimezone() {
        return timezone;
    }

    public DayOfWeek getWeeklyResetDay() {
        return weeklyResetDay;
    }

    public int getWeeklyResetHour() {
        return weeklyResetHour;
    }

    public int getWeeklyResetMinute() {
        return weeklyResetMinute;
    }

    public int getGuiUpdateInterval() {
        return guiUpdateInterval;
    }

    public boolean useChequeItem() {
        return useChequeItem;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public boolean isItemsAdderEnabled() {
        return itemsAdderEnabled;
    }

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getMysqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("database.mysql.database", "bestsupplies");
    }

    public String getMysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("database.mysql.password", "");
    }

    public int getMysqlPoolSize() {
        return config.getInt("database.mysql.pool-size", 10);
    }

    public String getGuiTitle(String guiName) {
        return config.getString("gui-titles." + guiName, guiName);
    }

    public int getGuiSlot(String guiName, String slotName) {
        return config.getInt("gui-slots." + guiName + "." + slotName, -1);
    }

    public Material getDecorationFiller() {
        String mat = config.getString("decoration.filler", "WHITE_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(mat.toUpperCase());
        } catch (Exception e) {
            return Material.WHITE_STAINED_GLASS_PANE;
        }
    }

    public Material getDecorationBorder() {
        String mat = config.getString("decoration.border", "GRAY_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(mat.toUpperCase());
        } catch (Exception e) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
    }

    // Daily rewards getters
    public Map<DayOfWeek, DailyRewardDefinition> getDailyRewards() {
        return dailyRewards;
    }

    public DailyRewardDefinition getDailyReward(DayOfWeek day) {
        return dailyRewards.get(day);
    }

    public Map<Integer, StreakMilestone> getStreakMilestones() {
        return streakMilestones;
    }

    public StreakMilestone getStreakMilestone(int streak) {
        return streakMilestones.get(streak);
    }

    // Ranks getters
    public List<String> getRankPriority() {
        return rankPriority;
    }

    public Map<String, RankDefinition> getRanks() {
        return ranks;
    }

    public RankDefinition getRank(String rankId) {
        return ranks.get(rankId);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getDailyConfig() {
        return dailyConfig;
    }

    public FileConfiguration getRanksConfig() {
        return ranksConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
