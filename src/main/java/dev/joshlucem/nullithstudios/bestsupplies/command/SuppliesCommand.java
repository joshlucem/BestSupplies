package dev.joshlucem.nullithstudios.bestsupplies.command;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class SuppliesCommand implements CommandExecutor, TabCompleter {

    private final BestSupplies plugin;

    public SuppliesCommand(BestSupplies plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Open hub GUI
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
                return true;
            }

            if (!player.hasPermission("bestsupplies.use")) {
                Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
                return true;
            }

            plugin.getGuiManager().openHub(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "daily" -> handleDaily(sender);
            case "bank" -> handleBank(sender);
            case "food" -> handleFood(sender);
            case "status" -> handleStatus(sender);
            case "pending" -> handlePending(sender);
            case "admin" -> handleAdmin(sender, args);
            default -> {
                if (sender instanceof Player player) {
                    Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.invalid-command"), plugin.getConfigManager());
                } else {
                    sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.invalid-command")));
                }
                yield true;
            }
        };
    }

    private boolean handleDaily(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("bestsupplies.daily")) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            return true;
        }

        plugin.getGuiManager().openDaily(player);
        return true;
    }

    private boolean handleBank(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("bestsupplies.bank")) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            return true;
        }

        plugin.getGuiManager().openBank(player);
        return true;
    }

    private boolean handleFood(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("bestsupplies.food")) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            return true;
        }

        plugin.getGuiManager().openFood(player);
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("bestsupplies.status")) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            return true;
        }

        plugin.getGuiManager().openStatus(player);
        return true;
    }

    private boolean handlePending(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-only")));
            return true;
        }

        if (!player.hasPermission("bestsupplies.pending")) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            return true;
        }

        plugin.getGuiManager().openPending(player);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bestsupplies.admin")) {
            if (sender instanceof Player player) {
                Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.no-permission"), plugin.getConfigManager());
            } else {
                sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.no-permission")));
            }
            return true;
        }

        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }

        String adminCmd = args[1].toLowerCase();

        return switch (adminCmd) {
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "givecheque" -> handleGiveCheque(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> {
                sendAdminHelp(sender);
                yield true;
            }
        };
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Text.parse("<gold>=== BestSupplies Admin ===</gold>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin reload</gray> - <white>Recargar configuración</white>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin reset daily <jugador></gray> - <white>Resetear diaria</white>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin reset weekly <jugador></gray> - <white>Resetear cheque semanal</white>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin reset food <jugador> [packId]</gray> - <white>Resetear raciones</white>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin givecheque <jugador> <monto></gray> - <white>Dar cheque</white>"));
        sender.sendMessage(Text.parse("<gray>/supplies admin debug <jugador></gray> - <white>Ver info de debug</white>"));
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reload();
        
        if (sender instanceof Player player) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("general.reload-success"), plugin.getConfigManager());
        } else {
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.reload-success")));
        }
        
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.parse("<red>Uso: /supplies admin reset <daily|weekly|food> <jugador> [packId]</red>"));
            return true;
        }

        String resetType = args[2].toLowerCase();
        Player target = Bukkit.getPlayer(args[3]);

        if (target == null) {
            Map<String, String> placeholders = Map.of("%player%", args[3]);
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-not-found", placeholders)));
            return true;
        }

        switch (resetType) {
            case "daily" -> {
                plugin.getDailyService().resetDailyToday(target);
                Map<String, String> placeholders = Map.of("%player%", target.getName());
                sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.reset-daily", placeholders)));
            }
            case "weekly" -> {
                plugin.getBankService().resetWeeklyClaim(target);
                Map<String, String> placeholders = Map.of("%player%", target.getName());
                sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.reset-weekly", placeholders)));
            }
            case "food" -> {
                String packId = args.length > 4 ? args[4] : null;
                plugin.getFoodService().resetFoodCooldown(target, packId);
                Map<String, String> placeholders = Map.of("%player%", target.getName());
                sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.reset-food", placeholders)));
            }
            default -> sender.sendMessage(Text.parse("<red>Tipo inválido. Usa: daily, weekly, food</red>"));
        }

        return true;
    }

    private boolean handleGiveCheque(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.parse("<red>Uso: /supplies admin givecheque <jugador> <monto></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            Map<String, String> placeholders = Map.of("%player%", args[2]);
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-not-found", placeholders)));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Text.parse("<red>Monto inválido.</red>"));
            return true;
        }

        plugin.getBankService().giveCheque(target, amount);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", target.getName());
        placeholders.put("%amount%", Text.formatMoney(amount));
        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.give-cheque", placeholders)));

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.parse("<red>Uso: /supplies admin debug <jugador></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            Map<String, String> placeholders = Map.of("%player%", args[2]);
            sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("general.player-not-found", placeholders)));
            return true;
        }

        PlayerState state = plugin.getDailyService().getPlayerState(target);
        RankDefinition rank = plugin.getRankService().detectRank(target);
        String weekKey = plugin.getTimeService().getCurrentWeekKey();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", target.getName());
        placeholders.put("%streak%", String.valueOf(state.getStreak()));
        placeholders.put("%date%", state.getLastDailyDate() != null ? state.getLastDailyDate() : "nunca");
        placeholders.put("%week%", weekKey);
        placeholders.put("%rank%", rank != null ? rank.getId() : "default");

        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.debug-header", placeholders)));
        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.debug-streak", placeholders)));
        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.debug-last-daily", placeholders)));
        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.debug-week-key", placeholders)));
        sender.sendMessage(Text.parse(plugin.getConfigManager().getMessage("admin.debug-rank", placeholders)));

        // Additional debug info
        sender.sendMessage(Text.parse("<gray>Cheque reclamado: " + plugin.getBankService().hasClaimedWeekly(target) + "</gray>"));
        sender.sendMessage(Text.parse("<gray>Diaria reclamada hoy: " + plugin.getDailyService().hasClaimedToday(target) + "</gray>"));
        sender.sendMessage(Text.parse("<gray>Pendientes: " + plugin.getPendingService().getPendingCount(target) + "</gray>"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("daily", "bank", "food", "status", "pending"));
            if (sender.hasPermission("bestsupplies.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("bestsupplies.admin")) {
                completions.addAll(Arrays.asList("reload", "reset", "givecheque", "debug"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("reset")) {
            completions.addAll(Arrays.asList("daily", "weekly", "food"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            // Player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") 
                   && args[1].equalsIgnoreCase("reset") && args[2].equalsIgnoreCase("food")) {
            // Pack IDs
            completions.addAll(Arrays.asList("exploracion", "combate", "trabajo"));
        }

        // Filter by partial input
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .toList();
    }
}
