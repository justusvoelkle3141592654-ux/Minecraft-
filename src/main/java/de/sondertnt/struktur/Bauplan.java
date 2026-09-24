package de.sondertnt.struktur;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Setzt Blöcke in lokalen Koordinaten einer Struktur und dreht sie dabei
 * in 90-Grad-Schritten. Lokal liegt die Vorderseite (Eingang) immer bei
 * z = tiefe - 1 und zeigt nach Süden (+z).
 *
 * Der Ursprung (ox, oy, oz) ist die kleinste Welt-Ecke der (gedrehten)
 * Grundfläche. oy ist die Bodenebene (lokal y = 0).
 */
public class Bauplan {

    private final World welt;
    private final int ox;
    private final int oy;
    private final int oz;
    private final int breite;
    private final int tiefe;
    private final int rotation;

    public Bauplan(World welt, int ox, int oy, int oz, int breite, int tiefe, int rotation) {
        this.welt = welt;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.breite = breite;
        this.tiefe = tiefe;
        this.rotation = ((rotation % 4) + 4) % 4;
    }

    /** Ausdehnung in Welt-X-Richtung nach der Drehung. */
    public static int groesseX(int breite, int tiefe, int rotation) {
        return rotation % 2 == 0 ? breite : tiefe;
    }

    /** Ausdehnung in Welt-Z-Richtung nach der Drehung. */
    public static int groesseZ(int breite, int tiefe, int rotation) {
        return rotation % 2 == 0 ? tiefe : breite;
    }

    /** Liefert die Drehung, bei der die Vorderseite in die angegebene Richtung zeigt. */
    public static int rotationFuerVorderseite(BlockFace richtung) {
        switch (richtung) {
            case WEST:
                return 1;
            case NORTH:
                return 2;
            case EAST:
                return 3;
            default:
                return 0;
        }
    }

    public Block block(int x, int y, int z) {
        int wx;
        int wz;
        switch (rotation) {
            case 1:
                wx = tiefe - 1 - z;
                wz = x;
                break;
            case 2:
                wx = breite - 1 - x;
                wz = tiefe - 1 - z;
                break;
            case 3:
                wx = z;
                wz = breite - 1 - x;
                break;
            default:
                wx = x;
                wz = z;
                break;
        }
        return welt.getBlockAt(ox + wx, oy + y, oz + wz);
    }

    private boolean hoeheGueltig(int y) {
        int wy = oy + y;
        return wy >= 0 && wy < welt.getMaxHeight();
    }

    public void setze(int x, int y, int z, Material material) {
        setze(x, y, z, material, 0);
    }

    /** Setzt einen Block mit Datenwert (in 1.8.8 nur über die veraltete ID-Methode möglich). */
    @SuppressWarnings("deprecation")
    public void setze(int x, int y, int z, Material material, int daten) {
        if (!hoeheGueltig(y)) {
            return;
        }
        block(x, y, z).setTypeIdAndData(material.getId(), (byte) daten, false);
    }

    public Material typ(int x, int y, int z) {
        if (!hoeheGueltig(y)) {
            return Material.AIR;
        }
        return block(x, y, z).getType();
    }

    public void fuellen(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        fuellen(x1, y1, z1, x2, y2, z2, material, 0);
    }

    public void fuellen(int x1, int y1, int z1, int x2, int y2, int z2, Material material, int daten) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    setze(x, y, z, material, daten);
                }
            }
        }
    }

    public boolean istRand(int x, int z) {
        return x == 0 || z == 0 || x == breite - 1 || z == tiefe - 1;
    }

    /** Dreht eine lokale Richtung in die Weltrichtung. */
    public BlockFace richtung(BlockFace lokal) {
        BlockFace ergebnis = lokal;
        for (int i = 0; i < rotation; i++) {
            ergebnis = imUhrzeigersinn(ergebnis);
        }
        return ergebnis;
    }

    /**
     * Datenwert für Leitern und Truhen, die in die lokale Richtung zeigen
     * (1.8: 2 = Norden, 3 = Süden, 4 = Westen, 5 = Osten).
     */
    public int blickDaten(BlockFace lokal) {
        switch (richtung(lokal)) {
            case NORTH:
                return 2;
            case SOUTH:
                return 3;
            case WEST:
                return 4;
            case EAST:
                return 5;
            default:
                return 2;
        }
    }

    private static BlockFace imUhrzeigersinn(BlockFace f) {
        switch (f) {
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            default:
                return f;
        }
    }

    /**
     * Räumt den Bauraum frei (lokal y = 1 bis hoehe) und füllt Lücken unter
     * der Grundfläche mit einem Fundament auf, damit nichts in der Luft schwebt.
     */
    public void vorbereiten(int hoehe, Material fundament, int maxTiefe) {
        for (int x = 0; x < breite; x++) {
            for (int z = 0; z < tiefe; z++) {
                for (int y = 1; y <= hoehe; y++) {
                    if (!hoeheGueltig(y)) {
                        continue;
                    }
                    Block b = block(x, y, z);
                    if (b.getType() != Material.AIR) {
                        b.setType(Material.AIR, false);
                    }
                }
                for (int y = 0; y > -maxTiefe; y--) {
                    if (oy + y < 1) {
                        break;
                    }
                    Block b = block(x, y, z);
                    if (b.getType().isSolid()) {
                        break;
                    }
                    setze(x, y, z, fundament);
                }
            }
        }
    }

    public World getWelt() {
        return welt;
    }

    public int getBreite() {
        return breite;
    }

    public int getTiefe() {
        return tiefe;
    }
}
