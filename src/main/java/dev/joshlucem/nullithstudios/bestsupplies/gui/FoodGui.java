package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.FoodPackDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.FoodService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodGui extends BaseGui {

    private static final String[] PACK_IDS = {"exploracion", "combate", "trabajo"};
    private static final String[] PACK_SLOT_KEYS = {"pack-exploration", "pack-combat", "pack-work"};
    private static final int[] DEFAULT_PACK_SLOTS = {20, 22, 24};
    private static final int DEFAULT_INFO_SLOT = 13;
    private static final int DEFAULT_BACK_SLOT = 49;

    public FoodGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("food"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        buildInfoDisplay();
        buildFoodPacks();

        int backSlot = getSlot("food", "back", DEFAULT_BACK_SLOT);
        setItem(backSlot, createBackButton(), event -> plugin.getGuiManager().openHub(player));
    }

    private void buildInfoDisplay() {
        Duration cooldown = plugin.getFoodService().getCooldownDuration(player);
        String cooldownStr = plugin.getTimeService().formatDuration(cooldown);

        List<String> lore = new ArrayList<>(plugin.getConfigManager().getMessageList("gui.food.info-lore"));
        lore.add("");
        lore.add(plugin.getConfigManager().getMessage("food.pack-info", Map.of("%time%", cooldownStr)));

        ItemStack info = ItemParser.createItem(
            Material.BOOK,
            plugin.getConfigManager().getMessage("gui.food.info-item"),
            lore,
            null
        );

        int infoSlot = getSlot("food", "info", DEFAULT_INFO_SLOT);
        setItem(infoSlot, info);
    }

    private void buildFoodPacks() {
        Map<String, FoodPackDefinition> availablePacks = plugin.getFoodService().getAvailablePacks(player);

        for (int i = 0; i < PACK_IDS.length; i++) {
            String packId = PACK_IDS[i];
            int slot = getSlot("food", PACK_SLOT_KEYS[i], DEFAULT_PACK_SLOTS[i]);
            FoodPackDefinition pack = availablePacks.get(packId);

            if (pack == null) {
                ItemStack locked = ItemParser.createItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    plugin.getConfigManager().getMessage("gui.food.pack-locked"),
                    plugin.getConfigManager().getMessageList("gui.food.pack-locked-lore")
                );
                setItem(slot, locked);
                continue;
            }

            FoodService.PackStatus status = plugin.getFoodService().getPackStatus(player, packId);
            setItem(slot, createPackItem(pack, status), event -> {
                FoodService.PackStatus currentStatus = plugin.getFoodService().getPackStatus(player, packId);
                if (currentStatus == FoodService.PackStatus.READY) {
                    claimPack(packId);
                }
            });
        }
    }

    private ItemStack createPackItem(FoodPackDefinition pack, FoodService.PackStatus status) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%pack%", pack.getDisplayName());

        if (status == FoodService.PackStatus.READY) {
            List<String> lore = new ArrayList<>();
            lore.addAll(plugin.getConfigManager().getMessageList("gui.food.pack-available-lore"));
            lore.add("<gray>Contenido:</gray>");

            for (String itemStr : pack.getItems()) {
                String[] parts = itemStr.split(":");
                if (parts.length < 2) {
                    continue;
                }
                String materialId = parts[0].toUpperCase();
                String amount = parts[1];
                String itemName = plugin.getConfigManager().getItemName(materialId);
                lore.add("<gray>- " + amount + "x " + itemName + "</gray>");
            }

            return ItemParser.createItem(
                pack.getIcon(),
                plugin.getConfigManager().getMessage("gui.food.pack-available", placeholders),
                lore,
                null
            );
        }

        String time = plugin.getFoodService().formatTimeUntilAvailable(player, pack.getId());
        placeholders.put("%time%", time);

        return ItemParser.createItem(
            Material.GRAY_DYE,
            plugin.getConfigManager().getMessage("gui.food.pack-cooldown", placeholders),
            getMessageList("gui.food.pack-cooldown-lore", placeholders),
            null
        );
    }

    private void claimPack(String packId) {
        FoodService.ClaimResult result = plugin.getFoodService().claimPack(player, packId);

        if (result == FoodService.ClaimResult.SUCCESS) {
            rebuild();
            return;
        }

        if (result == FoodService.ClaimResult.COOLDOWN) {
            String time = plugin.getFoodService().formatTimeUntilAvailable(player, packId);
            Text.sendPrefixed(
                player,
                plugin.getConfigManager().getMessage("food.cooldown", Map.of("%time%", time)),
                plugin.getConfigManager()
            );
            return;
        }

        Text.sendPrefixed(player, plugin.getConfigManager().getMessage("food.no-packs"), plugin.getConfigManager());
    }

    @Override
    public void updateCountdowns() {
        buildInfoDisplay();
        buildFoodPacks();
    }
}
