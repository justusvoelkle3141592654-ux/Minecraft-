package de.sondertnt.tnt;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * Erzeugt gezündetes Sonder-TNT und merkt sich den Typ als Entity-Metadaten.
 */
public class TntZuender {

    public static final String METADATEN_SCHLUESSEL = "sondertnt_typ";

    private final Plugin plugin;

    public TntZuender(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawnt ein gezündetes TNT an der Position.
     *
     * @param position Mitte des Blocks (x + 0.5, y, z + 0.5), wie bei Vanilla-TNT
     */
    public TNTPrimed zuenden(Location position, TntTyp typ) {
        TNTPrimed tnt = position.getWorld().spawn(position, TNTPrimed.class);
        tnt.setFuseTicks(Math.max(1, plugin.getConfig().getInt("tnt.zuendzeit", 80)));
        tnt.setMetadata(METADATEN_SCHLUESSEL, new FixedMetadataValue(plugin, typ.name()));
        position.getWorld().playSound(position, Sound.FUSE, 1.0F, 1.0F);
        return tnt;
    }

    /** @return der gemerkte Typ oder {@code null} bei normalem TNT. */
    public TntTyp typVon(Entity entity) {
        if (!(entity instanceof TNTPrimed) || !entity.hasMetadata(METADATEN_SCHLUESSEL)) {
            return null;
        }
        for (MetadataValue wert : entity.getMetadata(METADATEN_SCHLUESSEL)) {
            Plugin besitzer = wert.getOwningPlugin();
            // Vergleich über den Namen, damit es auch nach /reload funktioniert
            if (besitzer != null && besitzer.getName().equals(plugin.getName())) {
                return TntTyp.vonId(wert.asString());
            }
        }
        return null;
    }
}
