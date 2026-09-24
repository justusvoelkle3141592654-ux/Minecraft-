package de.backrooms.welt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Erzeugt die Backrooms-Welt nur aus Vanilla-Blöcken (1.8.8):
 * Level 0 (Lobby), Level 1 (Parkhaus), Level 2 (Rohre) übereinander
 * und die Boss-Arena um (0, 0).
 */
public class BackroomsGenerator extends ChunkGenerator {

    /** Block mit Datenwert (in 1.8.8 nur per ID + Daten setzbar). */
    private static final class Blocktyp {
        private final int id;
        private final byte daten;

        @SuppressWarnings("deprecation")
        private Blocktyp(Material material, int daten) {
            this.id = material.getId();
            this.daten = (byte) daten;
        }
    }

    /** Material, an dem Ausgänge erkannt werden (nur dort verbaut). */
    public static final Material AUSGANG = Material.EMERALD_BLOCK;

    private static final Blocktyp LUFT = new Blocktyp(Material.AIR, 0);
    private static final Blocktyp AUSGANG_BLOCK = new Blocktyp(AUSGANG, 0);
    private static final Blocktyp AUSGANG_LICHT = new Blocktyp(Material.GLOWSTONE, 0);

    // Level 0 - Lobby: senfgelbe Tapete, alter Teppich, vergilbte Decke, Neonröhren
    private static final Blocktyp L0_BODEN = new Blocktyp(Material.HARD_CLAY, 0);
    private static final Blocktyp L0_WAND = new Blocktyp(Material.STAINED_CLAY, 4);
    private static final Blocktyp L0_DECKE = new Blocktyp(Material.SANDSTONE, 2);
    private static final Blocktyp L0_LICHT = new Blocktyp(Material.SEA_LANTERN, 0);

    // Level 1 - Parkhaus: Beton, gelbe Markierungen, Leuchten
    private static final Blocktyp L1_BODEN = new Blocktyp(Material.STAINED_CLAY, 7);
    private static final Blocktyp L1_MARKIERUNG = new Blocktyp(Material.STAINED_CLAY, 4);
    private static final Blocktyp L1_WAND = new Blocktyp(Material.STAINED_CLAY, 8);
    private static final Blocktyp L1_SAEULE = new Blocktyp(Material.SMOOTH_BRICK, 0);
    private static final Blocktyp L1_DECKE = new Blocktyp(Material.STAINED_CLAY, 7);
    private static final Blocktyp L1_LICHT = new Blocktyp(Material.GLOWSTONE, 0);

    // Level 2 - Rohre: dunkle, enge Gänge mit Rohren unter der Decke
    private static final Blocktyp L2_BODEN = new Blocktyp(Material.SMOOTH_BRICK, 2);
    private static final Blocktyp L2_WAND = new Blocktyp(Material.STAINED_CLAY, 7);
    private static final Blocktyp L2_DECKE = new Blocktyp(Material.STAINED_CLAY, 15);
    private static final Blocktyp L2_ROHR = new Blocktyp(Material.IRON_FENCE, 0);
    private static final Blocktyp L2_LICHT = new Blocktyp(Material.GLOWSTONE, 0);

    // Boss-Arena
    private static final Blocktyp B_BODEN = new Blocktyp(Material.STAINED_CLAY, 15);
    private static final Blocktyp B_MUSTER = new Blocktyp(Material.STAINED_CLAY, 9);
    private static final Blocktyp B_WAND = new Blocktyp(Material.OBSIDIAN, 0);
    private static final Blocktyp B_LICHT = new Blocktyp(Material.SEA_LANTERN, 0);

    private final Plugin plugin;

    public BackroomsGenerator(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ChunkData generateChunkData(World welt, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData daten = createChunkData(welt);
        long seed = welt.getSeed();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = (chunkX << 4) + lx;
                int z = (chunkZ << 4) + lz;
                spalteLevel(daten, seed, Level.LEVEL_0, lx, lz, x, z);
                spalteLevel(daten, seed, Level.LEVEL_1, lx, lz, x, z);
                spalteLevel(daten, seed, Level.LEVEL_2, lx, lz, x, z);
                spalteArena(daten, lx, lz, x, z);
            }
        }
        return daten;
    }

    @SuppressWarnings("deprecation")
    private static void setze(ChunkData daten, int lx, int y, int lz, Blocktyp typ) {
        daten.setBlock(lx, y, lz, typ.id, typ.daten);
    }

    private void spalteLevel(ChunkData daten, long seed, Level level, int lx, int lz, int x, int z) {
        int zelle = level.getZelle();
        int boden = level.getBodenY();
        int decke = level.getDeckeY();
        int ix = Labyrinth.innerhalb(x, level);
        int iz = Labyrinth.innerhalb(z, level);
        int cx = Labyrinth.zelleVon(x, level);
        int cz = Labyrinth.zelleVon(z, level);

        Blocktyp bodenTyp;
        Blocktyp wandTyp;
        Blocktyp deckeTyp;
        Blocktyp lichtTyp;
        switch (level) {
            case LEVEL_1:
                bodenTyp = (ix == 1 || iz == 1) ? L1_MARKIERUNG : L1_BODEN;
                wandTyp = (ix == 0 && iz == 0) ? L1_SAEULE : L1_WAND;
                deckeTyp = L1_DECKE;
                lichtTyp = L1_LICHT;
                break;
            case LEVEL_2:
                bodenTyp = L2_BODEN;
                wandTyp = L2_WAND;
                deckeTyp = L2_DECKE;
                lichtTyp = L2_LICHT;
                break;
            default:
                bodenTyp = L0_BODEN;
                wandTyp = L0_WAND;
                deckeTyp = L0_DECKE;
                lichtTyp = L0_LICHT;
                break;
        }

        // Boden und Decke
        setze(daten, lx, boden, lz, bodenTyp);
        setze(daten, lx, decke, lz, deckeTyp);

        // Wände
        boolean wandVoll = false;
        boolean wandTuer = false;
        if (ix == 0 && iz == 0) {
            wandVoll = true; // Säule an jeder Kreuzung
        } else if (ix == 0 || iz == 0) {
            boolean senkrecht = ix == 0;
            Labyrinth.Segment seg = Labyrinth.segment(seed, level, cx, cz, senkrecht);
            if (seg == Labyrinth.Segment.WAND) {
                wandVoll = true;
            } else if (seg == Labyrinth.Segment.TUER) {
                int pos = Labyrinth.tuerPosition(seed, level, cx, cz, senkrecht);
                int entlang = senkrecht ? iz : ix;
                if (entlang == pos || entlang == pos + 1) {
                    wandTuer = true;
                } else {
                    wandVoll = true;
                }
            }
        }
        if (wandVoll) {
            for (int y = boden + 1; y < decke; y++) {
                setze(daten, lx, y, lz, wandTyp);
            }
            return;
        }
        if (wandTuer) {
            // Türöffnung 3 hoch, darüber ein Sturz
            for (int y = boden + 4; y < decke; y++) {
                setze(daten, lx, y, lz, wandTyp);
            }
            return;
        }

        // Zelleninneres
        int mitte = zelle / 2;
        boolean ausgang = Labyrinth.istAusgang(seed, level, cx, cz);
        if (ausgang && Math.abs(ix - mitte) <= 1 && Math.abs(iz - mitte) <= 1) {
            setze(daten, lx, boden, lz, AUSGANG_BLOCK);
            if (ix == mitte && iz == mitte) {
                setze(daten, lx, decke, lz, AUSGANG_LICHT);
            }
        }

        // Deckenlicht in der Zellenmitte (Level 0: Röhre aus 2 Blöcken)
        boolean lichtPos = iz == mitte && (ix == mitte || (level == Level.LEVEL_0 && ix == mitte - 1));
        if (lichtPos && !ausgang && Labyrinth.hatLicht(seed, level, cx, cz)) {
            setze(daten, lx, decke, lz, lichtTyp);
        }

        // Rohre unter der Decke in Level 2
        if (level == Level.LEVEL_2 && iz == 2) {
            setze(daten, lx, decke - 1, lz, L2_ROHR);
        }
    }

    private void spalteArena(ChunkData daten, int lx, int lz, int x, int z) {
        int r = Level.ARENA_RADIUS;
        if (Math.abs(x) > r || Math.abs(z) > r) {
            return;
        }
        Level boss = Level.BOSS;
        int boden = boss.getBodenY();
        int decke = boss.getDeckeY();
        int ring = Math.max(Math.abs(x), Math.abs(z));

        boolean rand = ring == r;
        boolean saeule = Math.abs(Math.abs(x) - 10) <= 1 && Math.abs(Math.abs(z) - 10) <= 1;

        setze(daten, lx, boden, lz, (ring == 6 || ring == 14) ? B_MUSTER : B_BODEN);
        boolean licht = !rand && Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4;
        setze(daten, lx, decke, lz, licht ? B_LICHT : B_BODEN);

        if (rand || saeule) {
            for (int y = boden + 1; y < decke; y++) {
                boolean streifen = rand && (y == boden + 4 || y == boden + 8);
                setze(daten, lx, y, lz, streifen ? B_MUSTER : B_WAND);
            }
        } else {
            for (int y = boden + 1; y < decke; y++) {
                setze(daten, lx, y, lz, LUFT);
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World welt) {
        return Collections.<BlockPopulator>singletonList(new VorratsPopulator(plugin));
    }

    @Override
    public Location getFixedSpawnLocation(World welt, Random random) {
        return Level.LEVEL_0.start(welt);
    }

    @Override
    public boolean canSpawn(World welt, int x, int z) {
        return true;
    }
}
