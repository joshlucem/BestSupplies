package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.balance.api.BalanceApi;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankService {

    private static final String MONTHLY_KEY_PREFIX = "month:";

    private final BestSupplies plugin;
    private final Database database;
    private final ConfigManager configManager;
    private final TimeService timeService;
    private final RankService rankService;
    private final RewardService rewardService;
    private final PendingService pendingService;

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

        this.chequeIdKey = new NamespacedKey(plugin, "cheque_id");
        this.chequeAmountKey = new NamespacedKey(plugin, "cheque_amount");
        this.chequeWeekKey = new NamespacedKey(plugin, "cheque_week");
        this.chequePlayerKey = new NamespacedKey(plugin, "cheque_player");
    }

    public boolean hasClaimedMonthlyDay(Player player, LocalDate date) {
        String dateKey = getMonthlyClaimKey(date);
        return database.hasWeeklyClaim(player.getUniqueId().toString(), dateKey);
    }

    public MonthlyDayStatus getMonthlyDayStatus(Player player, LocalDate date) {
        LocalDate today = timeService.getCurrentDate();

        if (date.getYear() != today.getYear() || date.getMonthValue() != today.getMonthValue()) {
            return MonthlyDayStatus.LOCKED;
        }

        if (date.isAfter(today)) {
            return MonthlyDayStatus.LOCKED;
        }

        return hasClaimedMonthlyDay(player, date) ? MonthlyDayStatus.CLAIMED : MonthlyDayStatus.AVAILABLE;
    }

    public double getMonthlyAmount(Player player, LocalDate date) {
        return getMonthlyAmount(player, date.getDayOfMonth());
    }

    public double getMonthlyAmount(Player player, int dayOfMonth) {
        RankDefinition rank = rankService.detectRank(player);
        if (rank == null) {
            return 0;
        }
        return Math.max(0, rank.getMonthlyAmount(dayOfMonth));
    }

    public boolean hasPendingMonthlyClaims(Player player) {
        LocalDate today = timeService.getCurrentDate();

        for (int day = 1; day <= today.getDayOfMonth(); day++) {
            LocalDate date = today.withDayOfMonth(day);
            if (getMonthlyDayStatus(player, date) == MonthlyDayStatus.AVAILABLE) {
                return true;
            }
        }

        return false;
    }

    public MonthlyClaimResult claimMonthlyDay(Player player, LocalDate date) {
        MonthlyDayStatus status = getMonthlyDayStatus(player, date);
        if (status == MonthlyDayStatus.LOCKED) {
            return MonthlyClaimResult.LOCKED;
        }

        if (status == MonthlyDayStatus.CLAIMED) {
            return MonthlyClaimResult.ALREADY_CLAIMED;
        }

        RankDefinition rank = rankService.detectRank(player);
        if (rank == null) {
            return MonthlyClaimResult.NO_RANK;
        }

        double amount = Math.max(0, rank.getMonthlyAmount(date.getDayOfMonth()));
        if (amount <= 0) {
            return MonthlyClaimResult.NO_REWARD;
        }

        if (!plugin.getEconomyService().depositSilver(player, amount, "monthly-bank")) {
            Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            return MonthlyClaimResult.ERROR;
        }

        String claimKey = getMonthlyClaimKey(date);
        database.setWeeklyClaim(player.getUniqueId().toString(), claimKey, true);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
        placeholders.put("%day%", String.valueOf(date.getDayOfMonth()));
        Text.sendPrefixed(player, configManager.getMessage("bank.monthly-claim-success", placeholders), configManager);

        plugin.debug("Banca mensual reclamada por " + player.getName() + " dia " + date.getDayOfMonth() + " monto " + Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
        return MonthlyClaimResult.SUCCESS;
    }

    public void resetMonthlyDayClaim(Player target, LocalDate date) {
        String claimKey = getMonthlyClaimKey(date);
        database.resetWeeklyClaim(target.getUniqueId().toString(), claimKey);
    }

    private String getMonthlyClaimKey(LocalDate date) {
        return MONTHLY_KEY_PREFIX + timeService.getDateKey(date);
    }

    public boolean hasClaimedWeekly(Player player) {
        String weekKey = timeService.getCurrentWeeklyPeriodKey();
        return database.hasWeeklyClaim(player.getUniqueId().toString(), weekKey);
    }

    public double getWeeklyAmount(Player player) {
        return rankService.getWeeklyMoney(player);
    }

    public ClaimResult claimWeekly(Player player) {
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

        String weekKey = timeService.getCurrentWeeklyPeriodKey();
        String playerUuid = player.getUniqueId().toString();

        if (configManager.useChequeItem()) {
            String chequeId = ChequeData.generateChequeId();
            ChequeData cheque = new ChequeData(chequeId, playerUuid, weekKey, amount);
            database.saveCheque(cheque);

            ItemStack chequeItem = createChequeItem(player, amount, weekKey, chequeId);

            if (ItemParser.hasAnyInventorySpace(player)) {
                player.getInventory().addItem(chequeItem);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
                Text.sendPrefixed(player, configManager.getMessage("bank.cheque-received", placeholders), configManager);
            } else {
                pendingService.saveChequeToPending(player, chequeId, amount, weekKey);
            }

            database.setWeeklyClaim(playerUuid, weekKey, true);
        } else {
            if (!plugin.getEconomyService().depositSilver(player, amount, "weekly-bank")) {
                Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
                return ClaimResult.ERROR;
            }

            database.setWeeklyClaim(playerUuid, weekKey, true);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
            Text.sendPrefixed(player, configManager.getMessage("bank.direct-deposit", placeholders), configManager);
        }

        Text.sendPrefixed(player, configManager.getMessage("bank.claim-success"), configManager);
        plugin.debug("Cheque semanal reclamado por " + player.getName() + ": " + Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));

        return ClaimResult.SUCCESS;
    }

    public ItemStack createChequeItem(Player player, double amount, String weekKey, String chequeId) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Text.parse(configManager.getMessage("cheque.item-name")));

            List<String> loreStrings = configManager.getMessageList("cheque.lore");
            List<Component> lore = new ArrayList<>();

            String rankTag = rankService.getRankTag(player);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
            placeholders.put("%bestsupplies_rank_tag%", rankTag);
            placeholders.put("%week%", weekKey);

            for (String line : loreStrings) {
                String processed = line;
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    processed = processed.replace(entry.getKey(), entry.getValue());
                }
                lore.add(Text.parse(processed));
            }
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(chequeIdKey, PersistentDataType.STRING, chequeId);
            pdc.set(chequeAmountKey, PersistentDataType.DOUBLE, amount);
            pdc.set(chequeWeekKey, PersistentDataType.STRING, weekKey);
            pdc.set(chequePlayerKey, PersistentDataType.STRING, player.getUniqueId().toString());

            item.setItemMeta(meta);
        }

        return item;
    }

    public RedeemResult redeemCheque(Player player, ItemStack chequeItem) {
        if (chequeItem == null || chequeItem.getType() != Material.PAPER) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        ItemMeta meta = chequeItem.getItemMeta();
        if (meta == null) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(chequeIdKey, PersistentDataType.STRING)) {
            return RedeemResult.NOT_A_CHEQUE;
        }

        String chequeId = pdc.get(chequeIdKey, PersistentDataType.STRING);
        Double amount = pdc.get(chequeAmountKey, PersistentDataType.DOUBLE);
        String playerUuid = pdc.get(chequePlayerKey, PersistentDataType.STRING);

        if (chequeId == null || amount == null) {
            return RedeemResult.INVALID;
        }

        if (playerUuid != null && !playerUuid.equals(player.getUniqueId().toString())) {
            Text.sendPrefixed(player, configManager.getMessage("cheque.not-owner"), configManager);
            return RedeemResult.NOT_OWNER;
        }

        ChequeData chequeData = database.getCheque(chequeId);
        if (chequeData == null) {
            Text.sendPrefixed(player, configManager.getMessage("cheque.invalid"), configManager);
            return RedeemResult.INVALID;
        }

        if (chequeData.isRedeemed()) {
            Text.sendPrefixed(player, configManager.getMessage("cheque.already-redeemed"), configManager);
            return RedeemResult.ALREADY_REDEEMED;
        }

        if (!plugin.getEconomyService().depositSilver(player, amount, "weekly-cheque-redeem")) {
            Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            return RedeemResult.ERROR;
        }

        database.redeemCheque(chequeId, System.currentTimeMillis());
        chequeItem.setAmount(chequeItem.getAmount() - 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
        Text.sendPrefixed(player, configManager.getMessage("cheque.redeemed", placeholders), configManager);

        plugin.debug("Cheque canjeado por " + player.getName() + ": " + Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount) + " (ID: " + chequeId + ")");

        return RedeemResult.SUCCESS;
    }

    public void resetWeeklyClaim(Player target) {
        String weekKey = timeService.getCurrentWeeklyPeriodKey();
        database.resetWeeklyClaim(target.getUniqueId().toString(), weekKey);
        plugin.debug("Cheque semanal reseteado para " + target.getName());
    }

    public void giveCheque(Player target, double amount) {
        String weekKey = timeService.getCurrentWeeklyPeriodKey();
        String chequeId = ChequeData.generateChequeId();
        String playerUuid = target.getUniqueId().toString();

        ChequeData cheque = new ChequeData(chequeId, playerUuid, weekKey, amount);
        database.saveCheque(cheque);

        ItemStack chequeItem = createChequeItem(target, amount, weekKey, chequeId);

        if (ItemParser.hasAnyInventorySpace(target)) {
            target.getInventory().addItem(chequeItem);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
            Text.sendPrefixed(target, configManager.getMessage("bank.cheque-received", placeholders), configManager);
        } else {
            pendingService.saveChequeToPending(target, chequeId, amount, weekKey);
        }

        plugin.debug("Cheque admin entregado a " + target.getName() + ": " + Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount));
    }

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

    public enum MonthlyDayStatus {
        AVAILABLE,
        CLAIMED,
        LOCKED
    }

    public enum MonthlyClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        LOCKED,
        NO_RANK,
        NO_REWARD,
        ERROR
    }

    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_RANK,
        NO_REWARD,
        ERROR
    }

    public enum RedeemResult {
        SUCCESS,
        NOT_A_CHEQUE,
        INVALID,
        NOT_OWNER,
        ALREADY_REDEEMED,
        ERROR
    }
}
