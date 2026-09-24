package de.backrooms.welt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Lässt Deckenlichter in der Nähe der Spieler kurz ausgehen (Level 0 und 1).
 * Die Lampen werden dafür kurz durch den Deckenblock ersetzt und danach
 * wiederhergestellt.
 */
public class Flackerlicht {

    private final Plugin plugin;
    private final WeltManager weltManager;
    private final Random random = new Random();

    /** Ausgeschaltete Lampen: Ort -> [Material-ID, Daten] des Originals. */
    private final Map<Location, int[]> aus = new HashMap<Location, int[]>();

    public Flackerlicht(Plugin plugin, WeltManager weltManager) {
        this.plugin = plugin;
        this.weltManager = weltManager;
    }

    /** Wird alle 10 Ticks aufgerufen. */
    public void tick() {
        if (!plugin.getConfig().getBoolean("licht.flackern", true)) {
            return;
        }
        double chance = plugin.getConfig().getDouble("licht.flacker-chance", 0.25);
        World welt = weltManager.getWelt();
        if (welt == null) {
            return;
        }
        for (Player spieler : welt.getPlayers()) {
            Level level = weltManager.levelVon(spieler.getLocation());
            if (level != Level.LEVEL_0 && level != Level.LEVEL_1) {
                continue;
            }
            if (random.nextDouble() >= chance) {
                continue;
            }
            int zelle = level.getZelle();
            int cx = Labyrinth.zelleVon(spieler.getLocation().getBlockX(), level) + random.nextInt(5) - 2;
            int cz = Labyrinth.zelleVon(spieler.getLocation().getBlockZ(), level) + random.nextInt(5) - 2;
            int mitte = zelle / 2;
            int y = level.getDeckeY();
            int dauer = 3 + random.nextInt(18);
            ausschalten(welt.getBlockAt(cx * zelle + mitte, y, cz * zelle + mitte), dauer);
            if (level == Level.LEVEL_0) {
                ausschalten(welt.getBlockAt(cx * zelle + mitte - 1, y, cz * zelle + mitte), dauer);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void ausschalten(final Block lampe, int dauerTicks) {
        Material typ = lampe.getType();
        if (typ != Material.SEA_LANTERN && typ != Material.GLOWSTONE) {
            return;
        }
        final Location ort = lampe.getLocation();
        if (aus.containsKey(ort)) {
            return;
        }
        // Ausschalten: gleicher Block wie die umgebende Decke
        Block nachbar = lampe.getRelative(1, 0, 1);
        aus.put(ort, new int[]{typ.getId(), lampe.getData()});
        lampe.setTypeIdAndData(nachbar.getTypeId(), nachbar.getData(), false);
        lampe.getWorld().playSound(ort, Sound.UI_BUTTON_CLICK, 0.2F, 2.0F);

        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                wiederherstellen(ort);
            }
        }, dauerTicks);
    }

    @SuppressWarnings("deprecation")
    private void wiederherstellen(Location ort) {
        int[] original = aus.remove(ort);
        if (original != null) {
            ort.getBlock().setTypeIdAndData(original[0], (byte) original[1], false);
        }
    }

    /** Schaltet beim Deaktivieren des Plugins alle Lampen wieder ein. */
    public void allesWiederherstellen() {
        for (Location ort : new java.util.ArrayList<Location>(aus.keySet())) {
            wiederherstellen(ort);
        }
    }
}
