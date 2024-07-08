package de.stylelabor.dev.cookieclicker;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class CookieClickerExpansion extends PlaceholderExpansion {

    private final CookieClicker plugin;

    public CookieClickerExpansion(CookieClicker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true; // This makes sure the expansion does not unregister on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cookieclicker";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public int calculatePlayerRank(Player player) {
        // Load all players' cookies into a map
        Map<UUID, Integer> allPlayerCookies = new HashMap<>();
        File cookiesFile = new File(plugin.getDataFolder(), "cookies.yml");
        FileConfiguration cookiesConfig = YamlConfiguration.loadConfiguration(cookiesFile);

        for (String key : cookiesConfig.getKeys(false)) {
            allPlayerCookies.put(UUID.fromString(key), cookiesConfig.getInt(key));
        }

        // Create a list from elements of HashMap
        List<Map.Entry<UUID, Integer>> list = new LinkedList<>(allPlayerCookies.entrySet());

        // Sort the list
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Find the rank of the current player
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : list) {
            if (entry.getKey().equals(player.getUniqueId())) {
                return rank;
            }
            rank++;
        }

        // Return -1 or any indicator for not found
        return -1;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier) {
            case "total_cookies":
                int totalCookies = plugin.loadCookies(player);
                return String.valueOf(totalCookies);

            case "total_cookies_point":
                int totalCookiesPoint = plugin.loadCookies(player);
                return String.format("%,d", totalCookiesPoint);

            case "cookies_per_click":
                int cookiesPerClick = plugin.getCookiesPerClick(player);
                return String.valueOf(cookiesPerClick);

            case "cookies_per_click_point":
                int cookiesPerClickPoint = plugin.getCookiesPerClick(player);
                return String.format("%,d", cookiesPerClickPoint);

            case "rank":
                int rank = calculatePlayerRank(player);
                return rank >= 0 ? String.valueOf(rank) : "N/A"; // Return "N/A" if rank is -1 or any indicator for not found

            default:
                return null;
        }
    }
}