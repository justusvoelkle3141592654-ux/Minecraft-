package de.sondertnt.struktur;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Lässt Strukturen bei der Generierung neuer Chunks selten und zufällig entstehen.
 * Die Einstellungen werden bei jedem Chunk aus der config.yml gelesen, dadurch
 * wirkt "/sondertnt reload" sofort.
 *
 * Jede Struktur wird vollständig innerhalb des gerade generierten Chunks
 * gebaut (alle Strukturen sind maximal 15x15 groß), damit keine Nachbar-Chunks
 * geladen oder generiert werden müssen.
 */
public class StrukturPopulator extends BlockPopulator {

    /** Oberflächen, auf denen gebaut werden darf (kein Wasser, Lava, Eis oder Laub). */
    private static final Set<Material> BODEN = EnumSet.of(
            Material.GRASS, Material.DIRT, Material.SAND, Material.STONE, Material.GRAVEL,
            Material.SANDSTONE, Material.SNOW_BLOCK, Material.MYCEL, Material.HARD_CLAY,
            Material.STAINED_CLAY, Material.CLAY);

    /** Dünne Pflanzen/Schichten, die über dem eigentlichen Boden liegen können. */
    private static final Set<Material> BEWUCHS = EnumSet.of(
            Material.AIR, Material.SNOW, Material.LONG_GRASS, Material.YELLOW_FLOWER,
            Material.RED_ROSE, Material.DOUBLE_PLANT, Material.DEAD_BUSH,
            Material.BROWN_MUSHROOM, Material.RED_MUSHROOM);

    private final Plugin plugin;
    private final StrukturManager manager;

    public StrukturPopulator(Plugin plugin, StrukturManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void populate(World welt, Random random, Chunk chunk) {
        if (!plugin.isEnabled()) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("weltgenerierung.aktiviert", false)) {
            return;
        }
        if (welt.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        List<String> welten = cfg.getStringList("weltgenerierung.welten");
        if (!welten.isEmpty() && !welten.contains(welt.getName())) {
            return;
        }
        double chance = cfg.getDouble("weltgenerierung.chance-pro-chunk", 0.002);
        if (random.nextDouble() >= chance) {
            return;
        }

        Struktur struktur = zufaelligeStruktur(cfg, random);
        if (struktur == null) {
            return;
        }
        int rotation = random.nextInt(4);
        int gx = Bauplan.groesseX(struktur.getBreite(), struktur.getTiefe(), rotation);
        int gz = Bauplan.groesseZ(struktur.getBreite(), struktur.getTiefe(), rotation);
        if (gx > 16 || gz > 16) {
            return;
        }
        int ox = (chunk.getX() << 4) + random.nextInt(16 - gx + 1);
        int oz = (chunk.getZ() << 4) + random.nextInt(16 - gz + 1);

        // Boden an Ecken und Mitte prüfen
        int[][] punkte = {
                {ox, oz}, {ox + gx - 1, oz}, {ox, oz + gz - 1}, {ox + gx - 1, oz + gz - 1},
                {ox + gx / 2, oz + gz / 2}
        };
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int mitte = 0;
        for (int i = 0; i < punkte.length; i++) {
            int y = bodenHoehe(welt, punkte[i][0], punkte[i][1]);
            if (y < 0) {
                return;
            }
            min = Math.min(min, y);
            max = Math.max(max, y);
            if (i == punkte.length - 1) {
                mitte = y;
            }
        }
        int erlaubt = Math.max(0, cfg.getInt("weltgenerierung.max-hoehenunterschied", 4));
        if (max - min > erlaubt) {
            return;
        }
        if (mitte < 5 || mitte + struktur.getHoehe() >= welt.getMaxHeight() - 1) {
            return;
        }

        manager.errichten(struktur, welt, ox, mitte, oz, rotation, random);

        if (cfg.getBoolean("weltgenerierung.log", true)) {
            plugin.getLogger().info("Struktur '" + struktur.getId() + "' generiert in " + welt.getName()
                    + " bei " + (ox + gx / 2) + " " + (mitte + 1) + " " + (oz + gz / 2));
        }
    }

    /** @return Y des obersten Bodenblocks oder -1, wenn der Untergrund ungeeignet ist. */
    private int bodenHoehe(World welt, int x, int z) {
        int y = welt.getHighestBlockYAt(x, z);
        while (y > 0 && BEWUCHS.contains(welt.getBlockAt(x, y, z).getType())) {
            y--;
        }
        if (y <= 0) {
            return -1;
        }
        return BODEN.contains(welt.getBlockAt(x, y, z).getType()) ? y : -1;
    }

    private Struktur zufaelligeStruktur(FileConfiguration cfg, Random random) {
        int summe = 0;
        List<String> ids = manager.alleIds();
        int[] gewichte = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            gewichte[i] = Math.max(0, cfg.getInt("weltgenerierung.gewichtung." + ids.get(i), 1));
            summe += gewichte[i];
        }
        if (summe <= 0) {
            return null;
        }
        int wurf = random.nextInt(summe);
        for (int i = 0; i < ids.size(); i++) {
            wurf -= gewichte[i];
            if (wurf < 0) {
                return manager.get(ids.get(i));
            }
        }
        return null;
    }
}
