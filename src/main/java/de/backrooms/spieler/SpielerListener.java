package de.backrooms.spieler;

import de.backrooms.BackroomsPlugin;
import de.backrooms.welt.BackroomsGenerator;
import de.backrooms.welt.Level;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ausgangs-Knöpfe, Tod und Wiedereinstieg im aktuellen Level. */
public class SpielerListener implements Listener {

    private final BackroomsPlugin plugin;
    /** Level, in dem ein Spieler gestorben ist (für den Respawn). */
    private final Map<UUID, Level> gestorbenIn = new HashMap<UUID, Level>();

    public SpielerListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Knopf an einer Ausgangs-Säule gedrückt -> nächstes Level. */
    @EventHandler
    public void beimKnopf(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block knopf = event.getClickedBlock();
        if (knopf == null || knopf.getType() != BackroomsGenerator.AUSGANGS_KNOPF
                || !plugin.getWeltManager().istBackrooms(knopf.getWorld())) {
            return;
        }
        boolean anSaeule = false;
        for (BlockFace seite : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (knopf.getRelative(seite).getType() == BackroomsGenerator.AUSGANG) {
                anSaeule = true;
                break;
            }
        }
        Level level = Level.vonHoehe(knopf.getY());
        if (anSaeule && level != null && !level.istBoss()) {
            plugin.getSpielablauf().ausgangErreicht(event.getPlayer(), level);
        }
    }

    @EventHandler
    public void beimBetreten(PlayerJoinEvent event) {
        if (plugin.getWeltManager().istBackrooms(event.getPlayer().getWorld())) {
            plugin.getSpielablauf().ressourcenpaketSenden(event.getPlayer());
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
                spieler.sendTitle(level.getFarbe() + level.getName(), ChatColor.GRAY + "Versuch es noch einmal ...",
                        10, 40, 10);
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
