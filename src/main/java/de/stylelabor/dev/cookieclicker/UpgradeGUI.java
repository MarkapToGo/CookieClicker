package de.stylelabor.dev.cookieclicker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        // Fetch the player's cookies per click and current cookies
        int cookiesPerClick = plugin.getCookiesPerClick(player);
        int currentCookies = plugin.getCurrentCookies(player); // Assuming there's a method to fetch current cookies

        plugin.getLogger().info("Fetched cookies per click for player " + player.getName() + ": " + cookiesPerClick);
        plugin.getLogger().info("Fetched current cookies for player " + player.getName() + ": " + currentCookies);

        // Fetch translated names for special items and replace placeholders
        String cookiesItemName = plugin.getMessage("gui.current_cookies_item").replace("%current_cookies%", String.valueOf(currentCookies));
        String cookiesPerClickItemName = plugin.getMessage("gui.cookies_per_click_item").replace("%cookies_per_click%", String.valueOf(cookiesPerClick));


        // Create a red concrete block to display "Design"
        ItemStack designItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta designMeta = designItem.getItemMeta();
        if (designMeta != null) {
            designMeta.setDisplayName("Design");
            designItem.setItemMeta(designMeta);
        }

        // Place the design item in the 4th row, middle slot
        inv.setItem(31, designItem); // Inventory slots start at 0, so slot 31 is the 4th row, middle slot

        // Create and add cookies per click item
        ItemStack cookiesPerClickItem = new ItemStack(Material.YELLOW_CONCRETE); // Assuming the material
        ItemMeta cookiesPerClickMeta = cookiesPerClickItem.getItemMeta();
        if (cookiesPerClickMeta != null) {
            cookiesPerClickMeta.setDisplayName(cookiesPerClickItemName);
            cookiesPerClickItem.setItemMeta(cookiesPerClickMeta);
        }
        inv.setItem(20, cookiesPerClickItem);

        // Create and add cookies item
        ItemStack cookiesItem = new ItemStack(Material.BROWN_DYE); // Assuming the material
        ItemMeta cookiesMeta = cookiesItem.getItemMeta();
        if (cookiesMeta != null) {
            cookiesMeta.setDisplayName(cookiesItemName);
            cookiesItem.setItemMeta(cookiesMeta);
        }
        inv.setItem(13, cookiesItem);

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
            UpgradePagination pagination = new UpgradePagination(plugin);
            pagination.openInventory(player, 0);
        } else {
            Inventory upgradeInv = Bukkit.createInventory(null, 45, ChatColor.translateAlternateColorCodes('&', plugin.getGUITitle("upgrade_title", "Cookie Upgrades")));

            for (int i = 0; i < upgrades.size(); i++) {
                Upgrade upgrade = upgrades.get(i);
                ItemStack item = new ItemStack(upgrade.getItem());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(upgrade.getName() + " - Cost: " + upgrade.getCost());
                    item.setItemMeta(meta);
                }
                upgradeInv.setItem(17 + 2 + i, item);
            }

            int cookiesPerClick = plugin.loadCookiesPerClick(player);
            int currentCookies = plugin.loadCookies(player);

            String cookiesPerClickItemName = ChatColor.translateAlternateColorCodes('&', plugin.getMessage("gui.cookies_per_click_item").replace("%cookies_per_click%", String.valueOf(cookiesPerClick)));
            ItemStack cookiesPerClickItem = new ItemStack(Material.YELLOW_TERRACOTTA);
            ItemMeta cookiesPerClickMeta = cookiesPerClickItem.getItemMeta();
            if (cookiesPerClickMeta != null) {
                cookiesPerClickMeta.setDisplayName(cookiesPerClickItemName);
                cookiesPerClickItem.setItemMeta(cookiesPerClickMeta);
            }
            upgradeInv.setItem(22, cookiesPerClickItem);

            String currentCookiesItemName = ChatColor.translateAlternateColorCodes('&', plugin.getMessage("gui.current_cookies_item").replace("%current_cookies%", String.valueOf(currentCookies)));
            ItemStack currentCookiesItem = new ItemStack(Material.BROWN_TERRACOTTA);
            ItemMeta currentCookiesMeta = currentCookiesItem.getItemMeta();
            if (currentCookiesMeta != null) {
                currentCookiesMeta.setDisplayName(currentCookiesItemName);
                currentCookiesItem.setItemMeta(currentCookiesMeta);
            }
            upgradeInv.setItem(6, currentCookiesItem);

            player.openInventory(upgradeInv);
        }
    }

}