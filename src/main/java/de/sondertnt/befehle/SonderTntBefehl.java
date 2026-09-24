package de.sondertnt.befehle;

import de.sondertnt.SonderTNTPlugin;
import de.sondertnt.tnt.SonderTntItems;
import de.sondertnt.tnt.TntTyp;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

/** /sondertnt give &lt;Spieler&gt; &lt;typ&gt; [anzahl] | alle &lt;Spieler&gt; [anzahl] | list | reload */
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
        } else if (sub.equals("alle")) {
            alleGeben(sender, label, args);
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
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " alle <Spieler> [anzahl]"
                + ChatColor.GRAY + " (alle Typen + Feuerzeug)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }

    private void geben(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED
                    + "Benutzung: /" + label + " give <Spieler> <typ> [anzahl]");
            return;
        }
        Player ziel = spielerFinden(sender, args[1]);
        if (ziel == null) {
            return;
        }
        TntTyp typ = TntTyp.vonId(args[2]);
        if (typ == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Unbekannter Typ '" + args[2]
                    + "'. Verfügbar: " + String.join(", ", TntTyp.alleIds()));
            return;
        }
        int anzahl = anzahlLesen(sender, args, 3, 1);
        if (anzahl < 1) {
            return;
        }

        verteilen(ziel, SonderTntItems.erstellen(typ, 1), anzahl);

        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + ziel.getName() + " hat " + anzahl + "x "
                + typ.getAnzeigename() + ChatColor.GREEN + " erhalten.");
        if (sender != ziel) {
            ziel.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + "Du hast " + anzahl + "x "
                    + typ.getAnzeigename() + ChatColor.GREEN + " erhalten.");
        }
    }

    /** Gibt alle Sonder-TNT-Typen (je anzahl, Standard 64) und ein Feuerzeug. */
    private void alleGeben(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED
                    + "Benutzung: /" + label + " alle <Spieler> [anzahl]");
            return;
        }
        Player ziel = spielerFinden(sender, args[1]);
        if (ziel == null) {
            return;
        }
        int anzahl = anzahlLesen(sender, args, 2, 64);
        if (anzahl < 1) {
            return;
        }

        for (TntTyp typ : TntTyp.values()) {
            verteilen(ziel, SonderTntItems.erstellen(typ, 1), anzahl);
        }
        verteilen(ziel, new ItemStack(Material.FLINT_AND_STEEL), 1);

        sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + ziel.getName() + " hat alle Sonder-TNT ("
                + anzahl + "x je Typ) und ein Feuerzeug erhalten.");
        if (sender != ziel) {
            ziel.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.GREEN + "Du hast alle Sonder-TNT ("
                    + anzahl + "x je Typ) und ein Feuerzeug erhalten.");
        }
    }

    private Player spielerFinden(CommandSender sender, String name) {
        Player ziel = plugin.getServer().getPlayerExact(name);
        if (ziel == null) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "Spieler '" + name + "' ist nicht online.");
        }
        return ziel;
    }

    /** @return die Anzahl aus args[index], den Standardwert oder -1 bei ungültiger Eingabe. */
    private int anzahlLesen(CommandSender sender, String[] args, int index, int standard) {
        if (args.length <= index) {
            return standard;
        }
        int anzahl;
        try {
            anzahl = Integer.parseInt(args[index]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED + "'" + args[index] + "' ist keine Zahl.");
            return -1;
        }
        if (anzahl < 1 || anzahl > MAX_ANZAHL) {
            sender.sendMessage(SonderTNTPlugin.PREFIX + ChatColor.RED
                    + "Die Anzahl muss zwischen 1 und " + MAX_ANZAHL + " liegen.");
            return -1;
        }
        return anzahl;
    }

    /**
     * Gibt das Item in Stapeln (max. Stapelgröße des Items). Was nicht ins
     * Inventar passt, wird vor dem Spieler fallen gelassen.
     */
    private void verteilen(Player ziel, ItemStack vorlage, int anzahl) {
        int rest = anzahl;
        while (rest > 0) {
            ItemStack stapel = vorlage.clone();
            stapel.setAmount(Math.min(vorlage.getMaxStackSize(), rest));
            rest -= stapel.getAmount();
            Map<Integer, ItemStack> uebrig = ziel.getInventory().addItem(stapel);
            for (ItemStack item : uebrig.values()) {
                ziel.getWorld().dropItemNaturally(ziel.getLocation(), item);
            }
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
            optionen = Arrays.asList("give", "alle", "list", "reload");
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
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("alle")) {
            if (args.length == 2) {
                optionen = new ArrayList<String>();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    optionen.add(p.getName());
                }
            } else if (args.length == 3) {
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
