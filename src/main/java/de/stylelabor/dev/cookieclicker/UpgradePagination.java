package de.stylelabor.dev.cookieclicker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UpgradePagination implements Listener {

    private final CookieClicker plugin;
    private static final int UPGRADES_PER_PAGE = 7; // 9 slots in a row - 2 for leaving first and last cell empty

    public UpgradePagination(CookieClicker plugin) {
        this.plugin = plugin;
        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openInventory(Player player, int page) {
        List<Upgrade> upgrades = plugin.loadUpgrades();
        int totalPages = (int) Math.ceil((double) upgrades.size() / UPGRADES_PER_PAGE);
        Inventory inv = Bukkit.createInventory(null, 36, "Cookie Upgrades - Page " + (page + 1));

        for (int i = 0; i < UPGRADES_PER_PAGE; i++) {
            int index = page * UPGRADES_PER_PAGE + i;
            if (index >= upgrades.size()) break;
            Upgrade upgrade = upgrades.get(index);
            ItemStack item = new ItemStack(upgrade.getItem());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = ChatColor.translateAlternateColorCodes('&', upgrade.getName());
                meta.setDisplayName(displayName);
                // Call the method to create lore for this upgrade
                List<String> lore = createLoreForUpgrade(upgrade);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            int slot = calculateSlotIndex(i);
            inv.setItem(slot, item);
        }

        if (page > 0) {
            inv.setItem(27, createNavItem("Previous Page"));
        } else {
            inv.setItem(27, createNavItem("Back to Main Menu"));
        }
        if (page < totalPages - 1) {
            inv.setItem(35, createNavItem("Next Page"));
        }

        ItemStack fillerItem = createFillerItem();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, fillerItem);
            }
        }
        player.openInventory(inv);
    }

    private List<String> createLoreForUpgrade(Upgrade upgrade) {
        File upgradesFile = new File(plugin.getDataFolder(), "upgrades.yml");
        FileConfiguration upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);

        String upgradeKey = findUpgradeKeyByItem(upgradesConfig, upgrade.getItem());
        if (upgradeKey == null) {
            return new ArrayList<>(); // Return an empty list if the upgrade key is not found
        }

        List<String> rawLore = upgradesConfig.getStringList(upgradeKey + ".lore");
        List<String> processedLore = new ArrayList<>();

        for (String line : rawLore) {
            line = ChatColor.translateAlternateColorCodes('&', line);
            line = line.replace("%cookies_cost%", String.format(Locale.GERMANY, "%,d", upgrade.getCost()));
            line = line.replace("%cookies-per-click-gain%", String.format(Locale.GERMANY, "%,d", upgrade.getCookiesPerClick()));
            processedLore.add(line);
        }

        return processedLore;
    }

    // Helper method to find the upgrade key by matching the item material
    private String findUpgradeKeyByItem(FileConfiguration upgradesConfig, Material item) {
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null) return null;

        for (String key : upgradesSection.getKeys(false)) {
            Material configuredItem = Material.getMaterial(Objects.requireNonNull(upgradesSection.getString(key + ".item")));
            if (item.equals(configuredItem)) {
                return "upgrades." + key; // Return the full key path for the found upgrade
            }
        }
        return null;
    }

    private ItemStack createFillerItem() {
        ItemStack fillerItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName("*");
            fillerItem.setItemMeta(fillerMeta);
        }
        return fillerItem;
    }

    @SuppressWarnings("ConstantValue")
    private int calculateSlotIndex(int i) {
        int row = i / 7; // Calculate row number based on index, 7 items per row
        int slotInRow = i % 7; // Calculate the position in the row

        // Calculate the actual slot index, skipping the first and last slots of each row
        int slotIndex = row * 9; // Starting slot of the row
        if (slotInRow >= 7) { // If the slot is the 6th or 7th item in the row, move it to the next row
            slotIndex += slotInRow + 2; // Skip to the next row's second slot
        } else {
            slotIndex += slotInRow + 1; // Skip the first slot of the row
        }
        // Add an offset to start from the third row
        slotIndex += 18; // 9 slots/row * 2 rows = 18 slots to skip

        return slotIndex;
    }

    public static int getUpgradesPerPage() {
        return UPGRADES_PER_PAGE;
    }

    private ItemStack createNavItem(String name) {
        Material material;
        if ("Back to Main Menu".equals(name)) {
            material = Material.BARRIER;
        } else {
            material = Material.ARROW;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        // Check if the click is within the upgrade inventory and not the player's own inventory
        if (inv == null || clickedItem == null || !clickedItem.hasItemMeta() || inv.getType() != InventoryType.CHEST) return;

        String inventoryTitle = event.getView().getTitle();
        if (!inventoryTitle.contains("Cookie Upgrades - Page")) return;

        event.setCancelled(true); // Prevent default inventory click behavior

        // Determine the current page number from the inventory title
        int currentPage = Integer.parseInt(inventoryTitle.replaceAll("[^0-9]", "")) - 1;
        int totalPages = (int) Math.ceil((double) plugin.loadUpgrades().size() / UPGRADES_PER_PAGE);

        if (clickedItem.getType() == Material.ARROW && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().contains("Next Page")) {
            if (currentPage + 1 < totalPages) {
                openInventory(player, currentPage + 1);
            }
        } else if (clickedItem.getType() == Material.ARROW && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().contains("Previous Page")) {
            if (currentPage > 0) {
                openInventory(player, currentPage - 1);
            }
        } else if (clickedItem.getType() == Material.BARRIER && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().contains("Back to Main Menu")) {
            UpgradeGUI.openInventory(player);
        } else {

            String clickedItemName = ChatColor.stripColor(Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName());

            for (Upgrade upgrade : plugin.loadUpgrades()) {
                // Directly use the translated name for comparison, ensuring consistency with displayed names
                String upgradeName = ChatColor.translateAlternateColorCodes('&', upgrade.getName());
                upgradeName = ChatColor.stripColor(upgradeName); // Strip color codes for comparison

                if (clickedItemName.equals(upgradeName)) {
                    // Check if the player can afford the upgrade
                    if (CookieClicker.getUpgradeManager().canAffordUpgrade(player, upgrade)) {
                        // Process the upgrade purchase
                        CookieClicker.getUpgradeManager().processUpgradePurchase(player, upgrade);
                    }
                    break; // Exit the loop once the matching upgrade is found and processed
                }
            }
        }
    }
}