package de.sondertnt.befehle;

import de.sondertnt.SonderTNTPlugin;
import de.sondertnt.tnt.SonderTntItems;
import de.sondertnt.tnt.TntTyp;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** /sondertnt give &lt;Spieler&gt; &lt;typ&gt; [anzahl] | list | reload */
public class SonderTntBefehl implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "sondertnt.admin";
    private static final int MAX_ANZAHL = 64 * 36;

    private final SonderTNTPlugin plugin;

    public SonderTntBefehl(SonderTNTPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Dazu fehlt dir die Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            hilfe(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("give")) {
            geben(sender, label, args);
        } else if (sub.equals("list")) {
            auflisten(sender);
        } else if (sub.equals("reload")) {
            int rezepte = plugin.neuLaden();
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + "Konfiguration neu geladen ("
                    + rezepte + " Rezept(e) aktiv).");
        } else {
            hilfe(sender, label);
        }
        return true;
    }

    private void hilfe(CommandSender sender, String label) {
        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GOLD + "Befehle:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <Spieler> <typ> [anzahl]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }

    private void geben(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED
                    + "Benutzung: /" + label + " give <Spieler> <typ> [anzahl]");
            return;
        }
        Player ziel = plugin.getServer().getPlayerExact(args[1]);
        if (ziel == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Spieler '" + args[1] + "' ist nicht online.");
            return;
        }
        TntTyp typ = TntTyp.vonId(args[2]);
        if (typ == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Unbekannter Typ '" + args[2]
                    + "'. Verfügbar: " + String.join(", ", TntTyp.alleIds()));
            return;
        }
        int anzahl = 1;
        if (args.length >= 4) {
            try {
                anzahl = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "'" + args[3] + "' ist keine Zahl.");
                return;
            }
            if (anzahl < 1 || anzahl > MAX_ANZAHL) {
                sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED
                        + "Die Anzahl muss zwischen 1 und " + MAX_ANZAHL + " liegen.");
                return;
            }
        }

        // In Stapel zu je 64 aufteilen; was nicht passt, wird vor dem Spieler fallen gelassen
        int rest = anzahl;
        while (rest > 0) {
            int stapel = Math.min(64, rest);
            rest -= stapel;
            Map<Integer, ItemStack> uebrig = ziel.getInventory().addItem(SonderTntItems.erstellen(typ, stapel));
            for (ItemStack item : uebrig.values()) {
                ziel.getWorld().dropItemNaturally(ziel.getLocation(), item);
            }
        }

        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + ziel.getName() + " hat " + anzahl + "x "
                + typ.getAnzeigename() + ChatColor.GREEN + " erhalten.");
        if (sender != ziel) {
            ziel.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + "Du hast " + anzahl + "x "
                    + typ.getAnzeigename() + ChatColor.GREEN + " erhalten.");
        }
    }

    private void auflisten(CommandSender sender) {
        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GOLD + "Sonder-TNT-Typen:");
        for (TntTyp typ : TntTyp.values()) {
            boolean rezept = plugin.getConfig().getBoolean("rezepte." + typ.getId(), true);
            sender.sendMessage(ChatColor.YELLOW + "- " + typ.getId() + ChatColor.GRAY + " (" + typ.getAnzeigename()
                    + ChatColor.GRAY + "): " + typ.getBeschreibung().get(0)
                    + (rezept ? ChatColor.GREEN + " [Rezept an]" : ChatColor.RED + " [Rezept aus]"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        List<String> optionen;
        if (args.length == 1) {
            optionen = Arrays.asList("give", "list", "reload");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                optionen = new ArrayList<String>();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    optionen.add(p.getName());
                }
            } else if (args.length == 3) {
                optionen = TntTyp.alleIds();
            } else if (args.length == 4) {
                optionen = Arrays.asList("1", "16", "64");
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
        List<String> treffer = new ArrayList<String>();
        StringUtil.copyPartialMatches(args[args.length - 1], optionen, treffer);
        Collections.sort(treffer);
        return treffer;
    }
}
