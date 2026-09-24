package de.sondertnt.struktur;

import org.bukkit.Material;

import java.util.Random;

/** Verfallene Ruine mit zerbrochenen Mauern, Säulenresten, Schutt und Spinnweben. */
public class Ruine extends Struktur {

    private static final int GROESSE = 11;

    public Ruine() {
        super("ruine", "Ruine", "Verfallene Mauerreste mit Säulen und Spinnweben");
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
        return 6;
    }

    @Override
    protected void bauen(Bauplan plan, Random random, StrukturManager manager) {
        int max = GROESSE - 1;

        // Lückenhafter Boden
        for (int x = 1; x < max; x++) {
            for (int z = 1; z < max; z++) {
                double w = random.nextDouble();
                if (w < 0.1) {
                    plan.setze(x, 0, z, Material.GRAVEL);
                } else if (w < 0.7) {
                    alterStein(plan, x, 0, z, random);
                }
            }
        }

        // Zerbrochene Mauern: unregelmäßige Höhen, Ecken am höchsten
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                if (!plan.istRand(x, z)) {
                    continue;
                }
                plan.setze(x, 0, z, Material.COBBLESTONE);
                boolean ecke = (x == 0 || x == max) && (z == 0 || z == max);
                int hoehe = ecke ? 3 + random.nextInt(3) : random.nextInt(5);
                for (int y = 1; y <= hoehe; y++) {
                    if (y > 1 && random.nextDouble() < 0.15) {
                        break; // herausgebrochenes Stück
                    }
                    alterStein(plan, x, y, z, random);
                }
            }
        }

        // Eingang vorne
        plan.fuellen(4, 1, max, 6, getHoehe(), max, Material.AIR);

        // Säulenreste
        int[][] saeulen = {{3, 3}, {7, 3}, {3, 7}, {7, 7}};
        for (int[] s : saeulen) {
            int hoehe = 1 + random.nextInt(5);
            for (int y = 1; y <= hoehe; y++) {
                steinziegel(plan, s[0], y, s[1], random);
            }
            if (random.nextBoolean()) {
                plan.setze(s[0], hoehe + 1, s[1], Material.STEP, 5); // Steinziegelstufe
            }
        }

        // Schutt
        for (int i = 0; i < 10; i++) {
            int x = 1 + random.nextInt(max - 1);
            int z = 1 + random.nextInt(max - 1);
            if (plan.typ(x, 1, z) == Material.AIR) {
                if (random.nextBoolean()) {
                    alterStein(plan, x, 1, z, random);
                } else {
                    plan.setze(x, 1, z, Material.STEP, 3); // Bruchsteinstufe
                }
            }
        }

        // Spinnweben
        for (int i = 0; i < 5; i++) {
            int x = 1 + random.nextInt(max - 1);
            int y = 1 + random.nextInt(3);
            int z = 1 + random.nextInt(max - 1);
            if (plan.typ(x, y, z) == Material.AIR) {
                plan.setze(x, y, z, Material.WEB);
            }
        }
    }
}
