package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class HubGui extends BaseGui {

    private static final int[] BLACK_SLOTS = {0, 2, 8, 18, 20, 26};
    private static final int[] GRAY_SLOTS = {1, 3, 4, 5, 6, 7, 9, 11, 15, 17, 19, 21, 22, 23, 24, 25};

    private static final int STATUS_SLOT = 10;
    private static final int DAILY_SLOT = 12;
    private static final int BANK_SLOT = 13;
    private static final int FOOD_SLOT = 14;
    private static final int PENDING_SLOT = 16;

    public HubGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("hub"), 3);
    }

    @Override
    protected void build() {
        fillEmpty(Material.WHITE_STAINED_GLASS_PANE);
        setSlots(BLACK_SLOTS, ItemParser.createFiller(Material.BLACK_STAINED_GLASS_PANE));
        setSlots(GRAY_SLOTS, ItemParser.createFiller(Material.GRAY_STAINED_GLASS_PANE));

        buildStatusItem();
        buildActions();
    }

    private void buildStatusItem() {
        boolean dailyPending = !plugin.getDailyService().hasClaimedToday(player);
        boolean bankPending = plugin.getBankService().hasPendingMonthlyClaims(player);
        boolean foodPending = plugin.getFoodService().hasAnyReadyRation(player);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Recompensas diarias: " + (dailyPending ? "<yellow>Pendiente</yellow>" : "<green>Reclamadas</green>") + "</gray>");
        lore.add("<gray>Recompensas de la Banca Mensual: " + (bankPending ? "<yellow>Pendiente</yellow>" : "<green>Reclamadas</green>") + "</gray>");
        lore.add("<gray>Recompensas de las Raciones: " + (foodPending ? "<yellow>Pendiente</yellow>" : "<green>Reclamadas</green>") + "</gray>");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            head.setItemMeta(skullMeta);
        }

        ItemStack item = ItemParser.createItem(
                head,
                "<gold><bold>Estado Personal</bold></gold>",
                lore
        );

        setItem(STATUS_SLOT, item);
    }

    private void buildActions() {
        setItem(
                DAILY_SLOT,
                createIconItem(
                        plugin.getConfigManager().getItemsAdderIcon("daily-menu", "nullith:diamond"),
                        Material.DIAMOND,
                        plugin.getConfigManager().getMessage("gui.hub.daily-item"),
                        plugin.getConfigManager().getMessageList("gui.hub.daily-lore")
                ),
                event -> {
                    if (!player.hasPermission("bestsupplies.daily")) {
                        sendNoPermission();
                        return;
                    }
                    plugin.getGuiManager().openDaily(player);
                }
        );

        setItem(
                BANK_SLOT,
                createIconItem(
                        plugin.getConfigManager().getItemsAdderIcon("monthly-bank-menu", "nullith:money_token"),
                        Material.PAPER,
                        plugin.getConfigManager().getMessage("gui.hub.bank-item"),
                        plugin.getConfigManager().getMessageList("gui.hub.bank-lore")
                ),
                event -> {
                    if (!player.hasPermission("bestsupplies.bank")) {
                        sendNoPermission();
                        return;
                    }
                    plugin.getGuiManager().openBank(player);
                }
        );

        setItem(
                FOOD_SLOT,
                ItemParser.createItem(
                        Material.BAKED_POTATO,
                        plugin.getConfigManager().getMessage("gui.hub.food-item"),
                        plugin.getConfigManager().getMessageList("gui.hub.food-lore")
                ),
                event -> {
                    if (!player.hasPermission("bestsupplies.food")) {
                        sendNoPermission();
                        return;
                    }
                    plugin.getGuiManager().openFood(player);
                }
        );

        int pendingCount = plugin.getPendingService().getPendingCount(player);
        setItem(
                PENDING_SLOT,
                ItemParser.createItem(
                        Material.SHULKER_BOX,
                        plugin.getConfigManager().getMessage("gui.hub.pending-item"),
                        getMessageList("gui.hub.pending-lore", java.util.Map.of("%count%", String.valueOf(pendingCount))),
                        null
                ),
                event -> {
                    if (!player.hasPermission("bestsupplies.pending")) {
                        sendNoPermission();
                        return;
                    }
                    plugin.getGuiManager().openPending(player);
                }
        );
    }
}
