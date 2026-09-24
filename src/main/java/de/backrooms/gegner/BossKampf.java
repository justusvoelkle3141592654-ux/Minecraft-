package de.backrooms.gegner;

import de.backrooms.BackroomsPlugin;
import de.backrooms.items.BackroomsItem;
import de.backrooms.welt.Level;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Der Endkampf gegen den Warden in der Boss-Arena.
 * Fähigkeiten: Dunkelheit (Blindheit für alle in der Nähe),
 * Schallschlag (Strahl, der Rüstung ignoriert) und ab halber
 * Lebensenergie ruft er einmalig Hounds zur Verstärkung.
 */
public class BossKampf {

    private final BackroomsPlugin plugin;
    private LivingEntity warden;
    private boolean spawnGeplant;
    private boolean verstaerkungGerufen;
    private int sekunden;
    private int leerSekunden;
    /** Nach einem Sieg wird eine Weile kein neuer Warden gespawnt (Zeit in ms). */
    private long pauseBis;

    public BossKampf(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean wardenLebt() {
        return warden != null && warden.isValid() && !warden.isDead();
    }

    public LivingEntity getWarden() {
        return wardenLebt() ? warden : null;
    }

    /** Alle Spieler, die sich gerade in der Boss-Arena befinden. */
    public List<Player> spielerInArena() {
        List<Player> liste = new ArrayList<Player>();
        World welt = plugin.getWeltManager().getWelt();
        if (welt == null) {
            return liste;
        }
        for (Player p : welt.getPlayers()) {
            if (plugin.getWeltManager().levelVon(p.getLocation()) == Level.BOSS && !p.isDead()) {
                liste.add(p);
            }
        }
        return liste;
    }

    /** Wird aufgerufen, wenn ein Spieler die Arena betritt. */
    public void spielerBetritt() {
        if (wardenLebt() || spawnGeplant || System.currentTimeMillis() < pauseBis) {
            return;
        }
        spawnGeplant = true;
        for (Player p : spielerInArena()) {
            p.sendTitle(ChatColor.DARK_AQUA + "Der Warden erwacht...", ChatColor.GRAY + "Mach dich bereit!");
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.6F, 0.5F);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                spawnGeplant = false;
                if (!wardenLebt() && !spielerInArena().isEmpty()) {
                    spawnen();
                }
            }
        }, 100L);
    }

    private void spawnen() {
        World welt = plugin.getWeltManager().getWelt();
        Location mitte = new Location(welt, 0.5, Level.BOSS.getBodenY() + 1, 0.5);
        warden = plugin.getGegnerManager().spawnen(GegnerTyp.WARDEN, mitte);
        verstaerkungGerufen = false;
        sekunden = 0;
        welt.strikeLightningEffect(mitte);
        for (Player p : spielerInArena()) {
            p.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Der Warden ist erwacht! "
                    + ChatColor.GRAY + "Besiege ihn, um den Backrooms zu entkommen.");
        }
        nameAktualisieren();
    }

    /** Wird jede Sekunde aufgerufen. */
    public void tick() {
        List<Player> spieler = spielerInArena();
        if (!wardenLebt()) {
            warden = null;
            if (!spieler.isEmpty()) {
                spielerBetritt();
            }
            return;
        }
        // Niemand mehr in der Arena -> Warden verschwindet nach einer Minute (Neustart des Kampfes)
        if (spieler.isEmpty()) {
            if (++leerSekunden >= 60) {
                warden.remove();
                warden = null;
                leerSekunden = 0;
            }
            return;
        }
        leerSekunden = 0;
        sekunden++;
        nameAktualisieren();

        int dunkelheit = Math.max(2, plugin.getConfig().getInt("boss.dunkelheit-sekunden", 8));
        int schall = Math.max(2, plugin.getConfig().getInt("boss.schallschlag-sekunden", 5));
        if (sekunden % dunkelheit == 0) {
            dunkelheit(spieler);
        }
        if (sekunden % schall == 0) {
            schallschlag(spieler);
        }
        if (!verstaerkungGerufen && warden.getHealth() <= warden.getMaxHealth() / 2) {
            verstaerkungGerufen = true;
            verstaerkung(spieler);
        }
    }

    private void nameAktualisieren() {
        if (!wardenLebt()) {
            return;
        }
        warden.setCustomName(GegnerTyp.WARDEN.getAnzeigename() + ChatColor.RED + " "
                + (int) Math.ceil(warden.getHealth()) + "/" + (int) warden.getMaxHealth() + " HP");
    }

    private void dunkelheit(List<Player> spieler) {
        Location ort = warden.getLocation();
        ort.getWorld().playSound(ort, Sound.WITHER_IDLE, 2.0F, 0.5F);
        for (Player p : spieler) {
            if (p.getLocation().distanceSquared(ort) <= 30 * 30) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0), true);
            }
        }
    }

    private void schallschlag(List<Player> spieler) {
        Player ziel = null;
        double beste = 18 * 18;
        for (Player p : spieler) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
                continue;
            }
            double d = p.getLocation().distanceSquared(warden.getLocation());
            if (d <= beste && warden.hasLineOfSight(p)) {
                beste = d;
                ziel = p;
            }
        }
        if (ziel == null) {
            return;
        }
        Location von = warden.getEyeLocation();
        Vector richtung = ziel.getEyeLocation().toVector().subtract(von.toVector());
        double laenge = richtung.length();
        if (laenge < 0.1) {
            return;
        }
        richtung.normalize();
        World welt = von.getWorld();
        for (double d = 0; d < laenge; d += 0.7) {
            Location punkt = von.clone().add(richtung.clone().multiply(d));
            welt.spigot().playEffect(punkt, Effect.FIREWORKS_SPARK, 0, 0, 0.05F, 0.05F, 0.05F, 0.0F, 2, 48);
        }
        welt.playSound(von, Sound.WITHER_SHOOT, 2.0F, 0.5F);
        double schaden = plugin.getConfig().getDouble("boss.schallschlag-schaden", 6.0);
        ziel.damage(schaden);
        ziel.setVelocity(richtung.clone().multiply(1.2).setY(0.45));
    }

    private void verstaerkung(List<Player> spieler) {
        for (Player p : spieler) {
            p.sendMessage(ChatColor.DARK_AQUA + "Der Warden ruft Verstärkung!");
        }
        Location ort = warden.getLocation();
        for (int i = 0; i < 3; i++) {
            double winkel = Math.PI * 2 * i / 3;
            Location platz = ort.clone().add(Math.cos(winkel) * 3, 0, Math.sin(winkel) * 3);
            plugin.getGegnerManager().spawnen(GegnerTyp.HOUND, platz);
        }
    }

    /** Wird aufgerufen, wenn der Warden stirbt. */
    public void besiegt() {
        warden = null;
        pauseBis = System.currentTimeMillis() + 15000L;
        // Verstärkung ebenfalls entfernen
        for (LivingEntity hound : plugin.getGegnerManager().alle(GegnerTyp.HOUND)) {
            if (plugin.getWeltManager().levelVon(hound.getLocation()) == Level.BOSS) {
                hound.remove();
            }
        }
        final List<Player> sieger = spielerInArena();
        for (Player p : sieger) {
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GESCHAFFT!",
                    ChatColor.GRAY + "Du bist den Backrooms entkommen");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
            Map<Integer, org.bukkit.inventory.ItemStack> rest =
                    p.getInventory().addItem(BackroomsItem.WARDEN_HERZ.erstellen(1));
            for (org.bukkit.inventory.ItemStack item : rest.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), item);
            }
            plugin.getSpielerDaten().siegErhoehen(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "Der Warden ist besiegt! In 5 Sekunden kehrst du zurück.");
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player p : sieger) {
                    if (p.isOnline() && plugin.getWeltManager().levelVon(p.getLocation()) == Level.BOSS) {
                        plugin.getSpielablauf().verlassen(p);
                    }
                }
            }
        }, 100L);
    }

    public void entfernen() {
        if (warden != null) {
            warden.remove();
            warden = null;
        }
    }
}
