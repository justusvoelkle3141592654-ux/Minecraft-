package de.backrooms.befehle;

import de.backrooms.BackroomsPlugin;
import de.backrooms.gegner.GegnerTyp;
import de.backrooms.items.BackroomsItem;
import de.backrooms.tnt.SonderTntItems;
import de.backrooms.tnt.TntTyp;
import de.backrooms.welt.Level;
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

/**
 * /backrooms level &lt;0|1|2|boss&gt; [Spieler]
 * /backrooms verlassen [Spieler]
 * /backrooms item &lt;Spieler&gt; &lt;item&gt; [anzahl]
 * /backrooms gegner &lt;typ&gt;
 * /backrooms info
 * /backrooms reload
 */
public class BackroomsBefehl implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "backrooms.admin";
    private static final String TNT_ENDUNG = "_tnt";

    private final BackroomsPlugin plugin;

    public BackroomsBefehl(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dazu fehlt dir die Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            hilfe(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("level")) {
            level(sender, label, args);
        } else if (sub.equals("verlassen")) {
            verlassen(sender, args);
        } else if (sub.equals("item")) {
            item(sender, label, args);
        } else if (sub.equals("gegner")) {
            gegner(sender, label, args);
        } else if (sub.equals("info")) {
            info(sender);
        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Backrooms-Konfiguration neu geladen.");
        } else {
            hilfe(sender, label);
        }
        return true;
    }

    private void hilfe(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Backrooms-Befehle:");
        sender.sendMessage(ChatColor.YELLOW + "/start" + ChatColor.GRAY + " - Ausrüstung + ab in Level 0");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " level <0|1|2|boss> [Spieler]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " verlassen [Spieler]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " item <Spieler> <item> [anzahl]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " gegner <typ>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }

    /** Spieler aus args[index] oder der Absender selbst. */
    private Player ziel(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player p = plugin.getServer().getPlayerExact(args[index]);
            if (p == null) {
                sender.sendMessage(ChatColor.RED + "Spieler '" + args[index] + "' ist nicht online.");
            }
            return p;
        }
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(ChatColor.RED + "Bitte einen Spieler angeben.");
        return null;
    }

    private void level(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /" + label + " level <0|1|2|boss> [Spieler]");
            return;
        }
        Level level = Level.vonText(args[1]);
        if (level == null) {
            sender.sendMessage(ChatColor.RED + "Unbekanntes Level. Möglich: 0, 1, 2, boss");
            return;
        }
        Player p = ziel(sender, args, 2);
        if (p == null) {
            return;
        }
        if (!plugin.getWeltManager().istBackrooms(p.getWorld())) {
            plugin.getSpielerDaten().setRueckkehr(p.getUniqueId(), p.getLocation());
        }
        plugin.getSpielablauf().zuLevel(p, level);
        sender.sendMessage(ChatColor.GREEN + p.getName() + " ist jetzt in " + level.getName() + ".");
    }

    private void verlassen(CommandSender sender, String[] args) {
        Player p = ziel(sender, args, 1);
        if (p == null) {
            return;
        }
        if (!plugin.getWeltManager().istBackrooms(p.getWorld())) {
            sender.sendMessage(ChatColor.RED + p.getName() + " ist nicht in den Backrooms.");
            return;
        }
        plugin.getSpielablauf().verlassen(p);
        sender.sendMessage(ChatColor.GREEN + p.getName() + " hat die Backrooms verlassen.");
    }

    private void item(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /" + label + " item <Spieler> <item> [anzahl]");
            sender.sendMessage(ChatColor.GRAY + "Items: " + String.join(", ", itemIds()));
            return;
        }
        Player p = ziel(sender, args, 1);
        if (p == null) {
            return;
        }
        ItemStack vorlage = itemVonId(args[2]);
        if (vorlage == null) {
            sender.sendMessage(ChatColor.RED + "Unbekanntes Item. Möglich: " + String.join(", ", itemIds()));
            return;
        }
        int anzahl = 1;
        if (args.length >= 4) {
            try {
                anzahl = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "'" + args[3] + "' ist keine Zahl.");
                return;
            }
        }
        if (anzahl < 1 || anzahl > 64 * 36) {
            sender.sendMessage(ChatColor.RED + "Die Anzahl muss zwischen 1 und " + (64 * 36) + " liegen.");
            return;
        }
        int rest = anzahl;
        while (rest > 0) {
            ItemStack stapel = vorlage.clone();
            stapel.setAmount(Math.min(vorlage.getMaxStackSize(), rest));
            rest -= stapel.getAmount();
            for (ItemStack uebrig : p.getInventory().addItem(stapel).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), uebrig);
            }
        }
        sender.sendMessage(ChatColor.GREEN + p.getName() + " hat " + anzahl + "x "
                + vorlage.getItemMeta().getDisplayName() + ChatColor.GREEN + " erhalten.");
    }

    private static ItemStack itemVonId(String id) {
        BackroomsItem item = BackroomsItem.vonId(id);
        if (item != null) {
            return item.erstellen(1);
        }
        if (id.toLowerCase().endsWith(TNT_ENDUNG)) {
            TntTyp typ = TntTyp.vonId(id.substring(0, id.length() - TNT_ENDUNG.length()));
            if (typ != null) {
                return SonderTntItems.erstellen(typ, 1);
            }
        }
        return null;
    }

    private static List<String> itemIds() {
        List<String> ids = new ArrayList<String>();
        for (BackroomsItem item : BackroomsItem.values()) {
            ids.add(item.getId());
        }
        for (TntTyp typ : TntTyp.values()) {
            ids.add(typ.getId() + TNT_ENDUNG);
        }
        return ids;
    }

    private void gegner(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können Gegner spawnen.");
            return;
        }
        Player p = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /" + label + " gegner <typ>");
            for (GegnerTyp typ : GegnerTyp.values()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + typ.getId() + ChatColor.GRAY + ": " + typ.getBeschreibung());
            }
            return;
        }
        GegnerTyp typ = GegnerTyp.vonId(args[1]);
        if (typ == null) {
            sender.sendMessage(ChatColor.RED + "Unbekannter Gegner. Möglich: " + String.join(", ", GegnerTyp.alleIds()));
            return;
        }
        if (!plugin.getWeltManager().istBackrooms(p.getWorld())) {
            sender.sendMessage(ChatColor.RED + "Gegner können nur in den Backrooms gespawnt werden.");
            return;
        }
        plugin.getGegnerManager().spawnen(typ, p.getLocation());
        sender.sendMessage(ChatColor.GREEN + typ.getAnzeigename() + ChatColor.GREEN + " gespawnt.");
    }

    private void info(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Spieler in den Backrooms:");
        boolean jemand = false;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getWeltManager().istBackrooms(p.getWorld())) {
                continue;
            }
            jemand = true;
            Level level = plugin.getWeltManager().levelVon(p.getLocation());
            sender.sendMessage(ChatColor.YELLOW + "- " + p.getName() + ChatColor.GRAY + ": "
                    + (level == null ? "zwischen den Ebenen" : level.getName())
                    + ", besiegt: " + plugin.getSpielerDaten().getBesiegt(p.getUniqueId())
                    + ", Siege: " + plugin.getSpielerDaten().getSiege(p.getUniqueId()));
        }
        if (!jemand) {
            sender.sendMessage(ChatColor.GRAY + "(niemand)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        List<String> optionen = Collections.emptyList();
        String sub = args[0].toLowerCase();
        if (args.length == 1) {
            optionen = Arrays.asList("level", "verlassen", "item", "gegner", "info", "reload");
        } else if (sub.equals("level")) {
            if (args.length == 2) {
                optionen = Arrays.asList("0", "1", "2", "boss");
            } else if (args.length == 3) {
                optionen = spielerNamen();
            }
        } else if (sub.equals("verlassen") && args.length == 2) {
            optionen = spielerNamen();
        } else if (sub.equals("item")) {
            if (args.length == 2) {
                optionen = spielerNamen();
            } else if (args.length == 3) {
                optionen = itemIds();
            } else if (args.length == 4) {
                optionen = Arrays.asList("1", "16", "64");
            }
        } else if (sub.equals("gegner") && args.length == 2) {
            optionen = GegnerTyp.alleIds();
        }
        List<String> treffer = new ArrayList<String>();
        StringUtil.copyPartialMatches(args[args.length - 1], optionen, treffer);
        Collections.sort(treffer);
        return treffer;
    }

    private List<String> spielerNamen() {
        List<String> namen = new ArrayList<String>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            namen.add(p.getName());
        }
        return namen;
    }
}
