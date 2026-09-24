package de.backrooms.gegner;

import de.backrooms.BackroomsPlugin;
import de.backrooms.items.BackroomsItem;
import de.backrooms.welt.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Endkampf gegen den Warden (eigenes Blockmodell). Er erscheint sofort, wenn
 * jemand die Arena betritt. Fähigkeiten (bewusst nicht zu schwer):
 * kurze Dunkelheit, Schallschlag und einmalig 2 Schattenhunde als Verstärkung.
 */
public class BossKampf {

    private final BackroomsPlugin plugin;
    private final BossBar leiste;
    private LivingEntity warden;
    private boolean verstaerkungGerufen;
    private int sekunden;
    private int leerSekunden;
    /** Nach einem Sieg kurz kein neuer Warden (Zeit in ms). */
    private long pauseBis;

    public BossKampf(BackroomsPlugin plugin) {
        this.plugin = plugin;
        this.leiste = Bukkit.createBossBar(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden",
                BarColor.BLUE, BarStyle.SEGMENTED_10);
        this.leiste.setVisible(false);
    }

    public boolean wardenLebt() {
        return warden != null && warden.isValid() && !warden.isDead();
    }

    public LivingEntity getWarden() {
        return wardenLebt() ? warden : null;
    }

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

    /** Spieler betritt die Arena: der Warden erscheint sofort. */
    public void spielerBetritt() {
        if (wardenLebt() || System.currentTimeMillis() < pauseBis) {
            return;
        }
        World welt = plugin.getWeltManager().getWelt();
        Location mitte = new Location(welt, 0.5, Level.BOSS.getBodenY() + 1, 0.5, 0F, 0F);
        warden = plugin.getGegnerManager().spawnen(GegnerTyp.WARDEN, mitte);
        verstaerkungGerufen = false;
        sekunden = 0;
        welt.strikeLightningEffect(mitte);
        welt.playSound(mitte, Sound.ENTITY_WITHER_SPAWN, 1.0F, 0.6F);
        for (Player p : spielerInArena()) {
            p.sendTitle(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "DER WARDEN",
                    ChatColor.GRAY + "Besiege ihn, um zu entkommen!", 10, 50, 10);
        }
        leisteAktualisieren(spielerInArena());
    }

    /** Wird jede Sekunde aufgerufen. */
    public void tick() {
        List<Player> spieler = spielerInArena();
        if (!wardenLebt()) {
            warden = null;
            leiste.setVisible(false);
            leiste.removeAll();
            if (!spieler.isEmpty()) {
                spielerBetritt();
            }
            return;
        }
        leisteAktualisieren(spieler);
        if (spieler.isEmpty()) {
            if (++leerSekunden >= 60) {
                plugin.getGegnerManager().entfernen(warden);
                warden = null;
                leerSekunden = 0;
            }
            return;
        }
        leerSekunden = 0;
        sekunden++;

        int dunkelheit = Math.max(3, plugin.getConfig().getInt("boss.dunkelheit-sekunden", 15));
        int schall = Math.max(3, plugin.getConfig().getInt("boss.schallschlag-sekunden", 8));
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

    private void leisteAktualisieren(List<Player> spieler) {
        if (!wardenLebt()) {
            return;
        }
        double anteil = Math.max(0.0, Math.min(1.0, warden.getHealth() / warden.getMaxHealth()));
        leiste.setProgress(anteil);
        leiste.setTitle(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden " + ChatColor.WHITE
                + (int) Math.ceil(warden.getHealth()) + " / " + (int) warden.getMaxHealth());
        for (Player p : new ArrayList<Player>(leiste.getPlayers())) {
            if (!spieler.contains(p)) {
                leiste.removePlayer(p);
            }
        }
        for (Player p : spieler) {
            if (!leiste.getPlayers().contains(p)) {
                leiste.addPlayer(p);
            }
        }
        leiste.setVisible(true);
    }

    private void dunkelheit(List<Player> spieler) {
        Location ort = warden.getLocation();
        ort.getWorld().playSound(ort, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 0.6F);
        for (Player p : spieler) {
            if (p.getLocation().distanceSquared(ort) <= 30 * 30) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0), true);
            }
        }
    }

    private void schallschlag(List<Player> spieler) {
        Player ziel = null;
        double beste = 16 * 16;
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
        Location von = warden.getLocation().add(0, 2.0, 0);
        Vector richtung = ziel.getEyeLocation().toVector().subtract(von.toVector());
        double laenge = richtung.length();
        if (laenge < 0.1) {
            return;
        }
        richtung.normalize();
        World welt = von.getWorld();
        for (double d = 0; d < laenge; d += 0.6) {
            welt.spawnParticle(Particle.SPELL_WITCH, von.clone().add(richtung.clone().multiply(d)), 2,
                    0.05, 0.05, 0.05, 0);
        }
        welt.playSound(von, Sound.ENTITY_WITHER_SHOOT, 1.5F, 0.5F);
        ziel.damage(plugin.getConfig().getDouble("boss.schallschlag-schaden", 4.0));
        ziel.setVelocity(richtung.clone().multiply(0.9).setY(0.35));
    }

    private void verstaerkung(List<Player> spieler) {
        for (Player p : spieler) {
            p.sendMessage(ChatColor.DARK_AQUA + "Der Warden ruft zwei Schattenhunde!");
        }
        Location ort = warden.getLocation();
        for (int i = 0; i < 2; i++) {
            double winkel = Math.PI * i;
            Location platz = ort.clone().add(Math.cos(winkel) * 3, 0, Math.sin(winkel) * 3);
            plugin.getGegnerManager().spawnen(GegnerTyp.SCHATTENHUND, platz);
        }
    }

    /** Wird aufgerufen, wenn der Warden stirbt. */
    public void besiegt() {
        warden = null;
        pauseBis = System.currentTimeMillis() + 15000L;
        leiste.setVisible(false);
        leiste.removeAll();
        for (LivingEntity hund : plugin.getGegnerManager().alle(GegnerTyp.SCHATTENHUND)) {
            if (plugin.getWeltManager().levelVon(hund.getLocation()) == Level.BOSS) {
                plugin.getGegnerManager().entfernen(hund);
            }
        }
        final List<Player> sieger = spielerInArena();
        for (Player p : sieger) {
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GESCHAFFT!",
                    ChatColor.GRAY + "Du bist den Backrooms entkommen", 10, 60, 10);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            for (ItemStack rest : p.getInventory().addItem(BackroomsItem.WARDEN_HERZ.erstellen(1)).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
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
            plugin.getGegnerManager().entfernen(warden);
            warden = null;
        }
        leiste.removeAll();
        leiste.setVisible(false);
    }
}
