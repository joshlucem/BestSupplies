package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.ChequeData;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankService {

    private final BestSupplies plugin;
    private final Database database;
    private final ConfigManager configManager;
    private final TimeService timeService;
    private final RankService rankService;
    private final RewardService rewardService;
    private final PendingService pendingService;

    // PDC Keys for cheque metadata
    private final NamespacedKey chequeIdKey;
    private final NamespacedKey chequeAmountKey;
    private final NamespacedKey chequeWeekKey;
    private final NamespacedKey chequePlayerKey;

    public BankService(BestSupplies plugin, Database database, ConfigManager configManager,
                       TimeService timeService, RankService rankService, 
                       RewardService rewardService, PendingService pendingService) {
        this.plugin = plugin;
        this.database = database;
        this.configManager = configManager;
        this.timeService = timeService;
        this.rankService = rankService;
        this.rewardService = rewardService;
        this.pendingService = pendingService;

        // Initialize PDC keys
        this.chequeIdKey = new NamespacedKey(plugin, "cheque_id");
        this.chequeAmountKey = new NamespacedKey(plugin, "cheque_amount");
        this.chequeWeekKey = new NamespacedKey(plugin, "cheque_week");
        this.chequePlayerKey = new NamespacedKey(plugin, "cheque_player");
    }

    /**
     * Check if player has claimed their weekly cheque
     */
    public boolean hasClaimedWeekly(Player player) {
        String weekKey = timeService.getCurrentWeekKey();
        return database.hasWeeklyClaim(player.getUniqueId().toString(), weekKey);
    }

    /**
     * Get the weekly money amount for a player based on their rank
     */
    public double getWeeklyAmount(Player player) {
        return rankService.getWeeklyMoney(player);
    }

    /**
     * Claim the weekly cheque
     */
    public ClaimResult claimWeekly(Player player) {
        // Check if already claimed this week
        if (hasClaimedWeekly(player)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        RankDefinition rank = rankService.detectRank(player);
        if (rank == null) {
            return ClaimResult.NO_RANK;
        }

        double amount = rank.getWeeklyMoney();
        if (amount <= 0) {
            return ClaimResult.NO_REWARD;
        }

        String weekKey = timeService.getCurrentWeekKey();
        String playerUuid = player.getUniqueId().toString();

        // Mark as claimed
        database.setWeeklyClaim(playerUuid, weekKey, true);

        // Check config: use cheque item or direct deposit?
        if (configManager.useChequeItem()) {
            // Create and give cheque item
            String chequeId = ChequeData.generateChequeId();
            
            // Save cheque to database
            ChequeData cheque = new ChequeData(chequeId, playerUuid, weekKey, amount);
            database.saveCheque(cheque);

            // Create cheque item
            ItemStack chequeItem = createChequeItem(player, amount, weekKey, chequeId);

            // Give to player or save to pending
            if (ItemParser.hasAnyInventorySpace(player)) {
                player.getInventory().addItem(chequeItem);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%amount%", Text.formatMoney(amount));
                Text.sendPrefixed(player, configManager.getMessage("bank.cheque-received", placeholders), configManager);
            } else {
                pendingService.saveChequeToPending(player, chequeId, amount, weekKey);
            }
        } else {
            // Direct deposit
            plugin.getEconomy().depositPlayer(player, amount);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatMoney(amount));
            Text.sendPrefixed(player, configManager.getMessage("bank.direct-deposit", placeholders), configManager);
        }

        Text.sendPrefixed(player, configManager.getMessage("bank.claim-success"), configManager);
        plugin.debug("Cheque semanal reclamado por " + player.getName() + ": $" + amount);

        return ClaimResult.SUCCESS;
    }

    /**
     * Create a cheque item with metadata
     */
    public ItemStack createChequeItem(Player player, double amount, String weekKey, String chequeId) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.displayName(Text.parse(configManager.getMessage("cheque.item-name")));

            // Set lore
            List<String> loreStrings = configManager.getMessageList("cheque.lore");
            List<Component> lore = new ArrayList<>();
            
            RankDefinition rank = rankService.detectRank(player);
            String rankName = rank != null ? rank.getDisplayName() : "Aventurero";
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatMoney(amount));
            placeholders.put("%rank%", rankName);
            placeholders.put("%week%", weekKey);
            
            for (String line : loreStrings) {
                String processed = line;
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    processed = processed.replace(entry.getKey(), entry.getValue());
                }
                lore.add(Text.parse(processed));
            }
            meta.lore(lore);

            // Store metadata in PDC
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(chequeIdKey, PersistentDataType.STRING, chequeId);
            pdc.set(chequeAmountKey, PersistentDataType.DOUBLE, amount);
            pdc.set(chequeWeekKey, PersistentDataType.STRING, weekKey);
            pdc.set(chequePlayerKey, PersistentDataType.STRING, player.getUniqueId().toString());

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Try to redeem a cheque item
     */
    public RedeemResult redeemCheque(Player player, ItemStack chequeItem) {
        if (chequeItem == null || chequeItem.getType() != Material.PAPER) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        ItemMeta meta = chequeItem.getItemMeta();
        if (meta == null) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Check if this is a valid cheque
        if (!pdc.has(chequeIdKey, PersistentDataType.STRING)) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        String chequeId = pdc.get(chequeIdKey, PersistentDataType.STRING);
        Double amount = pdc.get(chequeAmountKey, PersistentDataType.DOUBLE);
        String playerUuid = pdc.get(chequePlayerKey, PersistentDataType.STRING);

        if (chequeId == null || amount == null) {
            return RedeemResult.INVALID;
        }

        // Check if cheque belongs to this player
        if (playerUuid != null && !playerUuid.equals(player.getUniqueId().toString())) {
            Text.sendPrefixed(player, configManager.getMessage("cheque.not-owner"), configManager);
            return RedeemResult.NOT_OWNER;
        }

        // Check if cheque is already redeemed in database
        ChequeData chequeData = database.getCheque(chequeId);
        if (chequeData == null) {
            // Cheque not in database - this shouldn't happen normally
            Text.sendPrefixed(player, configManager.getMessage("cheque.invalid"), configManager);
            return RedeemResult.INVALID;
        }

        if (chequeData.isRedeemed()) {
            Text.sendPrefixed(player, configManager.getMessage("cheque.already-redeemed"), configManager);
            return RedeemResult.ALREADY_REDEEMED;
        }

        // Redeem the cheque
        database.redeemCheque(chequeId, System.currentTimeMillis());
        plugin.getEconomy().depositPlayer(player, amount);

        // Consume one cheque item
        chequeItem.setAmount(chequeItem.getAmount() - 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", Text.formatMoney(amount));
        Text.sendPrefixed(player, configManager.getMessage("cheque.redeemed", placeholders), configManager);

        plugin.debug("Cheque canjeado por " + player.getName() + ": $" + amount + " (ID: " + chequeId + ")");

        return RedeemResult.SUCCESS;
    }

    /**
     * Reset weekly claim for a player (admin)
     */
    public void resetWeeklyClaim(Player target) {
        String weekKey = timeService.getCurrentWeekKey();
        database.resetWeeklyClaim(target.getUniqueId().toString(), weekKey);
        plugin.debug("Cheque semanal reseteado para " + target.getName());
    }

    /**
     * Give a cheque directly to a player (admin)
     */
    public void giveCheque(Player target, double amount) {
        String weekKey = timeService.getCurrentWeekKey();
        String chequeId = ChequeData.generateChequeId();
        String playerUuid = target.getUniqueId().toString();

        // Save cheque to database
        ChequeData cheque = new ChequeData(chequeId, playerUuid, weekKey, amount);
        database.saveCheque(cheque);

        // Create and give cheque item
        ItemStack chequeItem = createChequeItem(target, amount, weekKey, chequeId);

        if (ItemParser.hasAnyInventorySpace(target)) {
            target.getInventory().addItem(chequeItem);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatMoney(amount));
            Text.sendPrefixed(target, configManager.getMessage("bank.cheque-received", placeholders), configManager);
        } else {
            pendingService.saveChequeToPending(target, chequeId, amount, weekKey);
        }

        plugin.debug("Cheque admin entregado a " + target.getName() + ": $" + amount);
    }

    /**
     * Check if an item is a cheque
     */
    public boolean isChequeItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(chequeIdKey, PersistentDataType.STRING);
    }

    /**
     * Claim results
     */
    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_RANK,
        NO_REWARD
    }

    /**
     * Redeem results
     */
    public enum RedeemResult {
        SUCCESS,
        NOT_A_CHEQUE,
        INVALID,
        NOT_OWNER,
        ALREADY_REDEEMED
    }
}
