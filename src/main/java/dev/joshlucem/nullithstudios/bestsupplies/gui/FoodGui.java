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
import java.util.*;

public class FoodGui extends BaseGui {

    private static final int[] PACK_SLOTS = {20, 22, 24};
    private static final String[] PACK_IDS = {"exploracion", "combate", "trabajo"};

    public FoodGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("food"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Info display (slot 13)
        buildInfoDisplay();

        // Food packs (slots 20, 22, 24)
        buildFoodPacks();

        // Back button (slot 49)
        setItem(49, createBackButton(), event -> {
            plugin.getGuiManager().openHub(player);
        });
    }

    private void buildInfoDisplay() {
        Duration cooldown = plugin.getFoodService().getCooldownDuration(player);
        String cooldownStr = plugin.getTimeService().formatDuration(cooldown);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%time%", cooldownStr);

        List<String> lore = plugin.getConfigManager().getMessageList("gui.food.info-lore");
        List<String> processedLore = new ArrayList<>(lore);
        processedLore.add("");
        processedLore.add(plugin.getConfigManager().getMessage("food.pack-info", placeholders));

        ItemStack item = ItemParser.createItem(
            Material.BOOK,
            plugin.getConfigManager().getMessage("gui.food.info-item"),
            processedLore,
            null
        );

        setItem(13, item);
    }

    private void buildFoodPacks() {
        Map<String, FoodPackDefinition> availablePacks = plugin.getFoodService().getAvailablePacks(player);

        for (int i = 0; i < PACK_IDS.length; i++) {
            String packId = PACK_IDS[i];
            int slot = PACK_SLOTS[i];

            FoodPackDefinition pack = availablePacks.get(packId);
            
            if (pack == null) {
                // Pack not available for this rank - show locked
                ItemStack item = ItemParser.createItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    "<gray>Pack no disponible</gray>",
                    List.of("<gray>Tu rango no tiene acceso</gray>", "<gray>a este pack.</gray>")
                );
                setItem(slot, item);
                continue;
            }

            FoodService.PackStatus status = plugin.getFoodService().getPackStatus(player, packId);
            ItemStack item = createPackItem(pack, status);

            final String clickPackId = packId;
            setItem(slot, item, event -> {
                if (status == FoodService.PackStatus.READY) {
                    claimPack(clickPackId);
                }
            });
        }
    }

    private ItemStack createPackItem(FoodPackDefinition pack, FoodService.PackStatus status) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%pack%", pack.getDisplayName());

        List<String> lore = new ArrayList<>();

        if (status == FoodService.PackStatus.READY) {
            // Available
            lore.add("<green>¡DISPONIBLE!</green>");
            lore.add("");

            // Show pack contents with translated names
            lore.add("<gray>Contenido:</gray>");
            for (String itemStr : pack.getItems()) {
                String[] parts = itemStr.split(":");
                if (parts.length >= 2) {
                    String materialId = parts[0].toUpperCase();
                    String amount = parts[1];
                    String itemName = plugin.getConfigManager().getItemName(materialId);
                    lore.add("<gray>- " + amount + "x " + itemName + "</gray>");
                }
            }

            lore.add("");
            lore.add("<green>¡Clic para reclamar!</green>");

            return ItemParser.createItem(
                pack.getIcon(),
                plugin.getConfigManager().getMessage("gui.food.pack-available", placeholders),
                lore,
                null
            );
        } else {
            // On cooldown
            String timeStr = plugin.getFoodService().formatTimeUntilAvailable(player, pack.getId());

            lore.add("<red>EN ESPERA</red>");
            lore.add("");
            lore.add("<gray>Disponible en: <yellow>" + timeStr + "</yellow></gray>");

            return ItemParser.createItem(
                Material.GRAY_DYE,
                plugin.getConfigManager().getMessage("gui.food.pack-cooldown", placeholders),
                lore,
                null
            );
        }
    }

    private void claimPack(String packId) {
        FoodService.ClaimResult result = plugin.getFoodService().claimPack(player, packId);

        if (result == FoodService.ClaimResult.SUCCESS) {
            // Refresh the GUI
            build();
        }
    }

    @Override
    public void updateCountdowns() {
        // Update pack items to show updated cooldowns
        Map<String, FoodPackDefinition> availablePacks = plugin.getFoodService().getAvailablePacks(player);

        for (int i = 0; i < PACK_IDS.length; i++) {
            String packId = PACK_IDS[i];
            int slot = PACK_SLOTS[i];

            FoodPackDefinition pack = availablePacks.get(packId);
            if (pack == null) {
                continue;
            }

            FoodService.PackStatus status = plugin.getFoodService().getPackStatus(player, packId);
            
            // Only update if on cooldown (countdown changes)
            if (status == FoodService.PackStatus.COOLDOWN) {
                ItemStack item = createPackItem(pack, status);
                inventory.setItem(slot, item);
            }
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
