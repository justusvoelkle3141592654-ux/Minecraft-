package de.sondertnt.struktur;

import org.bukkit.Material;

import java.util.Random;

/**
 * Basisklasse aller Strukturen. Gebaut wird ausschließlich mit Vanilla-Blöcken
 * über die Bukkit-API (kein WorldEdit, keine Schematic-Dateien).
 */
public abstract class Struktur {

    private final String id;
    private final String anzeigename;
    private final String beschreibung;

    protected Struktur(String id, String anzeigename, String beschreibung) {
        this.id = id;
        this.anzeigename = anzeigename;
        this.beschreibung = beschreibung;
    }

    public String getId() {
        return id;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    /** Ausdehnung in lokaler X-Richtung. */
    public abstract int getBreite();

    /** Ausdehnung in lokaler Z-Richtung (Vorderseite bei z = tiefe - 1). */
    public abstract int getTiefe();

    /** Höhe über der Bodenebene. */
    public abstract int getHoehe();

    /** Material für das Fundament unter der Struktur. */
    protected Material getFundament() {
        return Material.COBBLESTONE;
    }

    /** Baut die Struktur. Der Bauraum ist zu diesem Zeitpunkt bereits freigeräumt. */
    protected abstract void bauen(Bauplan plan, Random random, StrukturManager manager);

    public final void errichten(Bauplan plan, Random random, StrukturManager manager) {
        plan.vorbereiten(getHoehe(), getFundament(), 12);
        bauen(plan, random, manager);
    }

    // ------------------------------------------------------------ Hilfen

    /** Steinziegel, zufällig auch rissig (Daten 2) oder bemoost (Daten 1). */
    protected static void steinziegel(Bauplan plan, int x, int y, int z, Random random) {
        double w = random.nextDouble();
        int daten = w < 0.12 ? 2 : (w < 0.24 ? 1 : 0);
        plan.setze(x, y, z, Material.SMOOTH_BRICK, daten);
    }

    /** Zufälliger "alter" Stein für Ruinen. */
    protected static void alterStein(Bauplan plan, int x, int y, int z, Random random) {
        double w = random.nextDouble();
        if (w < 0.35) {
            plan.setze(x, y, z, Material.COBBLESTONE);
        } else if (w < 0.65) {
            plan.setze(x, y, z, Material.MOSSY_COBBLESTONE);
        } else {
            steinziegel(plan, x, y, z, random);
        }
    }

    /** Stehende Fackel (Datenwert 5). */
    protected static void fackel(Bauplan plan, int x, int y, int z) {
        plan.setze(x, y, z, Material.TORCH, 5);
    }
}
