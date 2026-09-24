package de.sondertnt.struktur;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

/** Holzhaus mit Walmdach und einer Truhe mit Zufallsloot. */
public class Schatzhaus extends Struktur {

    private static final int BREITE = 7;
    private static final int TIEFE = 9;

    public Schatzhaus() {
        super("schatzhaus", "Schatzhaus", "Holzhaus mit einer Schatztruhe (Zufallsloot)");
    }

    @Override
    public int getBreite() {
        return BREITE;
    }

    @Override
    public int getTiefe() {
        return TIEFE;
    }

    @Override
    public int getHoehe() {
        return 9;
    }

    @Override
    protected void bauen(Bauplan plan, Random random, StrukturManager manager) {
        int mx = BREITE - 1;
        int mz = TIEFE - 1;

        // Boden: Bruchsteinrand, Holzdielen innen
        plan.fuellen(0, 0, 0, mx, 0, mz, Material.COBBLESTONE);
        plan.fuellen(1, 0, 1, mx - 1, 0, mz - 1, Material.WOOD);

        // Wände aus Holzbrettern, Ecken aus Eichenstämmen
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x <= mx; x++) {
                for (int z = 0; z <= mz; z++) {
                    if (!plan.istRand(x, z)) {
                        continue;
                    }
                    boolean ecke = (x == 0 || x == mx) && (z == 0 || z == mz);
                    plan.setze(x, y, z, ecke ? Material.LOG : Material.WOOD);
                }
            }
        }

        // Fenster
        plan.setze(0, 2, 3, Material.THIN_GLASS);
        plan.setze(0, 2, 5, Material.THIN_GLASS);
        plan.setze(mx, 2, 3, Material.THIN_GLASS);
        plan.setze(mx, 2, 5, Material.THIN_GLASS);
        plan.setze(2, 2, 0, Material.THIN_GLASS);
        plan.setze(4, 2, 0, Material.THIN_GLASS);
        plan.setze(1, 2, mz, Material.THIN_GLASS);
        plan.setze(5, 2, mz, Material.THIN_GLASS);

        // Eingang vorne
        plan.setze(3, 1, mz, Material.AIR);
        plan.setze(3, 2, mz, Material.AIR);

        // Walmdach
        plan.fuellen(0, 5, 0, mx, 5, mz, Material.WOOD);
        plan.fuellen(1, 6, 1, mx - 1, 6, mz - 1, Material.WOOD);
        plan.fuellen(2, 7, 2, mx - 2, 7, mz - 2, Material.WOOD);
        plan.fuellen(3, 8, 3, 3, 8, mz - 3, Material.WOOD_STEP);

        // Einrichtung
        plan.setze(1, 1, 3, Material.BOOKSHELF);
        plan.setze(1, 1, 4, Material.BOOKSHELF);
        plan.setze(1, 1, 5, Material.BOOKSHELF);
        plan.setze(mx - 1, 1, 4, Material.WORKBENCH);
        fackel(plan, 1, 1, 1);
        fackel(plan, mx - 1, 1, 1);
        fackel(plan, 1, 1, mz - 1);
        fackel(plan, mx - 1, 1, mz - 1);

        // Schatztruhe an der Rückwand, Blick zum Eingang
        plan.setze(3, 1, 1, Material.CHEST, plan.blickDaten(BlockFace.SOUTH));
        manager.truheFuellen(plan.block(3, 1, 1), random);
    }
}
