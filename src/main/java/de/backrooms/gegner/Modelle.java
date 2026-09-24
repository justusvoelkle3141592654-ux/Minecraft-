package de.backrooms.gegner;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Blockmodelle der Gegner. Jedes Teil ist ein Block auf dem Kopf eines
 * unsichtbaren Rüstungsständers. So ein Block ist 0,625 Blöcke groß, deshalb
 * sind alle Positionen in "Modell-Einheiten" (1 Einheit = 0,625 Blöcke)
 * angegeben: rechts, hoch (Mitte des Teils über dem Boden) und vor.
 */
public final class Modelle {

    /** Größe eines Modellblocks in Weltblöcken. */
    public static final double EINHEIT = 0.625;

    /** Ein Teil des Modells. */
    public static final class Teil {
        final double rechts;
        final double hoch;
        final double vor;
        final Material material;
        final short daten;

        Teil(double rechts, double hoch, double vor, Material material, int daten) {
            this.rechts = rechts * EINHEIT;
            this.hoch = (hoch + 0.5) * EINHEIT;
            this.vor = vor * EINHEIT;
            this.material = material;
            this.daten = (short) daten;
        }
    }

    private Modelle() {
    }

    private static Teil t(double rechts, double hoch, double vor, Material material, int daten) {
        return new Teil(rechts, hoch, vor, material, daten);
    }

    private static Teil t(double rechts, double hoch, double vor, Material material) {
        return new Teil(rechts, hoch, vor, material, 0);
    }

    public static List<Teil> fuer(GegnerTyp typ) {
        List<Teil> teile = new ArrayList<Teil>();
        switch (typ) {
            case TAPETENKRIECHER:
                // flacher, gelber Körper wie die Tapete, rotes Auge, Zaun-Beine
                teile.add(t(-0.5, 0, -0.5, Material.SANDSTONE, 2));
                teile.add(t(0.5, 0, -0.5, Material.SANDSTONE, 2));
                teile.add(t(-0.5, 0, 0.5, Material.SANDSTONE, 2));
                teile.add(t(0.5, 0, 0.5, Material.SANDSTONE, 2));
                teile.add(t(0, 0.6, 1.0, Material.REDSTONE_BLOCK));
                teile.add(t(-1.3, -0.1, -0.6, Material.FENCE));
                teile.add(t(1.3, -0.1, -0.6, Material.FENCE));
                teile.add(t(-1.3, -0.1, 0.6, Material.FENCE));
                teile.add(t(1.3, -0.1, 0.6, Material.FENCE));
                break;
            case GRINSER:
                // schwarze Gestalt mit Kürbisgrinsen
                teile.add(t(0, 0, 0, Material.WOOL, 15));
                teile.add(t(0, 1, 0, Material.WOOL, 15));
                teile.add(t(0, 2, 0, Material.COAL_BLOCK));
                teile.add(t(-1, 2, 0, Material.WOOL, 15));
                teile.add(t(1, 2, 0, Material.WOOL, 15));
                teile.add(t(0, 3, 0, Material.JACK_O_LANTERN));
                break;
            case SCHATTENHUND:
                teile.add(t(0, 0.3, -0.5, Material.WOOL, 15));
                teile.add(t(0, 0.3, 0.5, Material.WOOL, 15));
                teile.add(t(0, 1.1, 1.3, Material.COAL_BLOCK));
                teile.add(t(0, 0.9, -1.3, Material.WOOL, 7));
                teile.add(t(-0.4, -0.4, -0.6, Material.FENCE));
                teile.add(t(0.4, -0.4, -0.6, Material.FENCE));
                teile.add(t(-0.4, -0.4, 0.6, Material.FENCE));
                teile.add(t(0.4, -0.4, 0.6, Material.FENCE));
                break;
            case PARTYBALLON:
                // Geschenk mit Schleife und bunten Ballons
                teile.add(t(0, 0, 0, Material.WOOL, 14));
                teile.add(t(0, 0.7, 0, Material.WOOL, 4));
                teile.add(t(0, 1.5, 0, Material.FENCE));
                teile.add(t(0, 2.5, 0, Material.WOOL, 14));
                teile.add(t(0.9, 2.8, 0.2, Material.WOOL, 4));
                teile.add(t(-0.9, 2.7, -0.2, Material.WOOL, 11));
                teile.add(t(0.1, 3.5, -0.6, Material.WOOL, 5));
                break;
            case ROHRGEIST:
                teile.add(t(-0.4, 0, 0, Material.IRON_FENCE));
                teile.add(t(0.4, 0, 0, Material.IRON_FENCE));
                teile.add(t(0, 1, 0, Material.CONCRETE, 8));
                teile.add(t(0, 2, 0, Material.CONCRETE, 8));
                teile.add(t(-1, 2, 0, Material.IRON_FENCE));
                teile.add(t(1, 2, 0, Material.IRON_FENCE));
                teile.add(t(0, 3, 0, Material.SEA_LANTERN));
                break;
            case WARDEN:
                // Beine
                teile.add(t(-0.55, 0, 0, Material.PRISMARINE, 2));
                teile.add(t(0.55, 0, 0, Material.PRISMARINE, 2));
                teile.add(t(-0.55, 1, 0, Material.PRISMARINE, 2));
                teile.add(t(0.55, 1, 0, Material.PRISMARINE, 2));
                // Rumpf mit leuchtender "Seele" in der Brust
                teile.add(t(-0.5, 2, 0, Material.PRISMARINE, 2));
                teile.add(t(0.5, 2, 0, Material.PRISMARINE, 2));
                teile.add(t(-0.5, 3, 0, Material.PRISMARINE, 2));
                teile.add(t(0.5, 3, 0, Material.PRISMARINE, 2));
                teile.add(t(0, 2.6, 0.6, Material.SEA_LANTERN));
                // lange Arme
                teile.add(t(-1.5, 1, 0, Material.CONCRETE, 9));
                teile.add(t(1.5, 1, 0, Material.CONCRETE, 9));
                teile.add(t(-1.5, 2, 0, Material.CONCRETE, 9));
                teile.add(t(1.5, 2, 0, Material.CONCRETE, 9));
                teile.add(t(-1.5, 3, 0, Material.PRISMARINE, 2));
                teile.add(t(1.5, 3, 0, Material.PRISMARINE, 2));
                // Kopf mit Hörnern
                teile.add(t(-0.5, 4.1, 0, Material.PRISMARINE, 2));
                teile.add(t(0.5, 4.1, 0, Material.PRISMARINE, 2));
                teile.add(t(0, 4.1, 0.5, Material.CONCRETE, 15));
                teile.add(t(-1.3, 4.7, 0, Material.PRISMARINE, 1));
                teile.add(t(1.3, 4.7, 0, Material.PRISMARINE, 1));
                teile.add(t(-1.8, 5.3, 0, Material.SEA_LANTERN));
                teile.add(t(1.8, 5.3, 0, Material.SEA_LANTERN));
                break;
            default:
                break;
        }
        return Collections.unmodifiableList(teile);
    }
}
