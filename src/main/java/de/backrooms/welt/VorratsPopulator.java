package de.backrooms.welt;

import de.backrooms.items.LootTabelle;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.Plugin;

import java.util.Random;

/**
 * Stellt in neu erzeugten Chunks Vorratstruhen mit Zufallsloot auf
 * (meist in Level 1, selten in Level 0 und 2). Gebaut wird nur im
 * eigenen Chunk, damit keine Nachbar-Chunks geladen werden.
 */
public class VorratsPopulator extends BlockPopulator {

    private static final BlockFace[] RICHTUNGEN = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};

    private final Plugin plugin;

    public VorratsPopulator(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World welt, Random random, Chunk chunk) {
        if (!plugin.isEnabled()) {
            return;
        }
        versuchen(welt, random, chunk, Level.LEVEL_0, plugin.getConfig().getDouble("vorrat.chance.level0", 0.06));
        versuchen(welt, random, chunk, Level.LEVEL_1, plugin.getConfig().getDouble("vorrat.chance.level1", 0.30));
        versuchen(welt, random, chunk, Level.LEVEL_2, plugin.getConfig().getDouble("vorrat.chance.level2", 0.10));
    }

    @SuppressWarnings("deprecation")
    private void versuchen(World welt, Random random, Chunk chunk, Level level, double chance) {
        if (random.nextDouble() >= chance) {
            return;
        }
        for (int versuch = 0; versuch < 8; versuch++) {
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            Block platz = welt.getBlockAt(x, level.getBodenY() + 1, z);
            Block boden = platz.getRelative(BlockFace.DOWN);
            if (platz.getType() != Material.AIR || boden.getType() == BackroomsGenerator.AUSGANG) {
                continue;
            }
            // Nicht direkt in eine Türöffnung stellen: nur in das Zelleninnere
            int ix = Labyrinth.innerhalb(x, level);
            int iz = Labyrinth.innerhalb(z, level);
            if (ix == 0 || iz == 0) {
                continue;
            }
            BlockFace blick = RICHTUNGEN[random.nextInt(RICHTUNGEN.length)];
            int daten = blick == BlockFace.NORTH ? 2 : blick == BlockFace.SOUTH ? 3 : blick == BlockFace.WEST ? 4 : 5;
            platz.setTypeIdAndData(Material.CHEST.getId(), (byte) daten, false);
            BlockState zustand = platz.getState();
            if (zustand instanceof Chest) {
                LootTabelle loot = new LootTabelle(plugin.getConfig().getStringList("vorrat.loot"));
                loot.fuellen(((Chest) zustand).getInventory(), random);
            }
            return;
        }
    }
}
