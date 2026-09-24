package de.backrooms.gegner;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Eigene Backrooms-Gegner. Jeder Gegner besteht aus einem unsichtbaren
 * Vanilla-Mob (Bewegung, Trefferfläche) und einem eigenen Blockmodell
 * (siehe {@link Modelle}), das sich mit ihm bewegt.
 */
public enum GegnerTyp {

    //            ID,               Name,                                         Leben, Schaden, Tempo, Beschreibung
    TAPETENKRIECHER("tapetenkriecher", ChatColor.YELLOW + "Tapetenkriecher", 10, 2, 0.28,
            "Getarnt wie die Tapete, krabbelt auch Wände hoch"),
    GRINSER("grinser", ChatColor.WHITE + "Grinser", 16, 3, 0.23,
            "Schwarze Gestalt mit leuchtendem Kürbisgrinsen"),
    SCHATTENHUND("schattenhund", ChatColor.DARK_GRAY + "Schattenhund", 12, 2, 0.32,
            "Schneller schwarzer Hund"),
    PARTYBALLON("partyballon", ChatColor.LIGHT_PURPLE + "Partyballon", 12, 2, 0.0,
            "Hüpfendes Geschenk mit bunten Ballons"),
    ROHRGEIST("rohrgeist", ChatColor.GRAY + "Rohrgeist", 20, 3, 0.22,
            "Wesen aus Rohren mit leuchtendem Kopf"),
    WARDEN("warden", ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden", 200, 5, 0.25,
            "Endboss: riesig, Schallschlag und Dunkelheit");

    private final String id;
    private final String anzeigename;
    private final int standardLeben;
    private final int standardSchaden;
    private final double tempo;
    private final String beschreibung;

    GegnerTyp(String id, String anzeigename, int standardLeben, int standardSchaden, double tempo,
              String beschreibung) {
        this.id = id;
        this.anzeigename = anzeigename;
        this.standardLeben = standardLeben;
        this.standardSchaden = standardSchaden;
        this.tempo = tempo;
        this.beschreibung = beschreibung;
    }

    public String getId() {
        return id;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    public int getStandardLeben() {
        return standardLeben;
    }

    public int getStandardSchaden() {
        return standardSchaden;
    }

    /** Laufgeschwindigkeit (Attribut), 0 = Vanilla-Wert behalten. */
    public double getTempo() {
        return tempo;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public static GegnerTyp vonId(String text) {
        if (text == null) {
            return null;
        }
        for (GegnerTyp typ : values()) {
            if (typ.id.equalsIgnoreCase(text) || typ.name().equalsIgnoreCase(text)) {
                return typ;
            }
        }
        return null;
    }

    public static List<String> alleIds() {
        List<String> ids = new ArrayList<String>();
        for (GegnerTyp typ : values()) {
            ids.add(typ.id);
        }
        return ids;
    }
}
