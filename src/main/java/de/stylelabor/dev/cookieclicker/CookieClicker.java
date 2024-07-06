package de.stylelabor.dev.cookieclicker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class CookieClicker extends JavaPlugin implements Listener {


    private FileConfiguration languageConfig;
    private FileConfiguration upgradesConfig;
    private final HashMap<UUID, Integer> cookiesPerClickMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Setup locations config
        setupLocationsConfig();

        // Ensure config.yml is generated if it does not exist
        saveDefaultConfig();

        // Load upgrades
        getServer().getPluginManager().registerEvents(new UpgradeGUI(), this);

        // Load cookies per click values from storage
        loadCookiesPerClickFromStorage();

        // Load upgrades from config
        setupUpgradesConfig();

        // Load language file
        loadLanguageFile();

        getServer().getPluginManager().registerEvents(new ClickListener(this), this);

        // Register command executor
        if (this.getCommand("cookieclicker") != null) {
            Objects.requireNonNull(this.getCommand("cookieclicker")).setExecutor(new CookieClickerCommand(this));
            // Register tab completer
            Objects.requireNonNull(this.getCommand("cookieclicker")).setTabCompleter(new CookieClickerTabCompleter());
        } else {
            getLogger().severe("The cookieclicker command was not found. Please check your plugin.yml");
        }
    }

    private void loadLanguageFile() {
        File languageFile = new File(getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            saveResource("language.yml", false);
        }
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    private void setupUpgradesConfig() {
        File upgradesFile = new File(getDataFolder(), "upgrades.yml");
        if (!upgradesFile.exists()) {
            saveResource("upgrades.yml", false);
        }
        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
        getLogger().info("Upgrades config setup completed.");
    }

    public List<Upgrade> loadUpgrades() {
        List<Upgrade> upgrades = new ArrayList<>();
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection != null) {
            for (String key : upgradesSection.getKeys(false)) {
                // getLogger().info("Loading upgrade: " + key);
                String name = upgradesSection.getString(key + ".name");
                Material item = Material.valueOf(upgradesSection.getString(key + ".item"));
                int cost = upgradesSection.getInt(key + ".cost");
                int cookiesPerClick = upgradesSection.getInt(key + ".cookies-per-click");
                upgrades.add(new Upgrade(name, item, cost, cookiesPerClick));
            }
        } else {
            getLogger().severe("Failed to load upgrades section from config.");
        }
        // getLogger().info(upgrades.size() + " upgrades loaded.");
        return upgrades;
    }


    public String getLanguageMessage(String path, String defaultValue) {
        return languageConfig.getString("messages." + path, defaultValue);
    }

    public void loadCookiesPerClickFromStorage() {
        File cookiesPerClickFile = new File(getDataFolder(), "cookies-per-click.yml");
        if (!cookiesPerClickFile.exists()) {
            return; // File doesn't exist, nothing to load
        }
        FileConfiguration cookiesPerClickConfig = YamlConfiguration.loadConfiguration(cookiesPerClickFile);
        for (String key : cookiesPerClickConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            int cookiesPerClick = cookiesPerClickConfig.getInt(key);
            cookiesPerClickMap.put(playerUUID, cookiesPerClick);
        }
    }

    // Method to load cookies from cookies.yml with error handling for file creation
    public int loadCookies(Player player) {
        File cookiesFile = new File(getDataFolder(), "cookies.yml");
        if (!cookiesFile.exists()) {
            try {
                boolean fileCreated = cookiesFile.createNewFile();
                if (!fileCreated) {
                    getLogger().severe("Failed to create cookies.yml");
                    return 0; // Or handle this case as appropriate for your application
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create cookies.yml", e);
                return 0; // Or handle this case as appropriate for your application
            }
        }
        FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);
        return cookiesConfig.getInt(player.getUniqueId().toString(), 0); // Default to 0 if not found
    }

    // Method to load cookies per click for a player
    public int loadCookiesPerClick(Player player) {
        return cookiesPerClickMap.getOrDefault(player.getUniqueId(), 1); // Default to 1 if not found
    }

    public void saveCookiesPerClick(Player player, int cookiesPerClick) {
        File cookiesPerClickFile = new File(getDataFolder(), "cookies-per-click.yml");
        if (!cookiesPerClickFile.exists()) {
            try {
                if (!cookiesPerClickFile.createNewFile()) {
                    getLogger().warning("Failed to create cookies-per-click.yml as it already exists.");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create cookies-per-click.yml", e);
                return;
            }
        }
        FileConfiguration cookiesPerClickConfig = YamlConfiguration.loadConfiguration(cookiesPerClickFile);
        cookiesPerClickConfig.set(player.getUniqueId().toString(), cookiesPerClick);
        try {
            cookiesPerClickConfig.save(cookiesPerClickFile);
            getLogger().info("Successfully saved " + cookiesPerClick + " cookiesPerClick for player " + player.getUniqueId() + " to cookies-per-click.yml");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save cookiesPerClick to cookies-per-click.yml", e);
        }
    }


    public int getCookiesPerClick(Player player) {
        File cookiesPerClickFile = new File(getDataFolder(), "cookies-per-click.yml");
        if (!cookiesPerClickFile.exists()) {
            return 1; // Return default value if file doesn't exist
        }
        FileConfiguration cookiesPerClickConfig = YamlConfiguration.loadConfiguration(cookiesPerClickFile);
        return cookiesPerClickConfig.getInt(player.getUniqueId().toString(), 1); // Default to 1 if not found
    }

    // Method to save cookies to cookies.yml
    public void saveCookies(Player player, int cookies) {
        File cookiesFile = new File(getDataFolder(), "cookies.yml");
        FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);
        cookiesConfig.set(player.getUniqueId().toString(), cookies);
        try {
            cookiesConfig.save(cookiesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save cookies to cookies.yml", e);
        }
    }

    public void setupLocationsConfig() {
        File locationsFile = new File(getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            if (!locationsFile.getParentFile().mkdirs()) {
                getLogger().severe("Could not create directories for locations.yml");
            }
            saveResource("locations.yml", false);
        }
    }

    public String getGUITitle(String path, String defaultValue) {
        if (languageConfig == null) {
            loadLanguageFile(); // Ensure languageConfig is loaded
        }
        return languageConfig.getString("gui." + path, defaultValue);
    }

    // Method to load the clicker block location
    public Location getClickerBlockLocation() {
        File locationsFile = new File(getDataFolder(), "locations.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(locationsFile);
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString("clicker.location.world")));
        double x = config.getDouble("clicker.location.x");
        double y = config.getDouble("clicker.location.y");
        double z = config.getDouble("clicker.location.z");
        return new Location(world, x, y, z);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Dynamically fetch the title for the "Cookie Upgrades" GUI
        String cookieUpgradesTitle = getGUITitle("upgrade_title", "Cookie Upgrades");

        // Check if the inventory clicked has a title that matches one of your custom GUIs
        if (cookieUpgradesTitle.equals(event.getView().getTitle())) {
            // Prevent moving items in, out, or within the GUI
            event.setCancelled(true);
        }
    }

    public void updateCookiesPerClick(Player player, int cookiesPerClick) {
        // Update the map
        cookiesPerClickMap.put(player.getUniqueId(), cookiesPerClick);

        // Save to persistent storage
        saveCookiesPerClick(player, cookiesPerClick);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}