package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.RationDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.FoodService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FoodGui extends BaseGui {

    private static final int[] BLACK_SLOTS = {0, 2, 8, 45, 47, 53};
    private static final int[] GRAY_SLOTS = {1, 3, 4, 5, 6, 7, 9, 11, 17, 18, 20, 26, 27, 29, 35, 36, 38, 44, 46, 48, 49, 50, 51, 52};
    private static final int[] CONTENT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 39, 40, 41, 42, 43};

    private static final int BACK_SLOT = 10;
    private static final int PREV_SLOT = 19;
    private static final int NEXT_SLOT = 28;
    private static final int INFO_SLOT = 37;

    private int page = 0;

    public FoodGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("food"), 6);
    }

    @Override
    protected void build() {
        fillEmpty(Material.WHITE_STAINED_GLASS_PANE);
        setSlots(BLACK_SLOTS, ItemParser.createFiller(Material.BLACK_STAINED_GLASS_PANE));
        setSlots(GRAY_SLOTS, ItemParser.createFiller(Material.GRAY_STAINED_GLASS_PANE));

        List<RationDefinition> rations = plugin.getFoodService().getAllRations();
        int totalPages = Math.max(1, (int) Math.ceil((double) rations.size() / CONTENT_SLOTS.length));
        if (page >= totalPages) {
            page = Math.max(0, totalPages - 1);
        }

        buildRations(rations);
        buildInfo(totalPages);
        buildNavigation(totalPages);
        buildBackButton();
    }

    private void buildRations(List<RationDefinition> rations) {
        int start = page * CONTENT_SLOTS.length;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int slot = CONTENT_SLOTS[i];
            int index = start + i;

            if (index >= rations.size()) {
                setItem(slot, ItemParser.createFiller(Material.WHITE_STAINED_GLASS_PANE));
                continue;
            }

            RationDefinition ration = rations.get(index);
            FoodService.RationStatus status = plugin.getFoodService().getRationStatus(player, ration);
            if (status == FoodService.RationStatus.READY) {
                setItem(slot, createRationItem(ration, status), event -> {
                    FoodService.ClaimResult result = plugin.getFoodService().claimRation(player, ration.getId());
                    if (result == FoodService.ClaimResult.SUCCESS) {
                        rebuild();
                    }
                });
            } else {
                setItem(slot, createRationItem(ration, status));
            }
        }
    }

    private ItemStack createRationItem(RationDefinition ration, FoodService.RationStatus status) {
        String rankId = plugin.getRankService().detectRankId(player);
        List<String> lore = new ArrayList<>(ration.getDescription());

        if (!lore.isEmpty()) {
            lore.add("");
        }

        switch (status) {
            case LOCKED -> {
                lore.add("<red>Esta racion esta bloqueada para tu rango.</red>");
                lore.add("<gray>Necesitas rango premium para desbloquearla.</gray>");
                return createIconItem(
                        plugin.getConfigManager().getItemsAdderIcon("lock", "mcicons:icon_lock"),
                        Material.BARRIER,
                        plugin.getConfigManager().getMessage("gui.food.ration-locked", Map.of("%ration%", ration.getDisplayName())),
                        lore
                );
            }
            case COOLDOWN -> {
                String time = plugin.getFoodService().formatTimeUntilAvailable(player, ration.getId());
                lore.add("<yellow>Disponible en: " + time + "</yellow>");
                return ItemParser.createItem(
                        resolveBaseIcon(ration.getIcon()),
                        plugin.getConfigManager().getMessage("gui.food.ration-cooldown", Map.of("%ration%", ration.getDisplayName(), "%time%", time)),
                        lore,
                        null
                );
            }
            case CLAIMED -> {
                lore.add("<gray>Ya reclamaste esta racion de uso unico.</gray>");
                return ItemParser.createItem(
                        Material.GRAY_DYE,
                        plugin.getConfigManager().getMessage("gui.food.ration-claimed", Map.of("%ration%", ration.getDisplayName())),
                        lore
                );
            }
            case READY -> {
                lore.add("<green>Clic para reclamar.</green>");

                List<String> fixedRewards = ration.resolveRewards(rankId);
                List<String> randomRewards = ration.resolveRandomRewards(rankId);
                appendRewardsPreview(lore, fixedRewards, randomRewards, ration.getRandomPicks());

                return ItemParser.createItem(
                        resolveBaseIcon(ration.getIcon()),
                        plugin.getConfigManager().getMessage("gui.food.ration-ready", Map.of("%ration%", ration.getDisplayName())),
                        lore,
                        null
                );
            }
            default -> {
                return ItemParser.createItem(Material.BARRIER, "<red>No disponible</red>", lore);
            }
        }
    }

    private void appendRewardsPreview(List<String> lore, List<String> fixedRewards, List<String> randomRewards, int randomPicks) {
        if ((fixedRewards == null || fixedRewards.isEmpty()) && (randomRewards == null || randomRewards.isEmpty())) {
            return;
        }

        lore.add("");
        lore.add("<gray>Contenido:</gray>");

        int shown = 0;
        if (fixedRewards != null) {
            for (String reward : fixedRewards) {
                if (shown >= 4) {
                    lore.add("<gray>- ...</gray>");
                    return;
                }
                lore.add("<gray>- " + reward + "</gray>");
                shown++;
            }
        }

        if (randomRewards != null && !randomRewards.isEmpty() && randomPicks > 0) {
            lore.add("<gray>- +" + randomPicks + " recompensa aleatoria</gray>");
        }
    }

    private ItemStack resolveBaseIcon(String iconId) {
        ItemStack ia = ItemParser.getItemsAdderItem(iconId);
        if (ia != null) {
            return ia;
        }

        try {
            Material material = Material.valueOf(iconId.toUpperCase());
            return new ItemStack(material);
        } catch (Exception ignored) {
        }

        return new ItemStack(Material.CHEST);
    }

    private void buildInfo(int totalPages) {
        Map<String, String> placeholders = Map.of(
                "%bestsupplies_rank_tag%", plugin.getRankService().getRankTag(player),
                "%page%", String.valueOf(page + 1),
                "%total%", String.valueOf(totalPages)
        );

        ItemStack info = ItemParser.createItem(
                Material.BOOK,
                plugin.getConfigManager().getMessage("gui.food.info-item", placeholders),
                getMessageList("gui.food.info-lore", placeholders),
                null
        );
        setItem(INFO_SLOT, info);
    }

    private void buildNavigation(int totalPages) {
        String backId = plugin.getConfigManager().getItemsAdderIcon("nav-back", "_iainternal:icon_back_orange");
        String nextId = plugin.getConfigManager().getItemsAdderIcon("nav-next", "_iainternal:icon_next_orange");
        String cancelId = plugin.getConfigManager().getItemsAdderIcon("nav-cancel", "_iainternal:icon_cancel");

        if (page > 0) {
            setItem(
                    PREV_SLOT,
                    createIconItem(backId, Material.ARROW,
                            plugin.getConfigManager().getMessage("gui.common.prev-page-item"),
                            plugin.getConfigManager().getMessageList("gui.common.prev-page-lore")),
                    event -> {
                        page--;
                        rebuild();
                    }
            );
        } else {
            setItem(PREV_SLOT, createIconItem(cancelId, Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-prev-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-prev-lore")));
        }

        if (page + 1 < totalPages) {
            setItem(
                    NEXT_SLOT,
                    createIconItem(nextId, Material.ARROW,
                            plugin.getConfigManager().getMessage("gui.common.next-page-item"),
                            plugin.getConfigManager().getMessageList("gui.common.next-page-lore")),
                    event -> {
                        page++;
                        rebuild();
                    }
            );
        } else {
            setItem(NEXT_SLOT, createIconItem(cancelId, Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-next-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-next-lore")));
        }
    }

    private void buildBackButton() {
        ItemStack back = ItemParser.createItem(
                Material.CHEST_MINECART,
                plugin.getConfigManager().getMessage("gui.food.back-item"),
                plugin.getConfigManager().getMessageList("gui.food.back-lore")
        );
        setItem(BACK_SLOT, back, event -> plugin.getGuiManager().openHub(player));
    }

    @Override
    public void updateCountdowns() {
        List<RationDefinition> rations = plugin.getFoodService().getAllRations();
        buildRations(rations);
    }
}
