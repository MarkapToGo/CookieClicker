package de.stylelabor.dev.cookieclicker;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class CookieClicker extends JavaPlugin implements Listener {

    private FileConfiguration languageConfig;
    private FileConfiguration upgradesConfig;
    private final HashMap<UUID, Integer> cookiesPerClickMap = new HashMap<>();
    private static UpgradeManager upgradeManager; // Declare the upgradeManager variable

    @Override
    public void onEnable() {

        // Display startup message
        String[] startupMessage = {
                "############################",
                "##                        ##",
                "##    CookieClicker 🍪    ##",
                "##    coded by Markap     ##",
                "##                        ##",
                "############################"
        };

        for (String line : startupMessage) {
            getLogger().info(line);
        }

        // Setup locations config
        setupLocationsConfig();

        // Ensure config.yml is generated if it does not exist
        saveDefaultConfig();

        // Load language.yml configuration on plugin enable
        loadLanguageConfig();

        // Load upgrades
        getServer().getPluginManager().registerEvents(new UpgradeGUI(), this);

        // Load cookies per click values from storage
        loadCookiesPerClickFromStorage();

        // Load upgrades from config
        setupUpgradesConfig();

        // Initialize the UpgradeManager instance
        upgradeManager = new UpgradeManager(this);

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

    public static UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public void showTotalCookiesInActionBar(Player player) {
        int totalCookies = getCurrentCookies(player); // Assuming this method exists and returns the total cookies for the player
        String messageTemplate = languageConfig.getString("total_cookies_actionbar", "Total Cookies: %total_cookies%");
        String messageWithCookies = messageTemplate.replace("%total_cookies_point%", String.format(Locale.GERMAN, "%,d", totalCookies));
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', messageWithCookies);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(coloredMessage));
    }

    public int getCurrentCookies(Player player) {
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = conn.prepareStatement("SELECT cookies FROM player_cookies WHERE uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cookies");
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not load current cookies from MySQL", e);
            }
            // Return default value if not found or error
            getLogger().info("[Debug] Defaulting to 0 currentCookies for player: " + player.getUniqueId() + " due to error or not found in MySQL.");
            return 0;
        } else {
            // YAML logic
            File cookiesFile = new File(getDataFolder(), "cookies.yml");
            if (!cookiesFile.exists()) {
                getLogger().info("[Debug] cookies.yml does not exist. Returning default value for player: " + player.getUniqueId());
                return 0; // Return default value if file doesn't exist
            }
            FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);
            return cookiesConfig.getInt(player.getUniqueId().toString(), 0); // Default to 0 if not found
        }
    }

    private void setupUpgradesConfig() {
        File upgradesFile = new File(getDataFolder(), "upgrades.yml");
        if (!upgradesFile.exists()) {
            saveResource("upgrades.yml", false);
        }
        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
        getLogger().info("Upgrades config setup completed.");
    }

    // Method to determine the storage type
    public boolean useMySQL() {
        return "MYSQL".equalsIgnoreCase(this.getConfig().getString("storage.type"));
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

    public String getMessage(String key) {
        if (languageConfig == null) {
            loadLanguageConfig();
        }
        String message = languageConfig.getString(key, "Key not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void loadLanguageConfig() {
        File languageFile = new File(getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            saveResource("language.yml", false);
        }
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    public int loadCookies(Player player) {
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = Objects.requireNonNull(conn).prepareStatement("SELECT cookies FROM player_cookies WHERE uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cookies");
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not load cookies from MySQL", e);
            }
            return 0; // Default to 0 if not found or error
        } else {
            // YAML logic
            File cookiesFile = new File(getDataFolder(), "cookies.yml");
            FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);
            return cookiesConfig.getInt(player.getUniqueId().toString(), 0); // Default to 0 if not found
        }
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

    // Method to load cookies per click for a player
    public synchronized int loadCookiesPerClick(Player player) {
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = Objects.requireNonNull(conn).prepareStatement("SELECT cookies_per_click FROM cookies_per_click WHERE uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cookies_per_click");
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not load cookies per click from MySQL", e);
            }
            return 1; // Default to 1 if not found or error
        } else {
            // YAML logic
            return cookiesPerClickMap.getOrDefault(player.getUniqueId(), 1); // Default to 1 if not found
        }
    }

    public void saveCookiesPerClick(Player player, int cookiesPerClick) {
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = Objects.requireNonNull(conn).prepareStatement("REPLACE INTO cookies_per_click (uuid, cookies_per_click) VALUES (?, ?)")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, cookiesPerClick);
                ps.executeUpdate();
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not save cookies per click to MySQL", e);
            }
        } else {
            // YAML logic
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
    }

    public int getCookiesPerClick(Player player) {
        // Check if the storage method is set to use MySQL
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = conn.prepareStatement("SELECT cookies_per_click FROM cookies_per_click WHERE uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int cookiesPerClick = rs.getInt("cookies_per_click");
                        getLogger().info("[Debug] Fetched " + cookiesPerClick + " cookiesPerClick for player: " + player.getUniqueId());
                        return cookiesPerClick;
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not load cookies per click from MySQL", e);
            }
            // Return default value if not found or error
            getLogger().info("[Debug] Defaulting to 1 cookiesPerClick for player: " + player.getUniqueId() + " due to error or not found in MySQL.");
            return 1;
        } else {
            // YAML logic
            File cookiesPerClickFile = new File(getDataFolder(), "cookies-per-click.yml");
            if (!cookiesPerClickFile.exists()) {
                getLogger().info("[Debug] cookies-per-click.yml does not exist. Returning default value for player: " + player.getUniqueId());
                return 1; // Return default value if file doesn't exist
            }
            FileConfiguration cookiesPerClickConfig = YamlConfiguration.loadConfiguration(cookiesPerClickFile);
            int cookiesPerClick = cookiesPerClickConfig.getInt(player.getUniqueId().toString(), 1); // Default to 1 if not found
            getLogger().info("[Debug] Fetched " + cookiesPerClick + " cookiesPerClick for player: " + player.getUniqueId());
            return cookiesPerClick;
        }
    }

    // Method to save cookies to cookies.yml
    public void saveCookies(Player player, int cookies) {
        if (useMySQL()) {
            // MySQL logic
            try (Connection conn = MySQLUtil.getConnection(this);
                 PreparedStatement ps = Objects.requireNonNull(conn).prepareStatement("REPLACE INTO player_cookies (uuid, cookies) VALUES (?, ?)")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, cookies);
                ps.executeUpdate();
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Could not save cookies to MySQL", e);
            }
        } else {
            // YAML logic
            File cookiesFile = new File(getDataFolder(), "cookies.yml");
            FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);
            cookiesConfig.set(player.getUniqueId().toString(), cookies);
            try {
                cookiesConfig.save(cookiesFile);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not save cookies to cookies.yml", e);
            }
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

    public Sound getPurchaseSound() {
        String soundName = getConfig().getString("purchase-sound.sound", "ENTITY_PLAYER_LEVELUP");
        return Sound.valueOf(soundName);
    }

    public float getPurchaseVolume() {
        return (float) getConfig().getDouble("purchase-sound.volume", 1.0);
    }

    public float getPurchasePitch() {
        return (float) getConfig().getDouble("purchase-sound.pitch", 1.0);
    }

    public synchronized void updateCookiesPerClick(Player player, int cookiesPerClick) {
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