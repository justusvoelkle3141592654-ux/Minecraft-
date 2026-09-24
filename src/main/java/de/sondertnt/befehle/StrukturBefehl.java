package de.sondertnt.befehle;

import de.sondertnt.SonderTNTPlugin;
import de.sondertnt.struktur.Struktur;
import de.sondertnt.struktur.StrukturManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /struktur bauen &lt;name&gt; | list */
public class StrukturBefehl implements CommandExecutor, TabCompleter {

    private final StrukturManager manager;

    public StrukturBefehl(StrukturManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(SonderTntBefehl.PERMISSION)) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Dazu fehlt dir die Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            hilfe(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            auflisten(sender);
        } else if (sub.equals("bauen")) {
            bauen(sender, label, args);
        } else {
            hilfe(sender, label);
        }
        return true;
    }

    private void hilfe(CommandSender sender, String label) {
        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GOLD + "Befehle:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " bauen <name>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
    }

    private void auflisten(CommandSender sender) {
        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GOLD + "Strukturen:");
        for (Struktur s : manager.alle()) {
            sender.sendMessage(ChatColor.YELLOW + "- " + s.getId() + ChatColor.GRAY + " (" + s.getAnzeigename() + ", "
                    + s.getBreite() + "x" + s.getTiefe() + "x" + s.getHoehe() + "): " + s.getBeschreibung());
        }
    }

    private void bauen(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Nur Spieler können Strukturen bauen.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Benutzung: /" + label + " bauen <name>");
            return;
        }
        Struktur struktur = manager.get(args[1]);
        if (struktur == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Unbekannte Struktur '" + args[1]
                    + "'. Verfügbar: " + String.join(", ", manager.alleIds()));
            return;
        }
        Location ort = manager.vorSpielerBauen((Player) sender, struktur);
        if (ort == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Hier ist nicht genug Platz nach oben oder unten.");
            return;
        }
        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + struktur.getAnzeigename() + " gebaut bei "
                + ort.getBlockX() + " " + ort.getBlockY() + " " + ort.getBlockZ() + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(SonderTntBefehl.PERMISSION)) {
            return Collections.emptyList();
        }
        List<String> optionen;
        if (args.length == 1) {
            optionen = Arrays.asList("bauen", "list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bauen")) {
            optionen = manager.alleIds();
        } else {
            return Collections.emptyList();
        }
        List<String> treffer = new ArrayList<String>();
        StringUtil.copyPartialMatches(args[args.length - 1], optionen, treffer);
        Collections.sort(treffer);
        return treffer;
    }
}
