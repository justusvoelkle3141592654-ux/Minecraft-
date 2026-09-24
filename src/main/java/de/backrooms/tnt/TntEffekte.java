package de.backrooms.tnt;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Die Zusatzeffekte der einzelnen Sonder-TNT-Typen.
 * Es werden ausschließlich Vanilla-Blöcke, -Partikel und -Sounds verwendet.
 */
public class TntEffekte {

    private final Plugin plugin;
    private final Random random = new Random();

    /** Entities, die vom Lift-TNT hochgeschleudert wurden: UUID -> Ablaufzeit (ms). */
    private final Map<UUID, Long> fallschutz = new HashMap<UUID, Long>();

    public TntEffekte(Plugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    /** Explosionsstärke des Typs laut config.yml. */
    public float staerke(TntTyp typ) {
        double standard;
        switch (typ) {
            case MEGA:
                standard = 15.0;
                break;
            default:
                standard = 4.0;
                break;
        }
        return (float) Math.max(0.0, cfg().getDouble("tnt." + typ.getId() + ".staerke", standard));
    }

    // ------------------------------------------------------------------ Feuer

    /** Setzt nach der Explosion zusätzliches Feuer und zündet Entities an. */
    public void feuer(final Location mitte) {
        final int radius = Math.max(1, Math.min(12, cfg().getInt("tnt.feuer.radius", 5)));
        final double dichte = cfg().getDouble("tnt.feuer.dichte", 0.35);
        final int brenndauer = Math.max(0, cfg().getInt("tnt.feuer.brenndauer-ticks", 160));

        // Einen Tick warten, damit die Explosion das Feuer nicht direkt wieder wegsprengt
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                World welt = mitte.getWorld();
                int mx = mitte.getBlockX();
                int my = mitte.getBlockY();
                int mz = mitte.getBlockZ();
                int r2 = radius * radius;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx * dx + dy * dy + dz * dz > r2) {
                                continue;
                            }
                            int y = my + dy;
                            if (y < 1 || y >= welt.getMaxHeight()) {
                                continue;
                            }
                            Block block = welt.getBlockAt(mx + dx, y, mz + dz);
                            if (block.getType() == Material.AIR
                                    && block.getRelative(BlockFace.DOWN).getType().isSolid()
                                    && random.nextDouble() < dichte) {
                                block.setType(Material.FIRE);
                            }
                        }
                    }
                }

                for (Entity entity : welt.getNearbyEntities(mitte, radius, radius, radius)) {
                    if (entity.getLocation().distanceSquared(mitte) <= r2) {
                        entity.setFireTicks(Math.max(entity.getFireTicks(), brenndauer));
                    }
                }
                partikel(mitte, Effect.FLAME, 0.15F, 60, radius);
                partikel(mitte, Effect.LAVA_POP, 0.0F, 30, radius);
            }
        });
    }

    // ------------------------------------------------------------------ Blitz

    /** Lässt nacheinander mehrere Blitze rund um die Explosion einschlagen. */
    public void blitz(final Location mitte) {
        int anzahl = Math.max(0, Math.min(50, cfg().getInt("tnt.blitz.anzahl", 5)));
        final double radius = Math.max(0.0, cfg().getDouble("tnt.blitz.radius", 6.0));
        final boolean nurEffekt = cfg().getBoolean("tnt.blitz.nur-effekt", false);
        int verzoegerung = Math.max(0, cfg().getInt("tnt.blitz.verzoegerung-ticks", 4));

        for (int i = 0; i < anzahl; i++) {
            // Der erste Blitz schlägt in der Mitte ein, die weiteren zufällig im Radius
            final boolean inDerMitte = i == 0;
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    World welt = mitte.getWorld();
                    double x = mitte.getX();
                    double z = mitte.getZ();
                    if (!inDerMitte) {
                        double winkel = random.nextDouble() * Math.PI * 2.0;
                        double abstand = random.nextDouble() * radius;
                        x += Math.cos(winkel) * abstand;
                        z += Math.sin(winkel) * abstand;
                    }
                    int y = bodenHoehe(welt, (int) Math.floor(x), mitte.getBlockY(), (int) Math.floor(z));
                    Location ziel = new Location(welt, x, y, z);
                    if (nurEffekt) {
                        welt.strikeLightningEffect(ziel);
                    } else {
                        welt.strikeLightning(ziel);
                    }
                }
            }, 1L + (long) i * verzoegerung);
        }
    }

    /**
     * Sucht ausgehend von der Explosionshöhe den Boden an (x, z). Anders als
     * getHighestBlockYAt funktioniert das auch in Innenräumen (Backrooms, Höhlen),
     * wo sonst der Blitz oben auf dem Dach einschlagen würde.
     */
    private static int bodenHoehe(World welt, int x, int startY, int z) {
        int y = Math.max(1, Math.min(welt.getMaxHeight() - 2, startY));
        if (welt.getBlockAt(x, y, z).getType().isSolid()) {
            // in einem Hügel/einer Wand: etwas nach oben suchen
            for (int i = 0; i < 3; i++) {
                if (!welt.getBlockAt(x, y + 1, z).getType().isSolid()) {
                    return y + 1;
                }
                y++;
            }
            return startY;
        }
        // in der Luft: nach unten bis zum Boden
        for (int i = 0; i < 16 && y > 1; i++) {
            if (welt.getBlockAt(x, y - 1, z).getType().isSolid()) {
                break;
            }
            y--;
        }
        return y;
    }

    // ------------------------------------------------------------------ Lift

    /**
     * Schleudert alle Entities im Radius nach oben. Es entsteht keine echte
     * Explosion, daher werden keine Blöcke zerstört und kein Schaden verursacht.
     */
    public void lift(Location mitte) {
        double radius = Math.max(0.5, cfg().getDouble("tnt.lift.radius", 7.0));
        // Werte über ~3.9 werden vom 1.8-Protokoll ohnehin abgeschnitten
        double hoehe = Math.max(0.0, Math.min(3.9, cfg().getDouble("tnt.lift.hoehe", 2.0)));
        double seitlich = Math.max(0.0, Math.min(3.9, cfg().getDouble("tnt.lift.seitlich", 0.4)));
        boolean schutz = cfg().getBoolean("tnt.lift.fallschaden-verhindern", true);
        long schutzMs = Math.max(0, cfg().getInt("tnt.lift.fallschutz-sekunden", 15)) * 1000L;

        World welt = mitte.getWorld();
        welt.playSound(mitte, Sound.EXPLODE, 4.0F, 1.2F);
        partikel(mitte, Effect.EXPLOSION_HUGE, 0.0F, 1, 0);
        partikel(mitte, Effect.CLOUD, 0.2F, 80, 2);

        aufraeumen();
        long jetzt = System.currentTimeMillis();
        double r2 = radius * radius;
        for (Entity entity : welt.getNearbyEntities(mitte, radius, radius, radius)) {
            if (entity instanceof Hanging || entity.isDead()) {
                continue;
            }
            if (entity.getLocation().distanceSquared(mitte) > r2) {
                continue;
            }
            Vector weg = entity.getLocation().toVector().subtract(mitte.toVector());
            weg.setY(0);
            if (weg.lengthSquared() > 0.0001) {
                weg.normalize().multiply(seitlich);
            } else {
                weg = new Vector(0, 0, 0);
            }
            entity.setVelocity(new Vector(weg.getX(), hoehe, weg.getZ()));
            if (schutz) {
                fallschutz.put(entity.getUniqueId(), jetzt + schutzMs);
            }
        }
    }

    /**
     * Prüft, ob ein Fallschaden durch den Lift-Schutz verhindert werden soll.
     * Der Schutz gilt für genau eine Landung.
     */
    public boolean fallschutzVerbrauchen(UUID uuid) {
        Long ablauf = fallschutz.remove(uuid);
        return ablauf != null && ablauf >= System.currentTimeMillis();
    }

    private void aufraeumen() {
        long jetzt = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = fallschutz.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < jetzt) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ Cluster

    /** Verteilt nach der Explosion mehrere kleine, normale TNT in der Umgebung. */
    public void cluster(final Location mitte) {
        final int anzahl = Math.max(0, Math.min(32, cfg().getInt("tnt.cluster.anzahl", 6)));
        final float kleinStaerke = (float) Math.max(0.0, cfg().getDouble("tnt.cluster.klein-staerke", 2.0));
        int minTicks = Math.max(1, cfg().getInt("tnt.cluster.zuendzeit-min", 20));
        int maxTicks = Math.max(minTicks, cfg().getInt("tnt.cluster.zuendzeit-max", 50));
        final double streuung = Math.max(0.0, Math.min(2.0, cfg().getDouble("tnt.cluster.streuung", 0.35)));
        final int min = minTicks;
        final int spanne = maxTicks - minTicks;

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                World welt = mitte.getWorld();
                Location start = mitte.clone().add(0, 0.5, 0);
                for (int i = 0; i < anzahl; i++) {
                    TNTPrimed klein = welt.spawn(start, TNTPrimed.class);
                    klein.setYield(kleinStaerke);
                    klein.setFuseTicks(min + (spanne > 0 ? random.nextInt(spanne + 1) : 0));
                    klein.setVelocity(new Vector(
                            (random.nextDouble() * 2.0 - 1.0) * streuung,
                            0.3 + random.nextDouble() * 0.4,
                            (random.nextDouble() * 2.0 - 1.0) * streuung));
                }
            }
        });
    }

    // ------------------------------------------------------------------ Hilfen

    /** Vanilla-Partikel über die Spigot-API von 1.8.8. */
    private void partikel(Location ort, Effect effekt, float tempo, int anzahl, float streuung) {
        ort.getWorld().spigot().playEffect(ort, effekt, 0, 0,
                streuung, streuung, streuung, tempo, anzahl, 64);
    }
}
