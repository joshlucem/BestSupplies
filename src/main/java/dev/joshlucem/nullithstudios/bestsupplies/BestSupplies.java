package dev.joshlucem.nullithstudios.bestsupplies;

import dev.joshlucem.nullithstudios.bestsupplies.command.SuppliesCommand;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.gui.GuiManager;
import dev.joshlucem.nullithstudios.bestsupplies.listener.ChequeListener;
import dev.joshlucem.nullithstudios.bestsupplies.listener.GuiListener;
import dev.joshlucem.nullithstudios.bestsupplies.listener.PlayerListener;
import dev.joshlucem.nullithstudios.bestsupplies.service.*;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.storage.MysqlDatabase;
import dev.joshlucem.nullithstudios.bestsupplies.storage.SqliteDatabase;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class BestSupplies extends JavaPlugin {

    private static BestSupplies instance;
    
    private ConfigManager configManager;
    private Database database;
    private Economy economy;
    
    private TimeService timeService;
    private RankService rankService;
    private RewardService rewardService;
    private DailyService dailyService;
    private BankService bankService;
    private FoodService foodService;
    private PendingService pendingService;
    
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load configuration
        configManager = new ConfigManager(this);
        configManager.loadAll();
        
        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault no encontrado o sin economía. El plugin no puede funcionar.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize database
        if (!initializeDatabase()) {
            getLogger().severe("Error al inicializar la base de datos. El plugin no puede funcionar.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize services
        initializeServices();
        
        // Initialize GUI manager
        guiManager = new GuiManager(this);
        
        // Register commands
        SuppliesCommand suppliesCommand = new SuppliesCommand(this);
        getCommand("supplies").setExecutor(suppliesCommand);
        getCommand("supplies").setTabCompleter(suppliesCommand);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ChequeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") 
                && configManager.isPlaceholderApiEnabled()) {
            new BestSuppliesPlaceholders(this).register();
            getLogger().info("PlaceholderAPI detectado y expansión registrada.");
        }
        
        getLogger().info("BestSupplies habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        // Close GUI update tasks
        if (guiManager != null) {
            guiManager.shutdown();
        }
        
        // Close database connection
        if (database != null) {
            database.close();
        }
        
        getLogger().info("BestSupplies deshabilitado.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean initializeDatabase() {
        try {
            String dbType = configManager.getDatabaseType();
            if ("mysql".equalsIgnoreCase(dbType)) {
                database = new MysqlDatabase(this, configManager);
            } else {
                database = new SqliteDatabase(this);
            }
            database.initialize();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error inicializando base de datos", e);
            return false;
        }
    }

    private void initializeServices() {
        timeService = new TimeService(configManager);
        rankService = new RankService(this, configManager);
        pendingService = new PendingService(this, database);
        rewardService = new RewardService(this, configManager, pendingService);
        dailyService = new DailyService(this, database, configManager, timeService, rewardService);
        bankService = new BankService(this, database, configManager, timeService, rankService, rewardService, pendingService);
        foodService = new FoodService(this, database, configManager, timeService, rankService, rewardService);
    }

    public void reload() {
        configManager.loadAll();
        timeService = new TimeService(configManager);
        rankService = new RankService(this, configManager);
        getLogger().info("Configuración recargada.");
    }

    public void debug(String message) {
        if (configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public static BestSupplies getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Database getDatabase() {
        return database;
    }

    public Economy getEconomy() {
        return economy;
    }

    public TimeService getTimeService() {
        return timeService;
    }

    public RankService getRankService() {
        return rankService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public DailyService getDailyService() {
        return dailyService;
    }

    public BankService getBankService() {
        return bankService;
    }

    public FoodService getFoodService() {
        return foodService;
    }

    public PendingService getPendingService() {
        return pendingService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
