package de.stylelabor.dev.cookieclicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
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

        // Populate upgrades
        for (int i = 0; i < UPGRADES_PER_PAGE; i++) {
            int index = page * UPGRADES_PER_PAGE + i;
            if (index >= upgrades.size()) break;
            Upgrade upgrade = upgrades.get(index);
            ItemStack item = new ItemStack(upgrade.getItem());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(upgrade.getName() + " - Cost: " + upgrade.getCost());
                item.setItemMeta(meta);
            }
            int slot = calculateSlotIndex(i);
            inv.setItem(slot, item);
        }

        // Navigation items
        if (page > 0) {
            inv.setItem(27, createNavItem("Previous Page"));
        } else {
            // Add a "Back to Main Menu" item on the first page
            inv.setItem(27, createNavItem("Back to Main Menu"));
        }
        if (page < totalPages - 1) {
            inv.setItem(35, createNavItem("Next Page"));
        }

        player.openInventory(inv);
    }

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
        ItemStack item = new ItemStack(Material.ARROW);
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

        if (inv == null || clickedItem == null || !clickedItem.hasItemMeta()) return;

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
            // Open the GUI for upgrades
            player.closeInventory();
            UpgradeGUI.openInventory(player);
        } else {
            // Handle upgrade item clicks
            for (Upgrade upgrade : plugin.loadUpgrades()) {
                if (Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().contains(upgrade.getName())) {
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