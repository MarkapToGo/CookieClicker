package de.stylelabor.dev.cookieclicker;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    public int calculatePlayerCookiesPerClickRank(Player player) {
        // Step 1 & 2: Fetch all players' cookies per click and sort them
        Map<Player, Integer> cookiesPerClickMap = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            int pCookiesPerClick = plugin.getCookiesPerClick(p);
            cookiesPerClickMap.put(p, pCookiesPerClick);
        }
        List<Map.Entry<Player, Integer>> sortedEntries = new ArrayList<>(cookiesPerClickMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int rank = 1; // Default to 1 in case the player is not found for some reason
        for (Map.Entry<Player, Integer> entry : sortedEntries) {
            if (entry.getKey().equals(player)) {
                break; // Found the player, break out of the loop
            }
            rank++;
        }
        return rank; // Return the calculated rank
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
                int rank = calculatePlayerCookiesPerClickRank(player);
                return rank >= 0 ? String.valueOf(rank) : "N/A"; // Return "N/A" if rank is -1 or any indicator for not found

            default:
                return null;
        }
    }
}