package de.backrooms.spieler;

import de.backrooms.BackroomsPlugin;
import de.backrooms.welt.BackroomsGenerator;
import de.backrooms.welt.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ausgänge, Tod und Wiedereinstieg im aktuellen Level. */
public class SpielerListener implements Listener {

    private final BackroomsPlugin plugin;
    /** Level, in dem ein Spieler gestorben ist (für den Respawn). */
    private final Map<UUID, Level> gestorbenIn = new HashMap<UUID, Level>();

    public SpielerListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void beimBewegen(PlayerMoveEvent event) {
        Location von = event.getFrom();
        Location nach = event.getTo();
        if (von.getBlockX() == nach.getBlockX() && von.getBlockY() == nach.getBlockY()
                && von.getBlockZ() == nach.getBlockZ()) {
            return;
        }
        if (!plugin.getWeltManager().istBackrooms(nach.getWorld())) {
            return;
        }
        Block darunter = nach.getBlock().getRelative(BlockFace.DOWN);
        if (darunter.getType() != BackroomsGenerator.AUSGANG) {
            return;
        }
        Level level = Level.vonHoehe(nach.getY());
        if (level != null && !level.istBoss()) {
            plugin.getSpielablauf().ausgangErreicht(event.getPlayer(), level);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void beimTod(PlayerDeathEvent event) {
        Player spieler = event.getEntity();
        Level level = plugin.getWeltManager().levelVon(spieler.getLocation());
        if (!plugin.getWeltManager().istBackrooms(spieler.getWorld())) {
            return;
        }
        // Items und Level behalten, nichts fällt auf den Boden
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        gestorbenIn.put(spieler.getUniqueId(), level == null ? Level.LEVEL_0 : level);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void beimRespawn(PlayerRespawnEvent event) {
        final Player spieler = event.getPlayer();
        final Level level = gestorbenIn.remove(spieler.getUniqueId());
        if (level == null || plugin.getWeltManager().getWelt() == null) {
            return;
        }
        event.setRespawnLocation(level.start(plugin.getWeltManager().getWelt()));
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!spieler.isOnline()) {
                    return;
                }
                spieler.sendTitle(level.getFarbe() + level.getName(), ChatColor.GRAY + "Versuch es noch einmal ...");
                if (level.istBoss()) {
                    plugin.getBossKampf().spielerBetritt();
                }
            }
        }, 5L);
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        plugin.getAnzeige().entfernen(event.getPlayer());
        plugin.getSpielerDaten().speichern();
    }
}
