package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.balance.api.BalanceApi;
import dev.joshlucem.nullithstudios.balance.api.BalanceApiProvider;
import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.OfflinePlayer;

public class EconomyService {

    private final BestSupplies plugin;
    private BalanceApi api;

    public EconomyService(BestSupplies plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("Balance") == null) {
            return false;
        }

        api = BalanceApiProvider.get(plugin.getServer());
        return api != null && api.isAvailable();
    }

    public boolean isAvailable() {
        return api != null && api.isAvailable();
    }

    public BalanceApi getApi() {
        return api;
    }

    public boolean depositSilver(OfflinePlayer player, double amount, String context) {
        return deposit(player, BalanceApi.CurrencyType.SILVER, amount, context);
    }

    public boolean depositGold(OfflinePlayer player, double amount, String context) {
        return deposit(player, BalanceApi.CurrencyType.GOLD, amount, context);
    }

    public boolean deposit(OfflinePlayer player, BalanceApi.CurrencyType currency, double amount, String context) {
        if (amount <= 0) {
            return true;
        }

        if (!isAvailable()) {
            plugin.getLogger().warning("BalanceApi no esta disponible para acreditar " + Text.formatCurrency(currency, amount));
            return false;
        }

        BalanceApi.OperationSource source = BalanceApi.OperationSource.apiExternalPlugin(plugin.getName(), context);
        BalanceApi.OperationResult<BalanceApi.WalletChangeReceipt> result = api.deposit(player, currency, amount, source);

        if (!result.success()) {
            String holder = player.getName() != null ? player.getName() : player.getUniqueId().toString();
            plugin.getLogger().warning(
                    "No se pudo depositar " + Text.formatCurrency(currency, amount)
                            + " a " + holder + " [" + context + "]: " + result.reason()
            );
            return false;
        }

        return true;
    }
}
