package de.backrooms.items;

import de.backrooms.BackroomsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Wirkungen der Backrooms-Items: Waffen, Heilung, Taschenlampe. */
public class ItemListener implements Listener {

    private static final String META_SCHUSS = "backrooms_schuss";

    private final BackroomsPlugin plugin;
    private final Map<String, Long> abklingzeit = new HashMap<String, Long>();
    /** Wo die Taschenlampe gerade (nur für diesen Spieler) einen Leuchtblock zeigt. */
    private final Map<UUID, Location> lichtfleck = new HashMap<UUID, Location>();
    /** Verhindert, dass Explosionsschaden als Schwertschlag gewertet wird. */
    private boolean explosionsSchaden;

    public ItemListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    private double wert(String pfad, double standard) {
        return plugin.getConfig().getDouble("waffen." + pfad, standard);
    }

    private static BackroomsItem inHand(Player p) {
        return BackroomsItem.von(p.getInventory().getItemInMainHand());
    }

    /** @return true, wenn die Aktion erlaubt ist (und startet die Abklingzeit). */
    private boolean bereit(Player p, String aktion, double sekunden) {
        String schluessel = p.getUniqueId() + ":" + aktion;
        long jetzt = System.currentTimeMillis();
        Long frei = abklingzeit.get(schluessel);
        if (frei != null && frei > jetzt) {
            return false;
        }
        abklingzeit.put(schluessel, jetzt + (long) (sekunden * 1000));
        return true;
    }

    private static void einsVerbrauchen(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            p.getInventory().setItemInMainHand(null);
        }
    }

    // ------------------------------------------------------------ Rechtsklick

    @EventHandler(priority = EventPriority.HIGH)
    public void beimKlicken(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = event.getPlayer();
        BackroomsItem item = inHand(p);
        if (item == null || item == BackroomsItem.ENERGIERIEGEL) {
            return; // Energieriegel wird normal gegessen
        }
        // Knöpfe (Ausgang) sollen weiterhin funktionieren
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.STONE_BUTTON) {
            return;
        }
        event.setCancelled(true); // z. B. kein Umgraben mit der "Hacke"

        switch (item) {
            case PISTOLE:
                if (bereit(p, "pistole", wert("pistole-nachladen", 0.35))) {
                    schiessen(p, "PISTOLE", 3.0, false);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1.0F, 1.6F);
                }
                break;
            case BAZOOKA:
                if (bereit(p, "bazooka", wert("bazooka-nachladen", 2.0))) {
                    rakete(p);
                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.6F, 0.6F);
                }
                break;
            case GRANATE:
                if (bereit(p, "granate", 0.5)) {
                    schiessen(p, "GRANATE", 1.1, true);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0F, 0.6F);
                    einsVerbrauchen(p);
                }
                break;
            case MEDKIT:
                if (p.getHealth() < p.getMaxHealth()) {
                    p.setHealth(p.getMaxHealth());
                    p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 6, 0.4, 0.3, 0.4, 0);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.8F);
                    einsVerbrauchen(p);
                } else {
                    p.sendMessage(ChatColor.GRAY + "Du bist schon voll geheilt.");
                }
                break;
            case MANDELWASSER:
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8.0));
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.CONFUSION);
                p.removePotionEffect(PotionEffectType.POISON);
                p.removePotionEffect(PotionEffectType.WITHER);
                p.removePotionEffect(PotionEffectType.SLOW);
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);
                einsVerbrauchen(p);
                break;
            case ADRENALIN:
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1), true);
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 20, 1), true);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0F, 1.5F);
                einsVerbrauchen(p);
                break;
            case KOMPASS:
                p.sendMessage(plugin.getSpielablauf().kompassInfo(p));
                break;
            default:
                break;
        }
    }

    private Snowball schiessen(Player p, String art, double tempo, boolean schwerkraft) {
        Vector richtung = p.getLocation().getDirection().normalize();
        Snowball geschoss = p.launchProjectile(Snowball.class, richtung.multiply(tempo));
        geschoss.setGravity(schwerkraft);
        geschoss.setMetadata(META_SCHUSS, new FixedMetadataValue(plugin, art));
        Location muendung = p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.8));
        p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, muendung, 4, 0.05, 0.05, 0.05, 0.01);
        entfernenNach(geschoss, schwerkraft ? 100 : 40);
        return geschoss;
    }

    private void rakete(Player p) {
        final Snowball rakete = schiessen(p, "RAKETE", 1.4, false);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 1.2F, 0.5F);
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!rakete.isValid() || ++ticks > 60) {
                    if (rakete.isValid()) {
                        explodieren(rakete.getLocation(), rakete.getShooter(), "bazooka");
                        rakete.remove();
                    }
                    cancel();
                    return;
                }
                Location ort = rakete.getLocation();
                ort.getWorld().spawnParticle(Particle.FLAME, ort, 3, 0.05, 0.05, 0.05, 0.01);
                ort.getWorld().spawnParticle(Particle.SMOKE_LARGE, ort, 2, 0.05, 0.05, 0.05, 0.01);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void entfernenNach(final Entity entity, long ticks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    entity.remove();
                }
            }
        }, ticks);
    }

    /** Art eines Geschosses des Plugins ("PISTOLE", "RAKETE", "GRANATE") oder {@code null}. */
    public static String schussArt(Entity entity) {
        return art(entity);
    }

    private static String art(Entity entity) {
        if (entity == null || !entity.hasMetadata(META_SCHUSS)) {
            return null;
        }
        for (MetadataValue wert : entity.getMetadata(META_SCHUSS)) {
            return wert.asString();
        }
        return null;
    }

    @EventHandler
    public void beimTreffer(ProjectileHitEvent event) {
        String art = art(event.getEntity());
        if ("RAKETE".equals(art)) {
            explodieren(event.getEntity().getLocation(), event.getEntity().getShooter(), "bazooka");
            event.getEntity().remove();
        } else if ("GRANATE".equals(art)) {
            explodieren(event.getEntity().getLocation(), event.getEntity().getShooter(), "granate");
            event.getEntity().remove();
        } else if ("PISTOLE".equals(art)) {
            Location ort = event.getEntity().getLocation();
            ort.getWorld().spawnParticle(Particle.CRIT, ort, 6, 0.1, 0.1, 0.1, 0.05);
        }
    }

    /** Explosion ohne Blockschaden: nur Effekte und Schaden an Gegnern. */
    private void explodieren(Location ort, ProjectileSource schuetze, String waffe) {
        World welt = ort.getWorld();
        double radius = wert(waffe + "-radius", waffe.equals("bazooka") ? 4.0 : 3.5);
        double schaden = wert(waffe + "-schaden", waffe.equals("bazooka") ? 25.0 : 15.0);
        welt.spawnParticle(Particle.EXPLOSION_HUGE, ort, 1, 0, 0, 0, 0);
        welt.spawnParticle(Particle.FLAME, ort, 30, radius / 3, radius / 3, radius / 3, 0.05);
        welt.playSound(ort, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
        Entity verursacher = schuetze instanceof Entity ? (Entity) schuetze : null;
        explosionsSchaden = true;
        try {
            for (Entity e : welt.getNearbyEntities(ort, radius, radius, radius)) {
                if (!(e instanceof LivingEntity) || e instanceof ArmorStand || e.equals(verursacher)) {
                    continue;
                }
                if (e instanceof Player && !welt.getPVP()) {
                    continue;
                }
                double abstand = e.getLocation().distance(ort);
                if (abstand > radius) {
                    continue;
                }
                double anteil = 1.0 - (abstand / radius) * 0.5;
                if (verursacher != null) {
                    ((LivingEntity) e).damage(schaden * anteil, verursacher);
                } else {
                    ((LivingEntity) e).damage(schaden * anteil);
                }
                Vector stoss = e.getLocation().toVector().subtract(ort.toVector());
                if (stoss.lengthSquared() > 0.01) {
                    e.setVelocity(stoss.normalize().multiply(0.6).setY(0.4));
                }
            }
        } finally {
            explosionsSchaden = false;
        }
    }

    // ------------------------------------------------------------ Schaden

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchaden(EntityDamageByEntityEvent event) {
        Entity verursacher = event.getDamager();
        String art = art(verursacher);
        if (art != null) {
            if (art.equals("PISTOLE")) {
                event.setDamage(wert("pistole-schaden", 7.0));
            } else {
                event.setCancelled(true); // Rakete/Granate: Schaden kommt von der Explosion
            }
            return;
        }
        if (explosionsSchaden || !(verursacher instanceof Player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }
        if (inHand((Player) verursacher) == BackroomsItem.KLINGE) {
            event.setDamage(wert("klingen-schaden", 30.0));
            Location ort = event.getEntity().getLocation().add(0, 1, 0);
            ort.getWorld().spawnParticle(Particle.SWEEP_ATTACK, ort, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------ Essen / Platzieren

    @EventHandler(ignoreCancelled = true)
    public void beimEssen(PlayerItemConsumeEvent event) {
        if (BackroomsItem.von(event.getItem()) == BackroomsItem.ENERGIERIEGEL) {
            Player p = event.getPlayer();
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + 6));
            p.setSaturation(Math.min(20F, p.getSaturation() + 6F));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 0), true);
        }
    }

    /** Backrooms-Items lassen sich nicht als Block platzieren. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void beimPlatzieren(BlockPlaceEvent event) {
        if (BackroomsItem.von(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ Taschenlampe

    /**
     * Wird alle 3 Ticks aufgerufen. Der Block, auf den der Spieler mit der
     * Taschenlampe schaut, wird nur für ihn als Seelaterne angezeigt. Dadurch
     * berechnet sein Client dort echtes Licht (Lichtkegel).
     */
    @SuppressWarnings("deprecation")
    public void taschenlampenTick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location alt = lichtfleck.get(p.getUniqueId());
            Location neu = null;
            if (inHand(p) == BackroomsItem.TASCHENLAMPE) {
                Block ziel = p.getTargetBlock((java.util.Set<Material>) null, 24);
                if (ziel != null && ziel.getType().isOccluding()) {
                    neu = ziel.getLocation();
                    Location auge = p.getEyeLocation();
                    Vector schritt = neu.clone().add(0.5, 0.5, 0.5).toVector().subtract(auge.toVector());
                    double laenge = schritt.length();
                    schritt.normalize();
                    for (double d = 1.5; d < laenge; d += 2.5) {
                        p.spawnParticle(Particle.END_ROD, auge.clone().add(schritt.clone().multiply(d)), 1, 0, 0, 0, 0);
                    }
                }
            }
            if (alt != null && (neu == null || !alt.equals(neu))) {
                if (alt.getWorld().equals(p.getWorld())) {
                    Block b = alt.getBlock();
                    p.sendBlockChange(alt, b.getType(), b.getData());
                }
                lichtfleck.remove(p.getUniqueId());
            }
            if (neu != null && (alt == null || !alt.equals(neu))) {
                p.sendBlockChange(neu, Material.SEA_LANTERN, (byte) 0);
                lichtfleck.put(p.getUniqueId(), neu);
            }
        }
        // Einträge von Spielern, die nicht mehr online sind
        Iterator<UUID> it = lichtfleck.keySet().iterator();
        while (it.hasNext()) {
            if (plugin.getServer().getPlayer(it.next()) == null) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        lichtfleck.remove(event.getPlayer().getUniqueId());
        Iterator<String> it = abklingzeit.keySet().iterator();
        String praefix = event.getPlayer().getUniqueId() + ":";
        while (it.hasNext()) {
            if (it.next().startsWith(praefix)) {
                it.remove();
            }
        }
    }
}
