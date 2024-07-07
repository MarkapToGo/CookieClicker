package de.stylelabor.dev.cookieclicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public class UpgradeGUI implements Listener {

    public static void openInventory(Player player) {
        CookieClicker plugin = JavaPlugin.getPlugin(CookieClicker.class);
        String guiTitle = plugin.getGUITitle("upgrade_title", "Cookie Upgrades");
        Inventory inv = Bukkit.createInventory(null, 45, guiTitle); // Use the fetched title

        // Fetch the player's cookie count
        int cookies = plugin.loadCookies(player);

        // Create a brown concrete block to display the cookie count
        ItemStack cookieCounter = new ItemStack(Material.BROWN_CONCRETE);
        ItemMeta meta = cookieCounter.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Cookies: " + cookies);
            cookieCounter.setItemMeta(meta);
        }

        // Place the cookie counter in the middle of the second row
        inv.setItem(13, cookieCounter);

        // Create a green concrete block called "Upgrades!"
        ItemStack upgradesItem = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta upgradesMeta = upgradesItem.getItemMeta();
        if (upgradesMeta != null) {
            upgradesMeta.setDisplayName("Upgrades!");
            upgradesItem.setItemMeta(upgradesMeta);
        }

        // Place the upgrades item in the third row, 6th cell
        inv.setItem(24, upgradesItem);


        player.openInventory(inv);

        // Play sound
        String soundName = plugin.getConfig().getString("gui_open_sound.sound", "BLOCK_BARREL_OPEN");
        float volume = (float) plugin.getConfig().getDouble("gui_open_sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("gui_open_sound.pitch", 1.0);
        player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        CookieClicker plugin = JavaPlugin.getPlugin(CookieClicker.class);
        String guiTitle = plugin.getGUITitle("upgrade_title", "Cookie Upgrades");
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true); // Prevent any inventory action

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && Objects.requireNonNull(clickedItem.getItemMeta()).hasDisplayName()) {
                List<Upgrade> upgrades = plugin.loadUpgrades();
                Player player = (Player) event.getWhoClicked();

                for (Upgrade upgrade : upgrades) {
                    if (clickedItem.getType() == upgrade.getItem() && clickedItem.getItemMeta().getDisplayName().contains(upgrade.getName())) {
                        UpgradeManager upgradeManager = new UpgradeManager(plugin);
                        if (upgradeManager.canAffordUpgrade(player, upgrade)) {
                            upgradeManager.processUpgradePurchase(player, upgrade);
                            // player.closeInventory(); // Close the inventory after purchase
                            // openUpgradeGUI(player, plugin); // Re-open the inventory to reflect changes
                        } else {
                            player.sendMessage("You do not have enough cookies to purchase this upgrade.");
                        }
                        return; // Exit the method after handling the upgrade item
                    }
                }

                // If the clicked item is the green concrete block, open the upgrade GUI
                if (clickedItem.getType() == Material.GREEN_CONCRETE) {
                    openUpgradeGUI(player, plugin);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        CookieClicker plugin = JavaPlugin.getPlugin(CookieClicker.class);
        String guiTitle = plugin.getGUITitle("upgrade_title", "Cookie Upgrades");
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true); // Prevent dragging items within the GUI
        }
    }

    public void openUpgradeGUI(Player player, CookieClicker plugin) {
        List<Upgrade> upgrades = JavaPlugin.getPlugin(CookieClicker.class).loadUpgrades();
        int totalPages = (int) Math.ceil((double) upgrades.size() / UpgradePagination.getUpgradesPerPage());

        if (totalPages > 1) {
            // If more than one page is needed, use UpgradePagination to handle it
            UpgradePagination pagination = new UpgradePagination(plugin);
            pagination.openInventory(player, 0); // Open the first page
        } else {
            // If all upgrades fit on one page, use the existing logic to display them
            Inventory upgradeInv = Bukkit.createInventory(null, 45, "Cookie Upgrades"); // 5 rows x 9 slots = 45 slots

            for (int i = 0; i < upgrades.size(); i++) {
                Upgrade upgrade = upgrades.get(i);
                ItemStack item = new ItemStack(upgrade.getItem());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(upgrade.getName() + " - Cost: " + upgrade.getCost());
                    item.setItemMeta(meta);
                }
                upgradeInv.setItem(17 + 2 + i, item); // Adjust the slot placement as needed
            }

            // Fetch the player's cookies per click and current cookies
            int cookiesPerClick = plugin.getCookiesPerClick(player);
            int currentCookies = plugin.loadCookies(player);

            // Create an orange terracotta block to display cookies per click
            ItemStack cookiesPerClickItem = new ItemStack(Material.ORANGE_TERRACOTTA);
            ItemMeta cookiesPerClickMeta = cookiesPerClickItem.getItemMeta();
            if (cookiesPerClickMeta != null) {
                cookiesPerClickMeta.setDisplayName("Cookies per Click: " + cookiesPerClick);
                cookiesPerClickItem.setItemMeta(cookiesPerClickMeta);
            }

            // Create a brown terracotta block to display current cookies
            ItemStack currentCookiesItem = new ItemStack(Material.BROWN_TERRACOTTA);
            ItemMeta currentCookiesMeta = currentCookiesItem.getItemMeta();
            if (currentCookiesMeta != null) {
                currentCookiesMeta.setDisplayName("Current Cookies: " + currentCookies);
                currentCookiesItem.setItemMeta(currentCookiesMeta);
            }

            // Place the items in the specified slots
            upgradeInv.setItem(2, cookiesPerClickItem); // 3rd slot (index 2)
            upgradeInv.setItem(6, currentCookiesItem); // 7th slot (index 6)
            player.openInventory(upgradeInv);
        }

    }}