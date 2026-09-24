package de.sondertnt.struktur;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

/** Kleine Festung mit Mauer, vier Ecktürmen, Wehrgang, Tor und Bergfried. */
public class Festung extends Struktur {

    private static final int GROESSE = 15;
    private static final int MAUER = 5;
    private static final int TURM = 8;

    public Festung() {
        super("festung", "Kleine Festung", "Ummauerter Hof mit Ecktürmen, Wehrgang und Bergfried");
    }

    @Override
    public int getBreite() {
        return GROESSE;
    }

    @Override
    public int getTiefe() {
        return GROESSE;
    }

    @Override
    public int getHoehe() {
        return TURM + 2;
    }

    @Override
    protected void bauen(Bauplan plan, Random random, StrukturManager manager) {
        int max = GROESSE - 1;

        // Hof
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                plan.setze(x, 0, z, random.nextDouble() < 0.2 ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE);
            }
        }

        // Ringmauer mit Zinnen
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                if (!plan.istRand(x, z)) {
                    continue;
                }
                for (int y = 1; y <= MAUER; y++) {
                    steinziegel(plan, x, y, z, random);
                }
                if ((x + z) % 2 == 0) {
                    plan.setze(x, MAUER + 1, z, Material.SMOOTH_BRICK);
                }
            }
        }

        // Wehrgang aus Steinziegelstufen (Daten 5) an der Innenseite
        for (int x = 1; x < max; x++) {
            for (int z = 1; z < max; z++) {
                boolean innenring = x == 1 || z == 1 || x == max - 1 || z == max - 1;
                if (innenring) {
                    plan.setze(x, MAUER, z, Material.STEP, 5);
                }
            }
        }

        // Ecktürme (3x3) mit Zinnen und Fackel
        int[] ecken = {0, max - 2};
        for (int tx : ecken) {
            for (int tz : ecken) {
                for (int x = tx; x <= tx + 2; x++) {
                    for (int z = tz; z <= tz + 2; z++) {
                        for (int y = 1; y <= TURM; y++) {
                            steinziegel(plan, x, y, z, random);
                        }
                    }
                }
                plan.setze(tx, TURM + 1, tz, Material.SMOOTH_BRICK);
                plan.setze(tx + 2, TURM + 1, tz, Material.SMOOTH_BRICK);
                plan.setze(tx, TURM + 1, tz + 2, Material.SMOOTH_BRICK);
                plan.setze(tx + 2, TURM + 1, tz + 2, Material.SMOOTH_BRICK);
                fackel(plan, tx + 1, TURM + 1, tz + 1);
            }
        }

        // Tor vorne mit halb heruntergelassenem Fallgitter
        plan.fuellen(6, 1, max, 8, 3, max, Material.AIR);
        plan.fuellen(6, 3, max, 8, 3, max, Material.IRON_FENCE);

        // Leiter zum Wehrgang an der Rückmauer
        int leiterDaten = plan.blickDaten(BlockFace.SOUTH);
        for (int y = 1; y <= MAUER; y++) {
            plan.setze(7, y, 1, Material.LADDER, leiterDaten);
        }

        // Bergfried in der Mitte (5x5)
        int k1 = 5;
        int k2 = 9;
        for (int x = k1; x <= k2; x++) {
            for (int z = k1; z <= k2; z++) {
                boolean rand = x == k1 || x == k2 || z == k1 || z == k2;
                if (rand) {
                    for (int y = 1; y <= 4; y++) {
                        steinziegel(plan, x, y, z, random);
                    }
                    if ((x + z) % 2 == 0) {
                        plan.setze(x, 6, z, Material.SMOOTH_BRICK);
                    }
                }
                plan.setze(x, 5, z, Material.SMOOTH_BRICK);
            }
        }
        plan.setze(7, 1, k2, Material.AIR);
        plan.setze(7, 2, k2, Material.AIR);
        plan.setze(k1, 2, 7, Material.IRON_FENCE);
        plan.setze(k2, 2, 7, Material.IRON_FENCE);
        fackel(plan, k1 + 1, 1, k1 + 1);
        fackel(plan, k2 - 1, 1, k1 + 1);
        fackel(plan, 7, 6, 7);

        // Fackeln im Hof
        fackel(plan, 3, 1, 3);
        fackel(plan, max - 3, 1, 3);
        fackel(plan, 3, 1, max - 3);
        fackel(plan, max - 3, 1, max - 3);
    }
}
