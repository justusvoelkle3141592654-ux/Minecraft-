package de.backrooms.befehle;

import de.backrooms.BackroomsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** /start – Ausrüstung bekommen und in die Backrooms fallen. */
public class StartBefehl implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "backrooms.start";

    private final BackroomsPlugin plugin;

    public StartBefehl(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können /" + label + " benutzen.");
            return true;
        }
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dazu fehlt dir die Berechtigung.");
            return true;
        }
        plugin.getSpielablauf().betreten((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
