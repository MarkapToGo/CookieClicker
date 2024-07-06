package de.stylelabor.dev.cookieclicker;

import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class UpgradeManager {

    private final CookieClicker plugin;
    private final Map<Player, LinkedList<BossBar>> playerBossBars = new HashMap<>();
    private final Set<UUID> processingUpgrades = ConcurrentHashMap.newKeySet();

    public UpgradeManager(CookieClicker plugin) {
        this.plugin = plugin;
    }

    public boolean canAffordUpgrade(Player player, Upgrade upgrade) {
        int currentCookies = plugin.loadCookies(player);
        return currentCookies >= upgrade.getCost();
    }

    public void processUpgradePurchase(Player player, Upgrade upgrade) {
        UUID playerId = player.getUniqueId();
        // Check if the upgrade is already being processed for this player
        if (!processingUpgrades.add(playerId)) {
            player.sendMessage("Upgrade is already being processed.");
            return;
        }

        if (!canAffordUpgrade(player, upgrade)) {
            player.sendMessage("You do not have enough cookies to purchase this upgrade.");
            processingUpgrades.remove(playerId); // Remove player from processing set
            return;
        }

        int currentCookies = plugin.loadCookies(player);
        plugin.saveCookies(player, currentCookies - upgrade.getCost());
        applyUpgradeEffect(player, upgrade);
        processingUpgrades.remove(playerId); // Remove player from processing set after completion
    }

    private void applyUpgradeEffect(Player player, Upgrade upgrade) {
        int currentCookiesPerClick = plugin.loadCookiesPerClick(player);
        int newCookiesPerClick = currentCookiesPerClick + upgrade.getCookiesPerClick();

        plugin.updateCookiesPerClick(player, newCookiesPerClick);

        BossBar bossBar = plugin.getServer().createBossBar(
                "Cookies per Click - " + newCookiesPerClick,
                BarColor.GREEN,
                BarStyle.SOLID);

        bossBar.addPlayer(player);
        bossBar.setProgress(1.0); // Start with full bar

        Sound sound = plugin.getPurchaseSound();
        float volume = plugin.getPurchaseVolume();
        float pitch = plugin.getPurchasePitch();
        player.playSound(player.getLocation(), sound, volume, pitch);

        final int durationInSeconds = 3; // Total time for the BossBar to disappear
        final long totalTicks = durationInSeconds * 20; // Convert seconds to ticks (20 ticks = 1 second)
        final double decrementPerTick = 1.0 / totalTicks; // Calculate decrement amount per tick

        LinkedList<BossBar> bossBars = playerBossBars.computeIfAbsent(player, k -> new LinkedList<>());
        if (bossBars.size() >= 3) {
            BossBar toRemove = bossBars.poll(); // Remove the oldest BossBar
            if (toRemove != null) {
                toRemove.removePlayer(player);
            }
        }
        bossBars.add(bossBar); // Add the new BossBar to the list

        // Schedule a task to decrease the BossBar's progress and manage BossBar removal
        final int[] taskId = new int[1]; // Use an array to effectively make taskId final
        taskId[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private long ticksPassed = 0;

            @Override
            public void run() {
                if (ticksPassed >= totalTicks) {
                    bossBar.removePlayer(player); // Remove the player from the BossBar
                    bossBars.remove(bossBar); // Also remove it from the tracking list
                    plugin.getServer().getScheduler().cancelTask(taskId[0]); // Cancel this task
                    return;
                }

                double newProgress = bossBar.getProgress() - decrementPerTick;
                bossBar.setProgress(Math.max(0, newProgress)); // Ensure progress does not go below 0
                ticksPassed++;
            }
        }, 0L, 1L); // Schedule to run every tick
    }
}