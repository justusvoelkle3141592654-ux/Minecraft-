package de.backrooms.welt;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Die Ebenen der Backrooms. Alle Level liegen in derselben Welt übereinander
 * (verschiedene Höhen). Decken und Böden sind unzerstörbar, daher kommt man
 * nur über die Ausgänge ins nächste Level.
 */
public enum Level {

    //          Nummer, Name,          Untertitel,     Zelle, Boden-Y, Lufthöhe, Ausgangs-Chance je Zelle
    LEVEL_0(0, "Level 0", "Die Lobby", 8, 20, 4, 1.0 / 120.0, ChatColor.YELLOW),
    LEVEL_1(1, "Level 1", "Das Parkhaus", 12, 50, 6, 1.0 / 70.0, ChatColor.AQUA),
    LEVEL_2(2, "Level 2", "Die Rohre", 6, 80, 3, 1.0 / 150.0, ChatColor.GOLD),
    BOSS(3, "Boss-Arena", "Der Warden", 0, 110, 12, 0.0, ChatColor.DARK_AQUA);

    /** Halbe Kantenlänge der Boss-Arena (Arena reicht von -20 bis +20). */
    public static final int ARENA_RADIUS = 20;

    private final int nummer;
    private final String name;
    private final String untertitel;
    private final int zelle;
    private final int bodenY;
    private final int luftHoehe;
    private final double ausgangsChance;
    private final ChatColor farbe;

    Level(int nummer, String name, String untertitel, int zelle, int bodenY, int luftHoehe,
          double ausgangsChance, ChatColor farbe) {
        this.nummer = nummer;
        this.name = name;
        this.untertitel = untertitel;
        this.zelle = zelle;
        this.bodenY = bodenY;
        this.luftHoehe = luftHoehe;
        this.ausgangsChance = ausgangsChance;
        this.farbe = farbe;
    }

    public int getNummer() {
        return nummer;
    }

    public String getName() {
        return name;
    }

    public String getUntertitel() {
        return untertitel;
    }

    public ChatColor getFarbe() {
        return farbe;
    }

    /** Kantenlänge einer Labyrinth-Zelle (Wandabstand). */
    public int getZelle() {
        return zelle;
    }

    public int getBodenY() {
        return bodenY;
    }

    public int getLuftHoehe() {
        return luftHoehe;
    }

    public int getDeckeY() {
        return bodenY + luftHoehe + 1;
    }

    public double getAusgangsChance() {
        return ausgangsChance;
    }

    public boolean istBoss() {
        return this == BOSS;
    }

    /** Das Level, in das der Ausgang führt ({@code null} nach dem Boss). */
    public Level naechstes() {
        switch (this) {
            case LEVEL_0:
                return LEVEL_1;
            case LEVEL_1:
                return LEVEL_2;
            case LEVEL_2:
                return BOSS;
            default:
                return null;
        }
    }

    /** Startpunkt: Mitte der Zelle (0,0), diese Zelle ist immer frei. */
    public Location start(World welt) {
        if (this == BOSS) {
            return new Location(welt, 0.5, bodenY + 1, ARENA_RADIUS - 3.5, 180.0F, 0.0F);
        }
        double mitte = zelle / 2 + 0.5;
        return new Location(welt, mitte, bodenY + 1, mitte);
    }

    /** Bestimmt das Level anhand der Höhe ({@code null} = zwischen den Leveln). */
    public static Level vonHoehe(double y) {
        for (Level level : values()) {
            if (y >= level.bodenY - 2 && y <= level.getDeckeY() + 2) {
                return level;
            }
        }
        return null;
    }

    /** Sucht ein Level über "0", "1", "2", "boss" oder den Enum-Namen. */
    public static Level vonText(String text) {
        if (text == null) {
            return null;
        }
        for (Level level : values()) {
            if (text.equalsIgnoreCase(level.name()) || text.equalsIgnoreCase(String.valueOf(level.nummer))) {
                return level;
            }
        }
        return text.equalsIgnoreCase("boss") ? BOSS : null;
    }
}
