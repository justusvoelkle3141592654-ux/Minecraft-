package de.backrooms.gegner;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Gegner der Backrooms. Eaglercraft erlaubt keine eigenen Modelle, darum ist
 * jeder Gegner ein Vanilla-Mob mit Namen, Ausrüstung, Effekten und
 * Spezialfähigkeiten.
 */
public enum GegnerTyp {

    HOUND("hound", ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Hound", 24, 2, false,
            "Schneller Jäger in Rudeln (wütender Wolf)"),
    SMILER("smiler", ChatColor.WHITE + "" + ChatColor.BOLD + "Smiler", 40, 3, true,
            "Lauert im Dunkeln, scheut die Taschenlampe (Enderman)"),
    SKIN_STEALER("skin_stealer", ChatColor.RED + "Skin-Stealer", 30, 2, false,
            "Sieht aus wie ein Spieler (Zombie mit Spielerkopf)"),
    PARTYGOER("partygoer", ChatColor.YELLOW + "" + ChatColor.BOLD + "Partygoer", 24, 2, false,
            "Grinsender Party-Gast mit Bogen (Skelett)"),
    FACELING("faceling", ChatColor.GRAY + "Faceling", 20, 2, false,
            "Friedlich, bis man es angreift (Zombie-Dorfbewohner)"),
    TODESMOTTE("todesmotte", ChatColor.GOLD + "Todesmotte", 12, 1, false,
            "Kleiner, giftiger Schwarm-Gegner (Höhlenspinne)"),
    WARDEN("warden", ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden", 300, 3, false,
            "Endboss: Dunkelheit und Schallschlag (Wither-Skelett)");

    private final String id;
    private final String anzeigename;
    private final int standardLeben;
    private final int luftBedarf;
    private final boolean nurImDunkeln;
    private final String beschreibung;

    GegnerTyp(String id, String anzeigename, int standardLeben, int luftBedarf, boolean nurImDunkeln,
              String beschreibung) {
        this.id = id;
        this.anzeigename = anzeigename;
        this.standardLeben = standardLeben;
        this.luftBedarf = luftBedarf;
        this.nurImDunkeln = nurImDunkeln;
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

    /** Benötigte freie Blöcke über dem Boden zum Spawnen. */
    public int getLuftBedarf() {
        return luftBedarf;
    }

    public boolean istNurImDunkeln() {
        return nurImDunkeln;
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
