package de.backrooms.gegner;

import de.backrooms.BackroomsPlugin;
import de.backrooms.items.BackroomsItem;
import de.backrooms.welt.Level;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Spawnt, steuert und entfernt die Backrooms-Gegner. */
public class GegnerManager implements Listener {

    public static final String META_TYP = "backrooms_gegner";
    private static final String META_GEREIZT = "backrooms_gereizt";
    private static final int UNENDLICH = Integer.MAX_VALUE;

    private final BackroomsPlugin plugin;
    private final Map<UUID, LivingEntity> gegner = new HashMap<UUID, LivingEntity>();
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
            case HOUND: {
                Wolf wolf = welt.spawn(ort, Wolf.class);
                wolf.setAngry(true);
                effekt(wolf, PotionEffectType.SPEED, 1);
                effekt(wolf, PotionEffectType.INCREASE_DAMAGE, 0);
                mob = wolf;
                break;
            }
            case SMILER: {
                Enderman enderman = welt.spawn(ort, Enderman.class);
                effekt(enderman, PotionEffectType.INCREASE_DAMAGE, 0);
                mob = enderman;
                break;
            }
            case SKIN_STEALER: {
                Zombie zombie = welt.spawn(ort, Zombie.class);
                zombie.setBaby(false);
                zombie.setVillager(false);
                // Sieht aus wie ein Spieler: Kopf eines Spielers, Kleidung wie Steve
                ausruesten(zombie, spielerKopf(naechsterSpielerName(ort)),
                        leder(Material.LEATHER_CHESTPLATE, 0x3CA0B4),
                        leder(Material.LEATHER_LEGGINGS, 0x3C3CA0),
                        leder(Material.LEATHER_BOOTS, 0x505050), null);
                effekt(zombie, PotionEffectType.SPEED, 0);
                mob = zombie;
                break;
            }
            case PARTYGOER: {
                Skeleton skelett = welt.spawn(ort, Skeleton.class);
                ausruesten(skelett, new ItemStack(Material.PUMPKIN),
                        leder(Material.LEATHER_CHESTPLATE, 0xFFD800),
                        leder(Material.LEATHER_LEGGINGS, 0xFFD800),
                        leder(Material.LEATHER_BOOTS, 0xFF4FA0), new ItemStack(Material.BOW));
                mob = skelett;
                break;
            }
            case FACELING: {
                Zombie zombie = welt.spawn(ort, Zombie.class);
                zombie.setBaby(false);
                zombie.setVillager(true);
                ausruesten(zombie, null, null, null, null, null);
                mob = zombie;
                break;
            }
            case TODESMOTTE: {
                CaveSpider spinne = welt.spawn(ort, CaveSpider.class);
                effekt(spinne, PotionEffectType.SPEED, 1);
                mob = spinne;
                break;
            }
            case WARDEN: {
                Skeleton skelett = welt.spawn(ort, Skeleton.class);
                skelett.setSkeletonType(Skeleton.SkeletonType.WITHER);
                int farbe = 0x0F4C5C;
                ausruesten(skelett, leder(Material.LEATHER_HELMET, 0x062A33),
                        leder(Material.LEATHER_CHESTPLATE, farbe),
                        leder(Material.LEATHER_LEGGINGS, farbe),
                        leder(Material.LEATHER_BOOTS, 0x062A33), null);
                effekt(skelett, PotionEffectType.INCREASE_DAMAGE, 1);
                effekt(skelett, PotionEffectType.FIRE_RESISTANCE, 0);
                skelett.setRemoveWhenFarAway(false);
                mob = skelett;
                break;
            }
            default:
                throw new IllegalArgumentException("Unbekannter Gegner " + typ);
        }

        double leben = typ == GegnerTyp.WARDEN
                ? plugin.getConfig().getDouble("boss.leben", typ.getStandardLeben())
                : plugin.getConfig().getDouble("gegner.leben." + typ.getId(), typ.getStandardLeben());
        leben = Math.max(1.0, Math.min(2000.0, leben));
        mob.setMaxHealth(leben);
        mob.setHealth(leben);
        mob.setCustomName(typ.getAnzeigename());
        mob.setCustomNameVisible(typ != GegnerTyp.SKIN_STEALER);
        mob.setCanPickupItems(false);
        mob.setMetadata(META_TYP, new FixedMetadataValue(plugin, typ.name()));
        gegner.put(mob.getUniqueId(), mob);
        return mob;
    }

    private static void effekt(LivingEntity mob, PotionEffectType typ, int staerke) {
        mob.addPotionEffect(new PotionEffect(typ, UNENDLICH, staerke, true), true);
    }

    private static void ausruesten(LivingEntity mob, ItemStack kopf, ItemStack brust, ItemStack beine,
                                   ItemStack schuhe, ItemStack hand) {
        EntityEquipment e = mob.getEquipment();
        e.setHelmet(kopf);
        e.setChestplate(brust);
        e.setLeggings(beine);
        e.setBoots(schuhe);
        // Setzen der Hand aktualisiert bei Skeletten die Angriffsart (Bogen oder Nahkampf)
        e.setItemInHand(hand);
        e.setHelmetDropChance(0F);
        e.setChestplateDropChance(0F);
        e.setLeggingsDropChance(0F);
        e.setBootsDropChance(0F);
        e.setItemInHandDropChance(0F);
    }

    private static ItemStack leder(Material material, int rgb) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(rgb));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack spielerKopf(String besitzer) {
        ItemStack kopf = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (besitzer != null) {
            SkullMeta meta = (SkullMeta) kopf.getItemMeta();
            meta.setOwner(besitzer);
            kopf.setItemMeta(meta);
        }
        return kopf;
    }

    private static String naechsterSpielerName(Location ort) {
        Player naechster = null;
        double beste = Double.MAX_VALUE;
        for (Player p : ort.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(ort);
            if (d < beste) {
                beste = d;
                naechster = p;
            }
        }
        return naechster == null ? null : naechster.getName();
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

    private static boolean istZiel(Player p) {
        return !p.isDead() && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE);
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

        Iterator<LivingEntity> it = gegner.values().iterator();
        while (it.hasNext()) {
            LivingEntity mob = it.next();
            if (!mob.isValid()) {
                it.remove();
                continue;
            }
            GegnerTyp typ = typVon(mob);
            if (typ == null) {
                continue;
            }
            // Weit weg von allen Spielern -> entfernen (außer dem Boss)
            if (typ != GegnerTyp.WARDEN && keinSpielerInDerNaehe(mob, 64.0)) {
                mob.remove();
                it.remove();
                continue;
            }
            zielSetzen(mob, typ);
            faehigkeiten(mob, typ);
        }

        int intervall = Math.max(1, plugin.getConfig().getInt("gegner.spawn-intervall-sekunden", 5));
        if (plugin.getConfig().getBoolean("gegner.aktiviert", true) && sekunden % intervall == 0) {
            spawnRunde(welt);
        }
        if (sekunden % 30 == 0) {
            fremdeEntfernen();
        }
    }

    /** Die Level liegen übereinander, deshalb zählt nur, wer im selben Level ist. */
    private static boolean gleichesLevel(Location a, Location b) {
        return Level.vonHoehe(a.getY()) == Level.vonHoehe(b.getY());
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
            return;
        }
        if (typ == GegnerTyp.FACELING && !mob.hasMetadata(META_GEREIZT)) {
            return;
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

    private void faehigkeiten(LivingEntity mob, GegnerTyp typ) {
        Location ort = mob.getLocation();
        switch (typ) {
            case SMILER:
                // Taschenlampe blendet Smiler in der Nähe
                for (Player p : mob.getWorld().getPlayers()) {
                    if (BackroomsItem.von(p.getItemInHand()) != BackroomsItem.TASCHENLAMPE) {
                        continue;
                    }
                    if (p.getLocation().distanceSquared(ort) > 10 * 10 || !p.hasLineOfSight(mob)) {
                        continue;
                    }
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3), true);
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1), true);
                    Vector weg = ort.toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (weg.lengthSquared() > 0.01) {
                        mob.setVelocity(weg.normalize().multiply(0.6).setY(0.2));
                    }
                    break;
                }
                break;
            case HOUND:
                if (random.nextInt(12) == 0) {
                    mob.getWorld().playSound(ort, Sound.WOLF_GROWL, 1.0F, 0.6F);
                }
                break;
            case PARTYGOER:
                if (random.nextInt(3) == 0) {
                    mob.getWorld().spigot().playEffect(ort.clone().add(0, 2.2, 0), Effect.NOTE,
                            0, 0, 0.4F, 0.3F, 0.4F, 1.0F, 3, 32);
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ Spawn-Runde

    private void spawnRunde(World welt) {
        int maxProSpieler = plugin.getConfig().getInt("gegner.max-pro-spieler", 5);
        int maxGesamt = plugin.getConfig().getInt("gegner.max-gesamt", 60);
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
            Location ort = spawnOrt(p, level, typ);
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

    private Location spawnOrt(Player p, Level level, GegnerTyp typ) {
        double min = plugin.getConfig().getDouble("gegner.abstand-min", 12);
        double max = Math.max(min + 1, plugin.getConfig().getDouble("gegner.abstand-max", 26));
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
            if (!fuss.getRelative(0, -1, 0).getType().isSolid()) {
                continue;
            }
            boolean frei = true;
            for (int h = 0; h < typ.getLuftBedarf(); h++) {
                if (fuss.getRelative(0, h, 0).getType() != Material.AIR) {
                    frei = false;
                    break;
                }
            }
            if (!frei) {
                continue;
            }
            if (typ.istNurImDunkeln() && fuss.getLightLevel() > 7) {
                continue;
            }
            return new Location(welt, x + 0.5, y, z + 0.5, random.nextFloat() * 360F, 0F);
        }
        return null;
    }

    // ------------------------------------------------------------ Aufräumen

    /** Entfernt alle Mobs in der Backrooms-Welt, die nicht von diesem Plugin gesteuert werden. */
    public void fremdeEntfernen() {
        World welt = plugin.getWeltManager().getWelt();
        if (welt == null) {
            return;
        }
        for (LivingEntity mob : welt.getLivingEntities()) {
            if (!(mob instanceof Player) && !gegner.containsKey(mob.getUniqueId())) {
                mob.remove();
            }
        }
    }

    public void allesEntfernen() {
        for (LivingEntity mob : gegner.values()) {
            mob.remove();
        }
        gegner.clear();
        fremdeEntfernen();
    }

    // ------------------------------------------------------------ Events

    @EventHandler(ignoreCancelled = true)
    public void beimZielen(EntityTargetEvent event) {
        GegnerTyp typ = typVon(event.getEntity());
        if (typ == null || event.getTarget() == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player)) {
            event.setCancelled(true); // Gegner kämpfen nicht untereinander
            return;
        }
        if (typ == GegnerTyp.FACELING && !event.getEntity().hasMetadata(META_GEREIZT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchaden(EntityDamageByEntityEvent event) {
        GegnerTyp opfer = typVon(event.getEntity());
        if (opfer == null) {
            return;
        }
        Entity verursacher = event.getDamager();
        if (verursacher instanceof Projectile) {
            ProjectileSource schuetze = ((Projectile) verursacher).getShooter();
            if (schuetze instanceof Entity) {
                verursacher = (Entity) schuetze;
            }
        }
        if (typVon(verursacher) != null) {
            event.setCancelled(true); // kein Eigenbeschuss unter Gegnern
            return;
        }
        if (opfer == GegnerTyp.FACELING && verursacher instanceof Player) {
            event.getEntity().setMetadata(META_GEREIZT, new FixedMetadataValue(plugin, true));
            ((Creature) event.getEntity()).setTarget((Player) verursacher);
        }
    }

    @EventHandler
    public void beimTod(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        GegnerTyp typ = typVon(mob);
        if (typ == null) {
            return;
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
        if (random.nextDouble() * 100 < plugin.getConfig().getDouble("gegner.beute.mandelwasser", 10)) {
            event.getDrops().add(BackroomsItem.MANDELWASSER.erstellen(1));
        }
        if (random.nextDouble() * 100 < plugin.getConfig().getDouble("gegner.beute.energieriegel", 15)) {
            event.getDrops().add(BackroomsItem.ENERGIERIEGEL.erstellen(1));
        }
    }
}
