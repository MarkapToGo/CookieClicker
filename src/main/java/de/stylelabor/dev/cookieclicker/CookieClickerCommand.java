package de.stylelabor.dev.cookieclicker;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class CookieClickerCommand implements CommandExecutor {

    public final CookieClicker plugin;

    public CookieClickerCommand(CookieClicker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("setclicker") && sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock != null) {
                Location blockLocation = targetBlock.getLocation();
                File locationsFile = new File(plugin.getDataFolder(), "locations.yml");
                FileConfiguration locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
                locationsConfig.set("clicker.location.world", Objects.requireNonNull(blockLocation.getWorld()).getName());
                locationsConfig.set("clicker.location.x", blockLocation.getX());
                locationsConfig.set("clicker.location.y", blockLocation.getY());
                locationsConfig.set("clicker.location.z", blockLocation.getZ());
                try {
                    locationsConfig.save(locationsFile);
                    player.sendMessage("Clicker block set! You can change it in the locations.yml!");

                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save clicker block location to locations.yml", e);
                    player.sendMessage("Failed to save clicker block location.");
                }
                return true;
            } else {
                player.sendMessage("No block in range!");
                return false;
            }
        }
        return false;
    }
}