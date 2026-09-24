package de.backrooms.spieler;

import de.backrooms.BackroomsPlugin;
import de.backrooms.gegner.BossKampf;
import de.backrooms.items.BackroomsItem;
import de.backrooms.tnt.SonderTntItems;
import de.backrooms.tnt.TntTyp;
import de.backrooms.welt.Labyrinth;
import de.backrooms.welt.Level;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Betreten, Level-Wechsel, Verlassen und die Anzeige pro Sekunde. */
public class Spielablauf {

    /** Suchradius des Kompasses in Zellen. */
    private static final int KOMPASS_RADIUS = 45;

    private final BackroomsPlugin plugin;
    private final Map<UUID, Long> ausgangSperre = new HashMap<UUID, Long>();

    public Spielablauf(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ Betreten (/start)

    public void betreten(Player spieler) {
        World welt = plugin.getWeltManager().getWelt();
        if (welt == null) {
            spieler.sendMessage(ChatColor.RED + "Die Backrooms-Welt ist nicht geladen.");
            return;
        }
        if (plugin.getWeltManager().istBackrooms(spieler.getWorld())) {
            spieler.sendMessage(ChatColor.RED + "Du bist schon in den Backrooms. Finde den Ausgang!");
            return;
        }
        plugin.getSpielerDaten().setRueckkehr(spieler.getUniqueId(), spieler.getLocation());
        startAusruestung(spieler);
        zuLevel(spieler, Level.LEVEL_0);
        spieler.sendMessage(ChatColor.GOLD + "Du bist durch den Boden der Realität gefallen...");
        spieler.sendMessage(ChatColor.GRAY + "Finde in jedem Level den " + ChatColor.GREEN + "Ausgang (Smaragd-Feld)"
                + ChatColor.GRAY + ". Dein " + BackroomsItem.KOMPASS.getAnzeigename() + ChatColor.GRAY
                + " zeigt den Weg. Am Ende wartet der " + ChatColor.DARK_AQUA + "Warden" + ChatColor.GRAY + ".");
    }

    /** Diamantausrüstung, Backrooms-Items und Sonder-TNT. */
    public void startAusruestung(Player spieler) {
        int schaerfe = plugin.getConfig().getInt("start.schwert-schaerfe", 3);
        int schutz = plugin.getConfig().getInt("start.ruestung-schutz", 2);
        int haltbarkeit = plugin.getConfig().getInt("start.haltbarkeit", 3);

        List<ItemStack> items = new ArrayList<ItemStack>();
        ItemStack schwert = new ItemStack(Material.DIAMOND_SWORD);
        verzaubern(schwert, Enchantment.DAMAGE_ALL, schaerfe);
        verzaubern(schwert, Enchantment.DURABILITY, haltbarkeit);
        items.add(schwert);

        PlayerInventory inv = spieler.getInventory();
        ItemStack helm = ruestung(Material.DIAMOND_HELMET, schutz, haltbarkeit);
        ItemStack brust = ruestung(Material.DIAMOND_CHESTPLATE, schutz, haltbarkeit);
        ItemStack hose = ruestung(Material.DIAMOND_LEGGINGS, schutz, haltbarkeit);
        ItemStack schuhe = ruestung(Material.DIAMOND_BOOTS, schutz, haltbarkeit);
        // Direkt anziehen, wenn der Platz frei ist, sonst ins Inventar
        if (leer(inv.getHelmet())) {
            inv.setHelmet(helm);
        } else {
            items.add(helm);
        }
        if (leer(inv.getChestplate())) {
            inv.setChestplate(brust);
        } else {
            items.add(brust);
        }
        if (leer(inv.getLeggings())) {
            inv.setLeggings(hose);
        } else {
            items.add(hose);
        }
        if (leer(inv.getBoots())) {
            inv.setBoots(schuhe);
        } else {
            items.add(schuhe);
        }

        items.add(BackroomsItem.KOMPASS.erstellen(1));
        items.add(BackroomsItem.TASCHENLAMPE.erstellen(1));
        for (int i = 0; i < plugin.getConfig().getInt("start.mandelwasser", 3); i++) {
            items.add(BackroomsItem.MANDELWASSER.erstellen(1)); // Flaschen stapeln nicht
        }
        int riegel = plugin.getConfig().getInt("start.energieriegel", 8);
        if (riegel > 0) {
            items.add(BackroomsItem.ENERGIERIEGEL.erstellen(riegel));
        }
        int tnt = Math.min(64, plugin.getConfig().getInt("start.sonder-tnt-je-typ", 8));
        if (tnt > 0) {
            for (TntTyp typ : TntTyp.values()) {
                items.add(SonderTntItems.erstellen(typ, tnt));
            }
        }

        for (ItemStack item : items) {
            for (ItemStack rest : inv.addItem(item).values()) {
                spieler.getWorld().dropItemNaturally(spieler.getLocation(), rest);
            }
        }
    }

    private static boolean leer(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private static void verzaubern(ItemStack item, Enchantment verzauberung, int stufe) {
        if (stufe > 0) {
            item.addUnsafeEnchantment(verzauberung, stufe);
        }
    }

    private static ItemStack ruestung(Material material, int schutz, int haltbarkeit) {
        ItemStack item = new ItemStack(material);
        verzaubern(item, Enchantment.PROTECTION_ENVIRONMENTAL, schutz);
        verzaubern(item, Enchantment.DURABILITY, haltbarkeit);
        return item;
    }

    // ------------------------------------------------------------ Level-Wechsel

    public void zuLevel(Player spieler, Level level) {
        World welt = plugin.getWeltManager().getWelt();
        spieler.teleport(level.start(welt));
        spieler.setFallDistance(0F);
        spieler.sendTitle(level.getFarbe() + "" + ChatColor.BOLD + level.getName(),
                ChatColor.GRAY + level.getUntertitel());
        spieler.playSound(spieler.getLocation(), Sound.PORTAL_TRAVEL, 0.4F, 1.4F);
        if (level.istBoss()) {
            plugin.getBossKampf().spielerBetritt();
        }
    }

    /** Spieler steht auf einem Ausgangsfeld. */
    public void ausgangErreicht(Player spieler, Level level) {
        long jetzt = System.currentTimeMillis();
        Long gesperrt = ausgangSperre.get(spieler.getUniqueId());
        if (gesperrt != null && gesperrt > jetzt) {
            return;
        }
        Level naechstes = level.naechstes();
        if (naechstes == null) {
            return;
        }
        ausgangSperre.put(spieler.getUniqueId(), jetzt + 3000L);
        spieler.sendMessage(ChatColor.GREEN + "Ausgang gefunden! " + ChatColor.GRAY + "Weiter nach "
                + naechstes.getFarbe() + naechstes.getName() + ChatColor.GRAY + " ...");
        zuLevel(spieler, naechstes);
    }

    /** Zurück an den Ort, an dem /start benutzt wurde (sonst Spawn der Hauptwelt). */
    public void verlassen(Player spieler) {
        Location ziel = plugin.getSpielerDaten().getRueckkehr(spieler.getUniqueId());
        if (ziel == null || plugin.getWeltManager().istBackrooms(ziel.getWorld())) {
            ziel = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        plugin.getSpielerDaten().entferneRueckkehr(spieler.getUniqueId());
        spieler.teleport(ziel);
        spieler.setFallDistance(0F);
        plugin.getAnzeige().entfernen(spieler);
        spieler.setCompassTarget(ziel.getWorld().getSpawnLocation());
    }

    // ------------------------------------------------------------ Jede Sekunde

    public void tick() {
        for (Player spieler : plugin.getServer().getOnlinePlayers()) {
            Level level = plugin.getWeltManager().levelVon(spieler.getLocation());
            boolean drin = plugin.getWeltManager().istBackrooms(spieler.getWorld());
            if (!drin) {
                if (plugin.getAnzeige().hatAnzeige(spieler)) {
                    plugin.getAnzeige().entfernen(spieler);
                    spieler.setCompassTarget(spieler.getWorld().getSpawnLocation());
                }
                continue;
            }
            taschenlampe(spieler);
            anzeigen(spieler, level);
        }
    }

    private void taschenlampe(Player spieler) {
        if (BackroomsItem.von(spieler.getItemInHand()) == BackroomsItem.TASCHENLAMPE) {
            spieler.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 15 * 20, 0, true), true);
        }
    }

    private void anzeigen(Player spieler, Level level) {
        List<String> zeilen = new ArrayList<String>();
        if (level == null) {
            zeilen.add(ChatColor.GRAY + "Zwischen den");
            zeilen.add(ChatColor.GRAY + "Ebenen ...");
        } else {
            zeilen.add(level.getFarbe() + "" + ChatColor.BOLD + level.getName());
            zeilen.add(ChatColor.GRAY + level.getUntertitel());
            zeilen.add(" ");
            if (level.istBoss()) {
                BossKampf boss = plugin.getBossKampf();
                LivingEntity warden = boss.getWarden();
                zeilen.add(warden == null ? ChatColor.GRAY + "Warden schläft"
                        : ChatColor.RED + "Warden " + (int) Math.ceil(warden.getHealth()) + "HP");
                if (warden != null) {
                    spieler.setCompassTarget(warden.getLocation());
                }
            } else {
                Location ausgang = Labyrinth.naechsterAusgang(spieler.getWorld(), level,
                        spieler.getLocation().getX(), spieler.getLocation().getZ(), KOMPASS_RADIUS);
                if (ausgang != null) {
                    spieler.setCompassTarget(ausgang);
                    int meter = (int) Math.round(Math.sqrt(horizontal(spieler.getLocation(), ausgang)));
                    zeilen.add(ChatColor.GREEN + "Ausgang: " + meter + "m");
                    if (meter <= 24) {
                        ausgang.getWorld().spigot().playEffect(ausgang.clone().add(0, 0.3, 0),
                                Effect.HAPPY_VILLAGER, 0, 0, 1.0F, 0.6F, 1.0F, 0.0F, 12, 32);
                    }
                } else {
                    zeilen.add(ChatColor.GREEN + "Ausgang: ???");
                }
            }
        }
        zeilen.add(ChatColor.RED + "Besiegt: " + plugin.getSpielerDaten().getBesiegt(spieler.getUniqueId()));
        plugin.getAnzeige().setzen(spieler, zeilen);
    }

    private static double horizontal(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** Text für den Kompass-Rechtsklick. */
    public String kompassInfo(Player spieler) {
        Level level = plugin.getWeltManager().levelVon(spieler.getLocation());
        if (level == null) {
            return ChatColor.GRAY + "Der Kompass dreht sich wild ... (nur in den Backrooms nutzbar)";
        }
        if (level.istBoss()) {
            return ChatColor.DARK_AQUA + "Hier gibt es keinen Ausgang. Besiege den Warden!";
        }
        Location ausgang = Labyrinth.naechsterAusgang(spieler.getWorld(), level,
                spieler.getLocation().getX(), spieler.getLocation().getZ(), KOMPASS_RADIUS);
        if (ausgang == null) {
            return ChatColor.GRAY + "Kein Ausgang in der Nähe gefunden. Geh weiter!";
        }
        int meter = (int) Math.round(Math.sqrt(horizontal(spieler.getLocation(), ausgang)));
        return ChatColor.GREEN + "Nächster Ausgang: " + meter + " Blöcke Richtung "
                + richtung(spieler.getLocation(), ausgang) + ChatColor.GRAY + " (Smaragd-Feld)";
    }

    private static String richtung(Location von, Location nach) {
        double dx = nach.getX() - von.getX();
        double dz = nach.getZ() - von.getZ();
        // Minecraft: -z = Norden, +x = Osten
        double winkel = Math.toDegrees(Math.atan2(dx, -dz));
        if (winkel < 0) {
            winkel += 360;
        }
        String[] namen = {"Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten", "Westen", "Nordwesten"};
        return namen[(int) Math.round(winkel / 45.0) % 8];
    }
}
