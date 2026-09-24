package de.sondertnt.struktur;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

/** Steinziegel-Turm mit drei Etagen, Leiter, Fenstern und Zinnen. */
public class Turm extends Struktur {

    private static final int GROESSE = 7;
    private static final int DACH = 15;

    public Turm() {
        super("turm", "Turm", "Steinziegel-Turm mit Leiter und Aussichtsplattform");
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
        return DACH + 2;
    }

    @Override
    protected void bauen(Bauplan plan, Random random, StrukturManager manager) {
        int max = GROESSE - 1;

        // Boden
        plan.fuellen(0, 0, 0, max, 0, max, Material.SMOOTH_BRICK);

        // Außenwände
        for (int y = 1; y < DACH; y++) {
            for (int x = 0; x <= max; x++) {
                for (int z = 0; z <= max; z++) {
                    if (plan.istRand(x, z)) {
                        steinziegel(plan, x, y, z, random);
                    }
                }
            }
        }

        // Zwischenböden aus Holz und Dachplattform
        plan.fuellen(1, 5, 1, max - 1, 5, max - 1, Material.WOOD);
        plan.fuellen(1, 10, 1, max - 1, 10, max - 1, Material.WOOD);
        plan.fuellen(0, DACH, 0, max, DACH, max, Material.SMOOTH_BRICK);

        // Zinnen
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                if (plan.istRand(x, z) && (x + z) % 2 == 0) {
                    plan.setze(x, DACH + 1, z, Material.SMOOTH_BRICK);
                }
            }
        }

        // Eingang vorne
        plan.setze(3, 1, max, Material.AIR);
        plan.setze(3, 2, max, Material.AIR);

        // Fenster (Eisengitter) links, rechts und vorne
        for (int y : new int[]{7, 12}) {
            plan.setze(0, y, 3, Material.IRON_FENCE);
            plan.setze(max, y, 3, Material.IRON_FENCE);
            plan.setze(3, y, max, Material.IRON_FENCE);
        }

        // Leiter an der Rückwand bis durch die Dachplattform
        int leiterDaten = plan.blickDaten(BlockFace.SOUTH);
        for (int y = 1; y <= DACH; y++) {
            plan.setze(3, y, 1, Material.LADDER, leiterDaten);
        }

        // Fackeln
        fackel(plan, 1, 1, max - 1);
        fackel(plan, max - 1, 1, max - 1);
        fackel(plan, 1, 6, max - 1);
        fackel(plan, max - 1, 11, max - 1);
        fackel(plan, 1, DACH + 1, max - 1);
        fackel(plan, max - 1, DACH + 1, max - 1);
    }
}
