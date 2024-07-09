package de.stylelabor.dev.cookieclicker;

import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class UpgradeManager {
    private final CookieClicker plugin;
    private final Map<Player, LinkedList<BossBar>> playerBossBars = new HashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> processingUpgrades = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastUpgradePurchaseTime = new ConcurrentHashMap<>();

    public UpgradeManager(CookieClicker plugin) {
        this.plugin = plugin;
    }

    public boolean canAffordUpgrade(Player player, Upgrade upgrade) {
        int currentCookies = plugin.loadCookies(player);
        return currentCookies >= upgrade.getCost();
    }

    public synchronized void processUpgradePurchase(Player player, Upgrade upgrade) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = lastUpgradePurchaseTime.getOrDefault(playerId, 0L);

        long upgradeCooldownMillis = 500;
        if (currentTime - lastPurchaseTime < upgradeCooldownMillis) {
            plugin.getLogger().info("Upgrade purchase attempt blocked due to cooldown for player: " + player.getName());
            return; // Block the purchase if it's within the cooldown period
        }

        lastUpgradePurchaseTime.put(playerId, currentTime); // Update the last purchase time

        plugin.getLogger().info("Attempting to process upgrade purchase for player: " + player.getName());

        Boolean isProcessing = processingUpgrades.putIfAbsent(playerId, true);
        if (isProcessing != null && isProcessing) {
            plugin.getLogger().info("Purchase blocked due to ongoing processing for player: " + player.getName());
            return;
        }

        try {
            if (!canAffordUpgrade(player, upgrade)) {
                plugin.getLogger().info("Player " + player.getName() + " cannot afford upgrade.");
                return;
            }

            int currentCookies = plugin.loadCookies(player);
            plugin.saveCookies(player, currentCookies - upgrade.getCost());
            applyUpgradeEffect(player, upgrade);
            plugin.getLogger().info("Upgrade effect applied to player: " + player.getName());
        } finally {
            processingUpgrades.remove(playerId);
        }
    }

    public synchronized void applyUpgradeEffect(Player player, Upgrade upgrade) {
        int currentCookiesPerClick = plugin.loadCookiesPerClick(player);
        int newCookiesPerClick = currentCookiesPerClick + upgrade.getCookiesPerClick();
        plugin.getLogger().info("Applying upgrade effect: " + player.getName() + " currentCookiesPerClick: " + currentCookiesPerClick + ", newCookiesPerClick: " + newCookiesPerClick);

        plugin.updateCookiesPerClick(player, newCookiesPerClick);

        BossBar bossBar = plugin.getServer().createBossBar(
                "Cookies per Click - " + newCookiesPerClick,
                BarColor.GREEN,
                BarStyle.SOLID);

        bossBar.addPlayer(player);
        bossBar.setProgress(1.0); // Start with full bar
        plugin.getLogger().info("Displayed BossBar for player: " + player.getName());

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