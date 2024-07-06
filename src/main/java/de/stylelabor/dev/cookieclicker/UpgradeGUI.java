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

        // Place the upgrades item in the third row, 7th cell
        inv.setItem(25, upgradesItem);


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
            // Check if the clicked item is not null and is the green concrete block
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.GREEN_CONCRETE) {
                // Prevent the default behaviour
                event.setCancelled(true);
                // Open the upgrade GUI for the player
                Player player = (Player) event.getWhoClicked();
                openUpgradeGUI(player, plugin);
            } else {
                // Prevent any other interaction within the GUI
                event.setCancelled(true);
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
        Inventory upgradeInv = Bukkit.createInventory(null, 45, "Cookie Upgrades"); // 5 rows x 9 slots = 45 slots
        List<Upgrade> upgrades = JavaPlugin.getPlugin(CookieClicker.class).loadUpgrades();

        for (int i = 0; i < upgrades.size(); i++) {
            Upgrade upgrade = upgrades.get(i);
            ItemStack item = new ItemStack(upgrade.getItem());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(upgrade.getName() + " - Cost: " + upgrade.getCost());
                item.setItemMeta(meta);
            }
            upgradeInv.setItem(18 + 2 + i, item); // Place in 3rd row, starting from 2nd slot
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

}