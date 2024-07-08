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

import java.util.*;

public class UpgradeGUI implements Listener {

    public static void openInventory(Player player) {
        CookieClicker plugin = JavaPlugin.getPlugin(CookieClicker.class);
        String guiTitle = plugin.getGUITitle("upgrade_title", "Cookie Upgrades");
        Inventory inv = Bukkit.createInventory(null, 45, guiTitle); // Use the fetched title

        // Fetch the player's cookies per click and current cookies
        int cookiesPerClick = plugin.getCookiesPerClick(player);
        int currentCookies = plugin.getCurrentCookies(player); // Assuming there's a method to fetch current cookies

        // Fetch translated names for special items and replace placeholders
        String cookiesItemName = plugin.getMessage("gui.current_cookies_item").replace("%current_cookies_point%", String.format(Locale.GERMAN, "%,d", currentCookies));
        String cookiesPerClickItemName = plugin.getMessage("gui.cookies_per_click_item").replace("%cookies_per_click%", String.format(Locale.GERMAN, "%,d", cookiesPerClick));

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
        inv.setItem(24, upgradesItem);





        // Step 1 & 2: Fetch all players' cookies per click and sort them
        Map<Player, Integer> cookiesPerClickMap = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            int pCookiesPerClick = plugin.getCookiesPerClick(p);
            cookiesPerClickMap.put(p, pCookiesPerClick);
        }
        List<Map.Entry<Player, Integer>> sortedEntries = new ArrayList<>(cookiesPerClickMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));


        //noinspection ExtractMethodRecommender
        int rank = 1; // Default to 1 in case the player is not found for some reason
        for (Map.Entry<Player, Integer> entry : sortedEntries) {
            if (entry.getKey().equals(player)) {
                break; // Found the player, break out of the loop
            }
            rank++;
        }

        // Step 4: Display the ranking in the GUI
        ItemStack rankItem = new ItemStack(Material.GOLD_NUGGET); // Assuming the material
        ItemMeta rankMeta = rankItem.getItemMeta();
        if (rankMeta != null) {
            rankMeta.setDisplayName("Your Ranking: #" + rank);
            rankItem.setItemMeta(rankMeta);
        }
        inv.setItem(22, rankItem); // Assuming the position







        //noinspection DuplicatedCode
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) { // Check if the slot is empty
                ItemStack fillerItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE); // Create a dark gray stained-glass pane
                ItemMeta fillerMeta = fillerItem.getItemMeta();
                if (fillerMeta != null) {
                    fillerMeta.setDisplayName("*"); // Set the display name to "*"
                    fillerItem.setItemMeta(fillerMeta);
                }
                inv.setItem(i, fillerItem); // Place the filler item in the empty slot
            }
        }

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
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && Objects.requireNonNull(clickedItem.getItemMeta()).hasDisplayName()) {
                Player player = (Player) event.getWhoClicked();

                List<Upgrade> upgrades = plugin.loadUpgrades();
                for (Upgrade upgrade : upgrades) {
                    if (clickedItem.getType() == upgrade.getItem() && clickedItem.getItemMeta().getDisplayName().contains(upgrade.getName())) {
                        UpgradeManager upgradeManager = new UpgradeManager(plugin);
                        if (upgradeManager.canAffordUpgrade(player, upgrade)) {
                            upgradeManager.processUpgradePurchase(player, upgrade);
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have enough cookies to purchase this upgrade.");
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

    public static void openUpgradeGUI(Player player, CookieClicker plugin) {
        List<Upgrade> upgrades = JavaPlugin.getPlugin(CookieClicker.class).loadUpgrades();
        int totalPages = (int) Math.ceil((double) upgrades.size() / UpgradePagination.getUpgradesPerPage());
        Inventory inv = Bukkit.createInventory(null, 45);

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

            //noinspection DuplicatedCode
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) { // Check if the slot is empty
                    ItemStack fillerItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE); // Create a dark gray stained-glass pane
                    ItemMeta fillerMeta = fillerItem.getItemMeta();
                    if (fillerMeta != null) {
                        fillerMeta.setDisplayName("*"); // Set the display name to "*"
                        fillerItem.setItemMeta(fillerMeta);
                    }
                    inv.setItem(i, fillerItem); // Place the filler item in the empty slot
                }
            }

            upgradeInv.setItem(6, currentCookiesItem);
            player.openInventory(upgradeInv);
        }
    }
}