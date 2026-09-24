package de.backrooms.welt;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

/** Lädt bzw. erstellt die Backrooms-Welt und stellt sie passend ein. */
public class WeltManager {

    private final Plugin plugin;
    private final BackroomsGenerator generator;
    private World welt;

    public WeltManager(Plugin plugin) {
        this.plugin = plugin;
        this.generator = new BackroomsGenerator(plugin);
    }

    public BackroomsGenerator getGenerator() {
        return generator;
    }

    public String getWeltName() {
        return plugin.getConfig().getString("welt.name", "backrooms");
    }

    /** Lädt die Welt oder erstellt sie beim ersten Start. */
    public World laden() {
        String name = getWeltName();
        welt = plugin.getServer().getWorld(name);
        if (welt == null) {
            plugin.getLogger().info("Lade/erstelle Backrooms-Welt '" + name + "' ...");
            welt = new WorldCreator(name)
                    .generator(generator)
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        }
        if (welt != null) {
            einstellen(welt);
        }
        return welt;
    }

    private void einstellen(World w) {
        w.setSpawnFlags(false, false);
        w.setMonsterSpawnLimit(0);
        w.setAnimalSpawnLimit(0);
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setGameRuleValue("keepInventory", "true");
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000L);
        w.setPVP(plugin.getConfig().getBoolean("welt.pvp", false));
        Location start = Level.LEVEL_0.start(w);
        w.setSpawnLocation(start.getBlockX(), start.getBlockY(), start.getBlockZ());
    }

    public World getWelt() {
        return welt;
    }

    public boolean istBackrooms(World w) {
        return w != null && welt != null && w.getUID().equals(welt.getUID());
    }

    /** @return das Level des Ortes oder {@code null}, wenn er nicht in den Backrooms liegt. */
    public Level levelVon(Location ort) {
        if (ort == null || !istBackrooms(ort.getWorld())) {
            return null;
        }
        return Level.vonHoehe(ort.getY());
    }
}
