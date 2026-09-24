package de.sondertnt.tnt;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Alle Sonder-TNT-Typen. Die ID wird in Befehlen, in der config.yml
 * und in der Lore-Markierung der Items verwendet.
 */
public enum TntTyp {

    MEGA("mega", ChatColor.RED + "" + ChatColor.BOLD + "Mega-TNT",
            "Eine gewaltige Explosion."),
    FEUER("feuer", ChatColor.GOLD + "" + ChatColor.BOLD + "Feuer-TNT",
            "Setzt die Umgebung in Brand."),
    BLITZ("blitz", ChatColor.YELLOW + "" + ChatColor.BOLD + "Blitz-TNT",
            "Ruft beim Explodieren Blitze herbei."),
    LIFT("lift", ChatColor.AQUA + "" + ChatColor.BOLD + "Lift-TNT",
            "Schleudert Spieler und Mobs in die Luft.",
            "Zerstört keine Blöcke."),
    CLUSTER("cluster", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Cluster-TNT",
            "Zerfällt in mehrere kleine TNT.");

    private final String id;
    private final String anzeigename;
    private final List<String> beschreibung;

    TntTyp(String id, String anzeigename, String... beschreibung) {
        this.id = id;
        this.anzeigename = anzeigename;
        this.beschreibung = Collections.unmodifiableList(Arrays.asList(beschreibung));
    }

    public String getId() {
        return id;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    public List<String> getBeschreibung() {
        return beschreibung;
    }

    /** Sucht einen Typ anhand seiner ID oder seines Enum-Namens (Groß-/Kleinschreibung egal). */
    public static TntTyp vonId(String text) {
        if (text == null) {
            return null;
        }
        for (TntTyp typ : values()) {
            if (typ.id.equalsIgnoreCase(text) || typ.name().equalsIgnoreCase(text)) {
                return typ;
            }
        }
        return null;
    }

    public static List<String> alleIds() {
        List<String> ids = new ArrayList<String>();
        for (TntTyp typ : values()) {
            ids.add(typ.id);
        }
        return ids;
    }
}
