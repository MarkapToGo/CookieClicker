package de.stylelabor.dev.cookieclicker;

import org.bukkit.entity.Player;

public class UpgradeManager {

    private final CookieClicker plugin;

    public UpgradeManager(CookieClicker plugin) {
        this.plugin = plugin;
    }

    public boolean canAffordUpgrade(Player player, Upgrade upgrade) {
        int currentCookies = plugin.loadCookies(player);
        return currentCookies >= upgrade.getCost();
    }

    public void processUpgradePurchase(Player player, Upgrade upgrade) {
        if (!canAffordUpgrade(player, upgrade)) {
            player.sendMessage("You do not have enough cookies to purchase this upgrade.");
            return;
        }

        int currentCookies = plugin.loadCookies(player);
        plugin.saveCookies(player, currentCookies - upgrade.getCost());
        applyUpgradeEffect(player, upgrade);
        player.sendMessage("Upgrade purchased successfully!");
    }

    private void applyUpgradeEffect(Player player, Upgrade upgrade) {
        int currentCookiesPerClick = plugin.loadCookiesPerClick(player);
        int newCookiesPerClick = currentCookiesPerClick + upgrade.getCookiesPerClick();
        // Assuming updateCookiesPerClick method exists and updates both the map and persistent storage
        plugin.updateCookiesPerClick(player, newCookiesPerClick);
        player.sendMessage("Your cookies per click have been increased to " + newCookiesPerClick + "!");
    }
}