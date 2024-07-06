package de.stylelabor.dev.cookieclicker;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public class ClickListener implements Listener {

    private final CookieClicker plugin;

    public ClickListener(CookieClicker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return; // Exit the method if there's no block involved in the event
        }

        Location clickerLocation = plugin.getClickerBlockLocation();
        Location eventLocation = event.getClickedBlock().getLocation();

        if (Objects.equals(eventLocation.getWorld(), clickerLocation.getWorld()) &&
                eventLocation.getBlockX() == clickerLocation.getBlockX() &&
                eventLocation.getBlockY() == clickerLocation.getBlockY() &&
                eventLocation.getBlockZ() == clickerLocation.getBlockZ()) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Open the GUI for upgrades
                UpgradeGUI.openInventory(event.getPlayer());
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {

                int cookies = plugin.loadCookies(event.getPlayer()) + plugin.loadCookiesPerClick(event.getPlayer()); // Increment cookies by cookies per click
                plugin.saveCookies(event.getPlayer(), cookies); // Save new cookie count

                // Send action bar message
                event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("Cookies - " + cookies));

                // Play sound
                String soundName = plugin.getConfig().getString("click-sound.sound", "BLOCK_NOTE_BLOCK_PLING");
                float volume = (float) plugin.getConfig().getDouble("click-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("click-sound.pitch", 1.0);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.valueOf(soundName), volume, pitch);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location clickerLocation = plugin.getClickerBlockLocation();
        Location blockLocation = event.getBlock().getLocation();

        if (Objects.equals(blockLocation.getWorld(), clickerLocation.getWorld()) &&
                blockLocation.getBlockX() == clickerLocation.getBlockX() &&
                blockLocation.getBlockY() == clickerLocation.getBlockY() &&
                blockLocation.getBlockZ() == clickerLocation.getBlockZ()) {
            event.setCancelled(true); // Prevent the block from being broken
        }
    }

}