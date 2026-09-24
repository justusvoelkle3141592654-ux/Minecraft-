package de.backrooms.items;

import de.backrooms.BackroomsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Wirkungen der Backrooms-Items. */
public class ItemListener implements Listener {

    private final BackroomsPlugin plugin;

    public ItemListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void beimEssen(PlayerItemConsumeEvent event) {
        BackroomsItem item = BackroomsItem.von(event.getItem());
        if (item == null) {
            return;
        }
        Player spieler = event.getPlayer();
        switch (item) {
            case MANDELWASSER:
                spieler.setHealth(Math.min(spieler.getMaxHealth(), spieler.getHealth() + 8.0));
                spieler.removePotionEffect(PotionEffectType.BLINDNESS);
                spieler.removePotionEffect(PotionEffectType.CONFUSION);
                spieler.removePotionEffect(PotionEffectType.POISON);
                spieler.removePotionEffect(PotionEffectType.WITHER);
                spieler.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 0), true);
                spieler.sendMessage(ChatColor.AQUA + "Das Mandelwasser beruhigt dich.");
                break;
            case ENERGIERIEGEL:
                spieler.setFoodLevel(Math.min(20, spieler.getFoodLevel() + 6));
                spieler.setSaturation(Math.min(20F, spieler.getSaturation() + 6F));
                spieler.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 0), true);
                break;
            default:
                break;
        }
    }

    /** Backrooms-Items (z. B. die Taschenlampe = Fackel) lassen sich nicht platzieren. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimPlatzieren(BlockPlaceEvent event) {
        if (BackroomsItem.von(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void beimKlicken(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (BackroomsItem.von(event.getItem()) == BackroomsItem.KOMPASS) {
            event.getPlayer().sendMessage(plugin.getSpielablauf().kompassInfo(event.getPlayer()));
        }
    }
}
