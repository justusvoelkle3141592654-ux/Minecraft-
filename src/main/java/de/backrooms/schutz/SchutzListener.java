package de.backrooms.schutz;

import de.backrooms.welt.WeltManager;
import de.backrooms.tnt.SonderTntItems;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Die Backrooms sind unzerstörbar: kein Abbauen, kein Bauen, keine
 * Explosionsschäden an Blöcken, kein Verbrennen. Ausnahmen: Sonder-TNT darf
 * platziert werden (es zündet sofort) und Spieler mit "backrooms.bauen" im
 * Kreativmodus dürfen bauen.
 */
public class SchutzListener implements Listener {

    public static final String BAU_RECHT = "backrooms.bauen";

    private final WeltManager weltManager;

    public SchutzListener(WeltManager weltManager) {
        this.weltManager = weltManager;
    }

    private static boolean darfBauen(Player spieler) {
        return spieler.getGameMode() == GameMode.CREATIVE && spieler.hasPermission(BAU_RECHT);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent event) {
        if (weltManager.istBackrooms(event.getBlock().getWorld()) && !darfBauen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimBauen(BlockPlaceEvent event) {
        if (!weltManager.istBackrooms(event.getBlock().getWorld()) || darfBauen(event.getPlayer())) {
            return;
        }
        if (!SonderTntItems.istSonderTnt(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimEimer(PlayerBucketEmptyEvent event) {
        if (weltManager.istBackrooms(event.getBlockClicked().getWorld()) && !darfBauen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimAufhaengen(HangingPlaceEvent event) {
        if (weltManager.istBackrooms(event.getBlock().getWorld())
                && (event.getPlayer() == null || !darfBauen(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    /** Explosionen verletzen weiterhin, zerstören aber keine Blöcke. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimExplodieren(EntityExplodeEvent event) {
        if (weltManager.istBackrooms(event.getLocation().getWorld())) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimBrennen(BlockBurnEvent event) {
        if (weltManager.istBackrooms(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimBlockAendern(EntityChangeBlockEvent event) {
        if (weltManager.istBackrooms(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    /** Nur die Gegner des Plugins (SpawnReason.CUSTOM) dürfen hier erscheinen. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimSpawnen(CreatureSpawnEvent event) {
        if (weltManager.istBackrooms(event.getLocation().getWorld())
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }
}
