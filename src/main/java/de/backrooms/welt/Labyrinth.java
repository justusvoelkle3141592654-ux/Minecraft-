package de.backrooms.welt;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Deterministischer Aufbau der Labyrinthe. Alles wird aus dem Welt-Seed und
 * den Weltkoordinaten berechnet. Dadurch passen die Chunks nahtlos zusammen und
 * der Ausgangs-Kompass kann die Ausgänge berechnen, ohne Blöcke zu lesen.
 *
 * Aufbau je Level: ein Raster aus Zellen der Größe {@link Level#getZelle()}.
 * Auf den Rasterlinien stehen Wände (Segmente), an den Kreuzungen Säulen.
 * Jedes Segment ist offen, hat eine Tür (2 breit) oder ist eine volle Wand.
 */
public final class Labyrinth {

    public enum Segment { OFFEN, TUER, WAND }

    private static final long SALZ_SEGMENT = 11;
    private static final long SALZ_TUER = 12;
    private static final long SALZ_AUSGANG = 13;
    private static final long SALZ_HALLE = 14;
    private static final long SALZ_LICHT = 15;

    private Labyrinth() {
    }

    // ------------------------------------------------------------ Zufall

    static long mix(long seed, long a, long b, long c) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (c * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** Pseudozufall im Bereich [0, 1). */
    static double zufall(long seed, Level level, long a, long b, long salz) {
        return (mix(seed, a, b, salz * 16 + level.ordinal()) >>> 11) * 0x1.0p-53;
    }

    // ------------------------------------------------------------ Zellen

    public static int zelleVon(int koordinate, Level level) {
        return Math.floorDiv(koordinate, level.getZelle());
    }

    public static int innerhalb(int koordinate, Level level) {
        return Math.floorMod(koordinate, level.getZelle());
    }

    /** Die Startzelle (0,0) und ihre Nachbarn haben nie einen Ausgang. */
    private static boolean nahStart(int cx, int cz) {
        return Math.abs(cx) <= 2 && Math.abs(cz) <= 2;
    }

    public static boolean istAusgang(long seed, Level level, int cx, int cz) {
        if (level.istBoss() || nahStart(cx, cz)) {
            return false;
        }
        return zufall(seed, level, cx, cz, SALZ_AUSGANG) < level.getAusgangsChance();
    }

    /** Große offene Hallen (6x6 Zellen), nicht in Level 2. */
    private static boolean istHalle(long seed, Level level, int cx, int cz) {
        if (level == Level.LEVEL_2) {
            return false;
        }
        int rx = Math.floorDiv(cx, 6);
        int rz = Math.floorDiv(cz, 6);
        return zufall(seed, level, rx, rz, SALZ_HALLE) < 0.12;
    }

    /** Zellen, deren Wände immer offen sind (Start und Ausgänge sind so immer erreichbar). */
    private static boolean immerOffen(long seed, Level level, int cx, int cz) {
        return (cx == 0 && cz == 0) || istAusgang(seed, level, cx, cz);
    }

    /**
     * Zustand eines Wandsegments.
     *
     * @param senkrecht true = Wand auf der Linie x = cx * zelle (Westseite der Zelle),
     *                  false = Wand auf der Linie z = cz * zelle (Nordseite der Zelle)
     */
    public static Segment segment(long seed, Level level, int cx, int cz, boolean senkrecht) {
        int nx = senkrecht ? cx - 1 : cx;
        int nz = senkrecht ? cz : cz - 1;
        if (immerOffen(seed, level, cx, cz) || immerOffen(seed, level, nx, nz)) {
            return Segment.OFFEN;
        }
        if (istHalle(seed, level, cx, cz) && istHalle(seed, level, nx, nz)) {
            return Segment.OFFEN;
        }
        double offen;
        double tuer;
        switch (level) {
            case LEVEL_1:
                offen = 0.45;
                tuer = 0.35;
                break;
            case LEVEL_2:
                offen = 0.30;
                tuer = 0.42;
                break;
            default:
                offen = 0.35;
                tuer = 0.40;
                break;
        }
        double r = zufall(seed, level, cx, cz, SALZ_SEGMENT * 2 + (senkrecht ? 1 : 0));
        if (r < offen) {
            return Segment.OFFEN;
        }
        return r < offen + tuer ? Segment.TUER : Segment.WAND;
    }

    /** Position der 2 Blöcke breiten Türöffnung innerhalb des Segments (1 .. zelle-2). */
    public static int tuerPosition(long seed, Level level, int cx, int cz, boolean senkrecht) {
        int moeglich = level.getZelle() - 2;
        double r = zufall(seed, level, cx, cz, SALZ_TUER * 2 + (senkrecht ? 1 : 0));
        return 1 + (int) (r * moeglich);
    }

    /** Ob die Deckenleuchte der Zelle vorhanden ist. */
    public static boolean hatLicht(long seed, Level level, int cx, int cz) {
        double chance;
        switch (level) {
            case LEVEL_1:
                chance = 0.55;
                break;
            case LEVEL_2:
                chance = 0.15;
                break;
            default:
                chance = 0.92;
                break;
        }
        return zufall(seed, level, cx, cz, SALZ_LICHT) < chance;
    }

    // ------------------------------------------------------------ Ausgänge

    /** Mitte des Ausgangsfeldes einer Zelle (auf dem Boden, Spielerhöhe). */
    public static Location ausgangsMitte(World welt, Level level, int cx, int cz) {
        int z = level.getZelle();
        return new Location(welt, cx * z + z / 2 + 0.5, level.getBodenY() + 1, cz * z + z / 2 + 0.5);
    }

    /**
     * Sucht den nächsten Ausgang im Umkreis von {@code radiusZellen} Zellen.
     *
     * @return Position des Ausgangs oder {@code null}
     */
    public static Location naechsterAusgang(World welt, Level level, double x, double z, int radiusZellen) {
        if (level.istBoss()) {
            return null;
        }
        long seed = welt.getSeed();
        int zx = zelleVon((int) Math.floor(x), level);
        int zz = zelleVon((int) Math.floor(z), level);
        Location beste = null;
        double besteDist = Double.MAX_VALUE;
        for (int dx = -radiusZellen; dx <= radiusZellen; dx++) {
            for (int dz = -radiusZellen; dz <= radiusZellen; dz++) {
                int cx = zx + dx;
                int cz = zz + dz;
                if (!istAusgang(seed, level, cx, cz)) {
                    continue;
                }
                Location ort = ausgangsMitte(welt, level, cx, cz);
                double ax = ort.getX() - x;
                double az = ort.getZ() - z;
                double dist = ax * ax + az * az;
                if (dist < besteDist) {
                    besteDist = dist;
                    beste = ort;
                }
            }
        }
        return beste;
    }
}
