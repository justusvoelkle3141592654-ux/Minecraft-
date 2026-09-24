package de.backrooms.gegner;

import de.backrooms.BackroomsPlugin;
import de.backrooms.items.BackroomsItem;
import de.backrooms.items.ItemListener;
import de.backrooms.welt.Level;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Spawnt, steuert und entfernt die eigenen Backrooms-Gegner.
 * Jeder Gegner = unsichtbarer Vanilla-Mob + Blockmodell aus Rüstungsständern.
 */
public class GegnerManager implements Listener {

    public static final String META_TYP = "backrooms_gegner";
    /** Markiert Rüstungsständer, die zu einem Modell oder Schild des Plugins gehören. */
    public static final String META_TEIL = "backrooms_teil";
    private static final int UNENDLICH = Integer.MAX_VALUE;
    /** Höhe der Blockmitte über den Füßen eines Rüstungsständers. */
    private static final double KOPF_HOEHE = 1.69;

    private final BackroomsPlugin plugin;
    private final Map<UUID, LivingEntity> gegner = new HashMap<UUID, LivingEntity>();
    private final Map<UUID, List<ArmorStand>> modelle = new HashMap<UUID, List<ArmorStand>>();
    /** Rüstungsständer eines Modells -> zugehöriger Gegner (für Treffer auf das Modell). */
    private final Map<UUID, LivingEntity> teilZuGegner = new HashMap<UUID, LivingEntity>();
    private final Random random = new Random();
    private int sekunden;

    public GegnerManager(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ Spawnen

    public LivingEntity spawnen(GegnerTyp typ, Location ort) {
        World welt = ort.getWorld();
        LivingEntity mob;
        switch (typ) {
            case TAPETENKRIECHER:
                mob = welt.spawn(ort, Spider.class);
                break;
            case SCHATTENHUND: {
                Wolf wolf = welt.spawn(ort, Wolf.class);
                wolf.setAngry(true);
                mob = wolf;
                break;
            }
            case PARTYBALLON: {
                Slime slime = welt.spawn(ort, Slime.class);
                slime.setSize(2);
                mob = slime;
                break;
            }
            case WARDEN: {
                IronGolem golem = welt.spawn(ort, IronGolem.class);
                golem.setPlayerCreated(false);
                mob = golem;
                break;
            }
            default: {
                // GRINSER und ROHRGEIST
                Zombie zombie = welt.spawn(ort, Zombie.class);
                zombie.setBaby(false);
                mob = zombie;
                break;
            }
        }

        // Unsichtbar und ohne Vanilla-Ausrüstung: sichtbar ist nur das Blockmodell
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, UNENDLICH, 0, true, false), true);
        EntityEquipment ausruestung = mob.getEquipment();
        if (ausruestung != null) {
            ausruestung.clear();
        }
        mob.setSilent(true);
        mob.setCanPickupItems(false);
        mob.setRemoveWhenFarAway(false);

        double leben = Math.max(1.0, Math.min(1000.0, typ == GegnerTyp.WARDEN
                ? plugin.getConfig().getDouble("boss.leben", typ.getStandardLeben())
                : plugin.getConfig().getDouble("gegner.leben." + typ.getId(), typ.getStandardLeben())));
        AttributeInstance maxLeben = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxLeben != null) {
            maxLeben.setBaseValue(leben);
        }
        mob.setHealth(leben);
        AttributeInstance tempo = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (tempo != null && typ.getTempo() > 0) {
            tempo.setBaseValue(typ.getTempo());
        }
        AttributeInstance folgen = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (folgen != null) {
            folgen.setBaseValue(typ == GegnerTyp.WARDEN ? 40 : 24);
        }

        mob.setCustomName(typ.getAnzeigename());
        mob.setCustomNameVisible(true);
        mob.setMetadata(META_TYP, new FixedMetadataValue(plugin, typ.name()));
        gegner.put(mob.getUniqueId(), mob);
        modellBauen(mob, typ);
        return mob;
    }

    private void modellBauen(LivingEntity mob, GegnerTyp typ) {
        List<ArmorStand> staender = new ArrayList<ArmorStand>();
        for (Modelle.Teil teil : Modelle.fuer(typ)) {
            // Kein Marker: Marker-Ständer werden mit dem Licht an ihren Füßen gezeichnet,
            // die bei tiefen Teilen im Boden liegen (Teile wären dann schwarz).
            ArmorStand stand = standErzeugen(position(mob.getLocation(), teil), false);
            stand.setHelmet(new ItemStack(teil.material, 1, teil.daten));
            staender.add(stand);
            teilZuGegner.put(stand.getUniqueId(), mob);
        }
        modelle.put(mob.getUniqueId(), staender);
    }

    /**
     * Unsichtbarer Rüstungsständer des Plugins.
     *
     * @param marker true = ohne Trefferfläche (für schwebende Schilder)
     */
    public ArmorStand standErzeugen(Location ort, boolean marker) {
        ArmorStand stand = ort.getWorld().spawn(ort, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(marker);
        stand.setBasePlate(false);
        stand.setSilent(true);
        stand.setMetadata(META_TEIL, new FixedMetadataValue(plugin, true));
        return stand;
    }

    private static Location position(Location basis, Modelle.Teil teil) {
        double winkel = Math.toRadians(basis.getYaw());
        // Minecraft: Yaw 0 = Süden (+z). Vorwärts = (-sin, cos), rechts = (-cos, -sin)
        double vx = -Math.sin(winkel);
        double vz = Math.cos(winkel);
        double rx = -Math.cos(winkel);
        double rz = -Math.sin(winkel);
        double x = basis.getX() + rx * teil.rechts + vx * teil.vor;
        double z = basis.getZ() + rz * teil.rechts + vz * teil.vor;
        double y = basis.getY() + teil.hoch - KOPF_HOEHE;
        return new Location(basis.getWorld(), x, y, z, basis.getYaw(), 0F);
    }

    /** Wird jeden Tick aufgerufen: Modelle folgen ihrem Mob. */
    public void modelleBewegen() {
        Iterator<Map.Entry<UUID, List<ArmorStand>>> it = modelle.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<ArmorStand>> eintrag = it.next();
            LivingEntity mob = gegner.get(eintrag.getKey());
            if (mob == null || !mob.isValid() || mob.isDead()) {
                for (ArmorStand stand : eintrag.getValue()) {
                    teilZuGegner.remove(stand.getUniqueId());
                    stand.remove();
                }
                it.remove();
                continue;
            }
            GegnerTyp typ = typVon(mob);
            List<Modelle.Teil> teile = Modelle.fuer(typ);
            List<ArmorStand> staender = eintrag.getValue();
            Location basis = mob.getLocation();
            for (int i = 0; i < staender.size() && i < teile.size(); i++) {
                staender.get(i).teleport(position(basis, teile.get(i)));
            }
        }
    }

    // ------------------------------------------------------------ Abfragen

    public GegnerTyp typVon(Entity entity) {
        if (entity == null || !entity.hasMetadata(META_TYP)) {
            return null;
        }
        for (MetadataValue wert : entity.getMetadata(META_TYP)) {
            if (wert.getOwningPlugin() != null && wert.getOwningPlugin().getName().equals(plugin.getName())) {
                return GegnerTyp.vonId(wert.asString());
            }
        }
        return null;
    }

    public List<LivingEntity> alle(GegnerTyp typ) {
        List<LivingEntity> liste = new ArrayList<LivingEntity>();
        for (LivingEntity mob : gegner.values()) {
            if (mob.isValid() && typVon(mob) == typ) {
                liste.add(mob);
            }
        }
        return liste;
    }

    public void entfernen(LivingEntity mob) {
        List<ArmorStand> staender = modelle.remove(mob.getUniqueId());
        if (staender != null) {
            for (ArmorStand stand : staender) {
                teilZuGegner.remove(stand.getUniqueId());
                stand.remove();
            }
        }
        gegner.remove(mob.getUniqueId());
        mob.remove();
    }

    private static boolean istZiel(Player p) {
        return !p.isDead() && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE);
    }

    /** Die Level liegen übereinander, deshalb zählt nur, wer im selben Level ist. */
    private static boolean gleichesLevel(Location a, Location b) {
        return Level.vonHoehe(a.getY()) == Level.vonHoehe(b.getY());
    }

    private static Player naechstesZiel(LivingEntity mob, double radius) {
        Player beste = null;
        double besteDist = radius * radius;
        for (Player p : mob.getWorld().getPlayers()) {
            if (!istZiel(p) || !gleichesLevel(p.getLocation(), mob.getLocation())) {
                continue;
            }
            double d = p.getLocation().distanceSquared(mob.getLocation());
            if (d <= besteDist) {
                besteDist = d;
                beste = p;
            }
        }
        return beste;
    }

    // ------------------------------------------------------------ Takt (jede Sekunde)

    public void tick() {
        World welt = plugin.getWeltManager().getWelt();
        if (welt == null) {
            return;
        }
        sekunden++;

        for (LivingEntity mob : new ArrayList<LivingEntity>(gegner.values())) {
            if (!mob.isValid()) {
                gegner.remove(mob.getUniqueId());
                continue;
            }
            GegnerTyp typ = typVon(mob);
            if (typ == null) {
                continue;
            }
            if (typ != GegnerTyp.WARDEN && keinSpielerInDerNaehe(mob, 64.0)) {
                entfernen(mob);
                continue;
            }
            zielSetzen(mob, typ);
            geraeusch(mob, typ);
        }

        int intervall = Math.max(1, plugin.getConfig().getInt("gegner.spawn-intervall-sekunden", 8));
        if (plugin.getConfig().getBoolean("gegner.aktiviert", true) && sekunden % intervall == 0) {
            spawnRunde(welt);
        }
        if (sekunden % 30 == 0) {
            fremdeEntfernen();
        }
    }

    private static boolean keinSpielerInDerNaehe(LivingEntity mob, double radius) {
        for (Player p : mob.getWorld().getPlayers()) {
            if (gleichesLevel(p.getLocation(), mob.getLocation())
                    && p.getLocation().distanceSquared(mob.getLocation()) <= radius * radius) {
                return false;
            }
        }
        return true;
    }

    private void zielSetzen(LivingEntity mob, GegnerTyp typ) {
        if (!(mob instanceof Creature)) {
            return; // Slimes suchen sich ihr Ziel selbst
        }
        Creature kreatur = (Creature) mob;
        LivingEntity aktuell = kreatur.getTarget();
        if (aktuell instanceof Player && aktuell.isValid() && istZiel((Player) aktuell)
                && aktuell.getWorld().equals(mob.getWorld()) && gleichesLevel(aktuell.getLocation(), mob.getLocation())
                && aktuell.getLocation().distanceSquared(mob.getLocation()) < 32 * 32) {
            return;
        }
        Player ziel = naechstesZiel(mob, typ == GegnerTyp.WARDEN ? 40.0 : 24.0);
        if (ziel != null) {
            kreatur.setTarget(ziel);
        }
    }

    /** Eigene Geräusche und Effekte (die Mobs selbst sind stumm). */
    private void geraeusch(LivingEntity mob, GegnerTyp typ) {
        if (random.nextInt(8) != 0) {
            return;
        }
        Location ort = mob.getLocation();
        World welt = mob.getWorld();
        switch (typ) {
            case TAPETENKRIECHER:
                welt.playSound(ort, Sound.ENTITY_SPIDER_AMBIENT, 0.6F, 1.6F);
                break;
            case GRINSER:
                welt.playSound(ort, Sound.ENTITY_WITCH_AMBIENT, 0.8F, 0.6F);
                break;
            case SCHATTENHUND:
                welt.playSound(ort, Sound.ENTITY_WOLF_GROWL, 0.8F, 0.7F);
                break;
            case PARTYBALLON:
                welt.playSound(ort, Sound.BLOCK_NOTE_PLING, 0.8F, 0.5F + random.nextFloat());
                welt.spawnParticle(Particle.NOTE, ort.clone().add(0, 2.3, 0), 3, 0.5, 0.3, 0.5, 1);
                break;
            case ROHRGEIST:
                welt.playSound(ort, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.6F, 0.5F);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ Spawn-Runde

    private void spawnRunde(World welt) {
        int maxProSpieler = plugin.getConfig().getInt("gegner.max-pro-spieler", 3);
        int maxGesamt = plugin.getConfig().getInt("gegner.max-gesamt", 40);
        for (Player p : welt.getPlayers()) {
            if (!istZiel(p) || gegner.size() >= maxGesamt) {
                continue;
            }
            Level level = plugin.getWeltManager().levelVon(p.getLocation());
            if (level == null || level.istBoss()) {
                continue;
            }
            int nah = 0;
            for (LivingEntity mob : gegner.values()) {
                if (mob.isValid() && mob.getWorld().equals(welt) && gleichesLevel(mob.getLocation(), p.getLocation())
                        && mob.getLocation().distanceSquared(p.getLocation()) < 40 * 40) {
                    nah++;
                }
            }
            if (nah >= maxProSpieler) {
                continue;
            }
            GegnerTyp typ = zufallsTyp(level);
            if (typ == null) {
                continue;
            }
            Location ort = spawnOrt(p, level);
            if (ort != null) {
                spawnen(typ, ort);
            }
        }
    }

    private GegnerTyp zufallsTyp(Level level) {
        ConfigurationSection tabelle = plugin.getConfig()
                .getConfigurationSection("gegner.spawn.level" + level.getNummer());
        if (tabelle == null) {
            return null;
        }
        List<GegnerTyp> typen = new ArrayList<GegnerTyp>();
        List<Integer> gewichte = new ArrayList<Integer>();
        int summe = 0;
        for (String schluessel : tabelle.getKeys(false)) {
            GegnerTyp typ = GegnerTyp.vonId(schluessel);
            int gewicht = tabelle.getInt(schluessel, 0);
            if (typ == null || typ == GegnerTyp.WARDEN || gewicht <= 0) {
                continue;
            }
            typen.add(typ);
            gewichte.add(gewicht);
            summe += gewicht;
        }
        if (summe <= 0) {
            return null;
        }
        int wurf = random.nextInt(summe);
        for (int i = 0; i < typen.size(); i++) {
            wurf -= gewichte.get(i);
            if (wurf < 0) {
                return typen.get(i);
            }
        }
        return null;
    }

    private Location spawnOrt(Player p, Level level) {
        double min = plugin.getConfig().getDouble("gegner.abstand-min", 14);
        double max = Math.max(min + 1, plugin.getConfig().getDouble("gegner.abstand-max", 28));
        World welt = p.getWorld();
        int y = level.getBodenY() + 1;
        for (int versuch = 0; versuch < 15; versuch++) {
            double winkel = random.nextDouble() * Math.PI * 2;
            double abstand = min + random.nextDouble() * (max - min);
            int x = (int) Math.floor(p.getLocation().getX() + Math.cos(winkel) * abstand);
            int z = (int) Math.floor(p.getLocation().getZ() + Math.sin(winkel) * abstand);
            if (!welt.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Block fuss = welt.getBlockAt(x, y, z);
            if (!fuss.getRelative(0, -1, 0).getType().isSolid()
                    || fuss.getType() != Material.AIR || fuss.getRelative(0, 1, 0).getType() != Material.AIR) {
                continue;
            }
            return new Location(welt, x + 0.5, y, z + 0.5, random.nextFloat() * 360F, 0F);
        }
        return null;
    }

    // ------------------------------------------------------------ Aufräumen

    private boolean istPluginTeil(Entity entity) {
        return entity.hasMetadata(META_TEIL);
    }

    /** Entfernt alle Mobs und Rüstungsständer in der Backrooms-Welt, die nicht zum Plugin gehören. */
    public void fremdeEntfernen() {
        World welt = plugin.getWeltManager().getWelt();
        if (welt == null) {
            return;
        }
        for (LivingEntity mob : welt.getLivingEntities()) {
            if (mob instanceof Player || gegner.containsKey(mob.getUniqueId()) || istPluginTeil(mob)) {
                continue;
            }
            mob.remove();
        }
    }

    public void allesEntfernen() {
        for (LivingEntity mob : new ArrayList<LivingEntity>(gegner.values())) {
            entfernen(mob);
        }
        gegner.clear();
        World welt = plugin.getWeltManager().getWelt();
        if (welt != null) {
            for (LivingEntity mob : welt.getLivingEntities()) {
                if (!(mob instanceof Player) && !istPluginTeil(mob)) {
                    mob.remove();
                }
            }
        }
    }

    // ------------------------------------------------------------ Events

    @EventHandler(ignoreCancelled = true)
    public void beimZielen(EntityTargetEvent event) {
        if (typVon(event.getEntity()) == null || event.getTarget() == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player)) {
            event.setCancelled(true); // Gegner kämpfen nicht untereinander
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchaden(EntityDamageByEntityEvent event) {
        Entity verursacher = event.getDamager();
        if (verursacher instanceof Projectile) {
            ProjectileSource schuetze = ((Projectile) verursacher).getShooter();
            if (schuetze instanceof Entity) {
                verursacher = (Entity) schuetze;
            }
        }
        GegnerTyp angreifer = typVon(verursacher);
        if (angreifer == null) {
            return;
        }
        if (typVon(event.getEntity()) != null) {
            event.setCancelled(true); // kein Eigenbeschuss unter Gegnern
            return;
        }
        // Schaden der Gegner kommt aus der config.yml statt aus den Vanilla-Werten
        double schaden = angreifer == GegnerTyp.WARDEN
                ? plugin.getConfig().getDouble("boss.schaden", angreifer.getStandardSchaden())
                : plugin.getConfig().getDouble("gegner.schaden." + angreifer.getId(), angreifer.getStandardSchaden());
        event.setDamage(schaden);
    }

    @EventHandler
    public void beimTod(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        GegnerTyp typ = typVon(mob);
        if (typ == null) {
            return;
        }
        List<ArmorStand> staender = modelle.remove(mob.getUniqueId());
        if (staender != null) {
            for (ArmorStand stand : staender) {
                stand.getWorld().spawnParticle(Particle.CLOUD, stand.getLocation().add(0, KOPF_HOEHE, 0), 3,
                        0.2, 0.2, 0.2, 0.02);
                teilZuGegner.remove(stand.getUniqueId());
                stand.remove();
            }
        }
        gegner.remove(mob.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(typ == GegnerTyp.WARDEN ? 200 : 5);

        Player killer = mob.getKiller();
        if (killer != null) {
            plugin.getSpielerDaten().besiegtErhoehen(killer.getUniqueId());
        }
        if (typ == GegnerTyp.WARDEN) {
            plugin.getBossKampf().besiegt();
            return;
        }
        double w = random.nextDouble() * 100;
        if (w < plugin.getConfig().getDouble("gegner.beute.mandelwasser", 20)) {
            event.getDrops().add(BackroomsItem.MANDELWASSER.erstellen(1));
        } else if (w < plugin.getConfig().getDouble("gegner.beute.mandelwasser", 20)
                + plugin.getConfig().getDouble("gegner.beute.energieriegel", 20)) {
            event.getDrops().add(BackroomsItem.ENERGIERIEGEL.erstellen(1));
        } else if (w < plugin.getConfig().getDouble("gegner.beute.mandelwasser", 20)
                + plugin.getConfig().getDouble("gegner.beute.energieriegel", 20)
                + plugin.getConfig().getDouble("gegner.beute.granate", 10)) {
            event.getDrops().add(BackroomsItem.GRANATE.erstellen(1));
        }
    }

    /**
     * Plugin-Rüstungsständer nehmen keinen Schaden. Treffer auf ein Modellteil
     * (Schlag oder Pistolenkugel) werden an den zugehörigen Gegner weitergegeben.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void beimTeilSchaden(EntityDamageEvent event) {
        if (!istPluginTeil(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
        LivingEntity mob = teilZuGegner.get(event.getEntity().getUniqueId());
        if (mob == null || !mob.isValid() || !(event instanceof EntityDamageByEntityEvent)) {
            return;
        }
        Entity verursacher = ((EntityDamageByEntityEvent) event).getDamager();
        if (verursacher instanceof Projectile) {
            ProjectileSource schuetze = ((Projectile) verursacher).getShooter();
            if ("PISTOLE".equals(ItemListener.schussArt(verursacher)) && schuetze instanceof Entity) {
                mob.damage(plugin.getConfig().getDouble("waffen.pistole-schaden", 7.0), (Entity) schuetze);
            }
            return; // Raketen und Granaten wirken über ihre Explosion
        }
        if (verursacher instanceof Player) {
            mob.damage(event.getDamage(), verursacher);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimTeilen(SlimeSplitEvent event) {
        if (typVon(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimBrennen(EntityCombustEvent event) {
        if (typVon(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimStaenderBenutzen(PlayerArmorStandManipulateEvent event) {
        if (istPluginTeil(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
