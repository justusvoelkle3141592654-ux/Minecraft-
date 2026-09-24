package de.backrooms.tnt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;

public class SonderTntListener implements Listener {

    private final Plugin plugin;
    private final TntZuender zuender;
    private final TntEffekte effekte;

    public SonderTntListener(Plugin plugin, TntZuender zuender, TntEffekte effekte) {
        this.plugin = plugin;
        this.zuender = zuender;
        this.effekte = effekte;
    }

    /**
     * Sonder-TNT wird beim Platzieren sofort gezündet. Das Ersetzen des Blocks
     * passiert einen Tick später, damit Schutz-Plugins das Platzieren vorher
     * noch abbrechen können und Vanilla das Item normal verbraucht.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimPlatzieren(BlockPlaceEvent event) {
        final Block block = event.getBlockPlaced();
        if (block.getType() != Material.TNT) {
            return;
        }
        final TntTyp typ = SonderTntItems.typVon(event.getItemInHand());
        if (typ == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (block.getType() != Material.TNT) {
                    return;
                }
                block.setType(Material.AIR);
                zuender.zuenden(block.getLocation().add(0.5, 0.0, 0.5), typ);
            }
        });
    }

    /** Werfer (Dispenser) sollen Sonder-TNT ebenfalls mit dem richtigen Typ zünden. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimWerfen(BlockDispenseEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER) {
            return;
        }
        final TntTyp typ = SonderTntItems.typVon(event.getItem());
        if (typ == null) {
            return;
        }
        // Abbrechen: Vanilla legt das Item dann zurück in den Werfer.
        event.setCancelled(true);
        final ItemStack einzeln = event.getItem().clone();
        einzeln.setAmount(1);

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (block.getType() != Material.DISPENSER) {
                    return;
                }
                BlockState zustand = block.getState();
                if (!(zustand instanceof Dispenser)) {
                    return;
                }
                MaterialData daten = zustand.getData();
                if (!(daten instanceof org.bukkit.material.Dispenser)) {
                    return;
                }
                Inventory inventar = ((Dispenser) zustand).getInventory();
                if (!inventar.removeItem(einzeln).isEmpty()) {
                    return; // Item ist nicht mehr im Werfer
                }
                BlockFace richtung = ((org.bukkit.material.Dispenser) daten).getFacing();
                Location ziel = block.getRelative(richtung).getLocation().add(0.5, 0.0, 0.5);
                zuender.zuenden(ziel, typ);
            }
        });
    }

    /** Hier wird die passende Explosion des Sonder-TNT ausgelöst. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimExplodieren(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        TntTyp typ = zuender.typVon(entity);
        if (typ == null) {
            return;
        }
        Location mitte = entity.getLocation();

        switch (typ) {
            case MEGA:
                event.setRadius(effekte.staerke(typ));
                event.setFire(false);
                break;
            case FEUER:
                event.setRadius(effekte.staerke(typ));
                event.setFire(true);
                effekte.feuer(mitte);
                break;
            case BLITZ:
                event.setRadius(effekte.staerke(typ));
                event.setFire(false);
                effekte.blitz(mitte);
                break;
            case LIFT:
                // Keine echte Explosion -> keine Blockschäden
                event.setCancelled(true);
                effekte.lift(mitte);
                break;
            case CLUSTER:
                event.setRadius(effekte.staerke(typ));
                event.setFire(false);
                effekte.cluster(mitte);
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchaden(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && effekte.fallschutzVerbrauchen(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
