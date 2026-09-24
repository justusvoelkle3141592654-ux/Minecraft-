package de.backrooms.spieler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seitenleiste (Scoreboard) mit Level, Entfernung zum Ausgang und
 * besiegten Gegnern. Jeder Spieler in den Backrooms bekommt ein eigenes
 * Scoreboard; beim Verlassen wird das Haupt-Scoreboard wiederhergestellt.
 */
public class Anzeige {

    private static final String TITEL = ChatColor.GOLD + "" + ChatColor.BOLD + "BACKROOMS";

    private final Plugin plugin;
    private final Map<UUID, Scoreboard> tafeln = new HashMap<UUID, Scoreboard>();
    private final Map<UUID, List<String>> zeilen = new HashMap<UUID, List<String>>();

    public Anzeige(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Setzt die Zeilen (von oben nach unten, max. 15, je max. 16 Zeichen). */
    public void setzen(Player spieler, List<String> neueZeilen) {
        UUID uuid = spieler.getUniqueId();
        Scoreboard tafel = tafeln.get(uuid);
        Objective ziel;
        if (tafel == null) {
            tafel = plugin.getServer().getScoreboardManager().getNewScoreboard();
            ziel = tafel.registerNewObjective("backrooms", "dummy");
            ziel.setDisplayName(TITEL);
            ziel.setDisplaySlot(DisplaySlot.SIDEBAR);
            tafeln.put(uuid, tafel);
        } else {
            ziel = tafel.getObjective("backrooms");
        }
        if (spieler.getScoreboard() != tafel) {
            spieler.setScoreboard(tafel);
        }

        // Zeilen eindeutig machen (gleiche Texte, z. B. Leerzeilen)
        List<String> eindeutig = new ArrayList<String>();
        Set<String> gesehen = new HashSet<String>();
        int zaehler = 0;
        for (String zeile : neueZeilen) {
            String text = zeile.length() > 16 ? zeile.substring(0, 16) : zeile;
            while (!gesehen.add(text)) {
                text = text + ChatColor.values()[zaehler++ % 16];
            }
            eindeutig.add(text);
        }

        List<String> alt = zeilen.get(uuid);
        if (alt != null) {
            for (String zeile : alt) {
                if (!eindeutig.contains(zeile)) {
                    tafel.resetScores(zeile);
                }
            }
        }
        int punkte = eindeutig.size();
        for (String zeile : eindeutig) {
            ziel.getScore(zeile).setScore(punkte--);
        }
        zeilen.put(uuid, eindeutig);
    }

    public boolean hatAnzeige(Player spieler) {
        return tafeln.containsKey(spieler.getUniqueId());
    }

    public void entfernen(Player spieler) {
        UUID uuid = spieler.getUniqueId();
        Scoreboard tafel = tafeln.remove(uuid);
        zeilen.remove(uuid);
        if (tafel != null && spieler.getScoreboard() == tafel) {
            spieler.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
    }
}
