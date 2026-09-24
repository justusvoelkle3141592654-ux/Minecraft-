package de.backrooms.spieler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Speichert pro Spieler den Rückkehr-Ort (wo /start benutzt wurde),
 * besiegte Gegner und Siege in plugins/Backrooms/spieler.yml.
 */
public class SpielerDaten {

    private final Plugin plugin;
    private final File datei;
    private final YamlConfiguration daten;
    private boolean geaendert;

    public SpielerDaten(Plugin plugin) {
        this.plugin = plugin;
        this.datei = new File(plugin.getDataFolder(), "spieler.yml");
        this.daten = YamlConfiguration.loadConfiguration(datei);
    }

    private String pfad(UUID uuid) {
        return "spieler." + uuid.toString();
    }

    public void setRueckkehr(UUID uuid, Location ort) {
        String p = pfad(uuid) + ".rueckkehr";
        daten.set(p + ".welt", ort.getWorld().getName());
        daten.set(p + ".x", ort.getX());
        daten.set(p + ".y", ort.getY());
        daten.set(p + ".z", ort.getZ());
        daten.set(p + ".yaw", (double) ort.getYaw());
        daten.set(p + ".pitch", (double) ort.getPitch());
        geaendert = true;
        speichern();
    }

    /** @return der gespeicherte Rückkehr-Ort oder {@code null}, wenn die Welt fehlt. */
    public Location getRueckkehr(UUID uuid) {
        ConfigurationSection abschnitt = daten.getConfigurationSection(pfad(uuid) + ".rueckkehr");
        if (abschnitt == null) {
            return null;
        }
        World welt = plugin.getServer().getWorld(abschnitt.getString("welt", ""));
        if (welt == null) {
            return null;
        }
        return new Location(welt, abschnitt.getDouble("x"), abschnitt.getDouble("y"), abschnitt.getDouble("z"),
                (float) abschnitt.getDouble("yaw"), (float) abschnitt.getDouble("pitch"));
    }

    public void entferneRueckkehr(UUID uuid) {
        daten.set(pfad(uuid) + ".rueckkehr", null);
        geaendert = true;
        speichern();
    }

    public int getBesiegt(UUID uuid) {
        return daten.getInt(pfad(uuid) + ".besiegt", 0);
    }

    public void besiegtErhoehen(UUID uuid) {
        daten.set(pfad(uuid) + ".besiegt", getBesiegt(uuid) + 1);
        geaendert = true;
    }

    public int getSiege(UUID uuid) {
        return daten.getInt(pfad(uuid) + ".siege", 0);
    }

    public void siegErhoehen(UUID uuid) {
        daten.set(pfad(uuid) + ".siege", getSiege(uuid) + 1);
        geaendert = true;
        speichern();
    }

    /** Schreibt die Datei, falls sich etwas geändert hat. */
    public void speichern() {
        if (!geaendert) {
            return;
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Konnte den Plugin-Ordner nicht anlegen.");
            }
            daten.save(datei);
            geaendert = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte spieler.yml nicht speichern: " + ex.getMessage());
        }
    }
}
