package de.sondertnt.struktur;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Verwaltet alle Strukturen, die Loot-Tabelle und das Bauen. */
public class StrukturManager {

    private final Plugin plugin;
    private final Map<String, Struktur> strukturen = new LinkedHashMap<String, Struktur>();
    private LootTabelle loot;

    public StrukturManager(Plugin plugin) {
        this.plugin = plugin;
        registrieren(new Turm());
        registrieren(new Ruine());
        registrieren(new Schatzhaus());
        registrieren(new Festung());
        lootNeuLaden();
    }

    private void registrieren(Struktur struktur) {
        strukturen.put(struktur.getId(), struktur);
    }

    /** Liest die Loot-Tabelle neu aus der config.yml und meldet ungültige Einträge. */
    public void lootNeuLaden() {
        loot = new LootTabelle(plugin.getConfig().getStringList("strukturen.loot"));
        for (String fehler : loot.getFehler()) {
            plugin.getLogger().warning("Ungültiger Loot-Eintrag in config.yml: " + fehler);
        }
    }

    public Struktur get(String id) {
        return id == null ? null : strukturen.get(id.toLowerCase());
    }

    public Collection<Struktur> alle() {
        return Collections.unmodifiableCollection(strukturen.values());
    }

    public List<String> alleIds() {
        return new ArrayList<String>(strukturen.keySet());
    }

    /** Füllt eine Truhe mit Zufallsloot. */
    public void truheFuellen(Block block, Random random) {
        BlockState zustand = block.getState();
        if (zustand instanceof Chest && loot != null) {
            loot.fuellen(((Chest) zustand).getInventory(), random);
        }
    }

    /**
     * Baut eine Struktur an einer Welt-Position.
     *
     * @param ox kleinste X-Koordinate der Grundfläche
     * @param oy Bodenebene (lokal y = 0)
     * @param oz kleinste Z-Koordinate der Grundfläche
     */
    public void errichten(Struktur struktur, World welt, int ox, int oy, int oz, int rotation, Random random) {
        Bauplan plan = new Bauplan(welt, ox, oy, oz, struktur.getBreite(), struktur.getTiefe(), rotation);
        struktur.errichten(plan, random, this);
    }

    /**
     * Baut eine Struktur vor dem Spieler, mit dem Eingang zum Spieler hin.
     *
     * @return Mitte der Grundfläche oder {@code null}, wenn die Höhe nicht passt
     */
    public Location vorSpielerBauen(Player spieler, Struktur struktur) {
        Location pos = spieler.getLocation();
        World welt = pos.getWorld();
        BlockFace blick = blickrichtung(pos.getYaw());
        int rotation = Bauplan.rotationFuerVorderseite(blick.getOppositeFace());

        int gx = Bauplan.groesseX(struktur.getBreite(), struktur.getTiefe(), rotation);
        int gz = Bauplan.groesseZ(struktur.getBreite(), struktur.getTiefe(), rotation);
        int abstand = Math.max(gx, gz) / 2 + 3;

        int mitteX = pos.getBlockX() + blick.getModX() * abstand;
        int mitteZ = pos.getBlockZ() + blick.getModZ() * abstand;
        int oy = pos.getBlockY() - 1; // Block, auf dem der Spieler steht
        if (oy < 1 || oy + struktur.getHoehe() >= welt.getMaxHeight()) {
            return null;
        }
        int ox = mitteX - gx / 2;
        int oz = mitteZ - gz / 2;

        errichten(struktur, welt, ox, oy, oz, rotation, new Random());
        return new Location(welt, mitteX, oy + 1, mitteZ);
    }

    /** Wandelt den Yaw-Winkel in eine Himmelsrichtung um (0 = Süden, 90 = Westen). */
    private static BlockFace blickrichtung(float yaw) {
        float w = ((yaw % 360.0F) + 360.0F) % 360.0F;
        if (w >= 45.0F && w < 135.0F) {
            return BlockFace.WEST;
        }
        if (w >= 135.0F && w < 225.0F) {
            return BlockFace.NORTH;
        }
        if (w >= 225.0F && w < 315.0F) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }
}
