package de.backrooms;

import de.backrooms.befehle.BackroomsBefehl;
import de.backrooms.befehle.StartBefehl;
import de.backrooms.gegner.BossKampf;
import de.backrooms.gegner.GegnerManager;
import de.backrooms.items.ItemListener;
import de.backrooms.schutz.SchutzListener;
import de.backrooms.spieler.Anzeige;
import de.backrooms.spieler.Spielablauf;
import de.backrooms.spieler.SpielerDaten;
import de.backrooms.spieler.SpielerListener;
import de.backrooms.tnt.SonderTntListener;
import de.backrooms.tnt.TntEffekte;
import de.backrooms.tnt.TntZuender;
import de.backrooms.welt.Flackerlicht;
import de.backrooms.welt.WeltManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Backrooms für Spigot/Paper 1.12.2.
 */
public class BackroomsPlugin extends JavaPlugin {

    private WeltManager weltManager;
    private SpielerDaten spielerDaten;
    private Anzeige anzeige;
    private Spielablauf spielablauf;
    private GegnerManager gegnerManager;
    private BossKampf bossKampf;
    private Flackerlicht flackerlicht;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        weltManager = new WeltManager(this);
        spielerDaten = new SpielerDaten(this);
        anzeige = new Anzeige(this);
        spielablauf = new Spielablauf(this);
        gegnerManager = new GegnerManager(this);
        bossKampf = new BossKampf(this);
        flackerlicht = new Flackerlicht(this, weltManager);

        if (weltManager.laden() == null) {
            getLogger().severe("Die Backrooms-Welt konnte nicht geladen werden!");
        }
        gegnerManager.fremdeEntfernen();

        TntZuender zuender = new TntZuender(this);
        TntEffekte effekte = new TntEffekte(this);
        getServer().getPluginManager().registerEvents(new SonderTntListener(this, zuender, effekte), this);
        getServer().getPluginManager().registerEvents(new SchutzListener(weltManager), this);
        getServer().getPluginManager().registerEvents(new SpielerListener(this), this);
        final ItemListener itemListener = new ItemListener(this);
        getServer().getPluginManager().registerEvents(itemListener, this);
        getServer().getPluginManager().registerEvents(gegnerManager, this);

        StartBefehl start = new StartBefehl(this);
        befehl("start", start, start);
        BackroomsBefehl backrooms = new BackroomsBefehl(this);
        befehl("backrooms", backrooms, backrooms);

        // Takt: jede Sekunde Anzeige, Gegner und Boss; alle 10 Ticks das Flackerlicht
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                spielablauf.tick();
                gegnerManager.tick();
                bossKampf.tick();
            }
        }, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                flackerlicht.tick();
            }
        }, 10L, 10L);
        // Blockmodelle der Gegner folgen jeden Tick ihrem Mob
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                gegnerManager.modelleBewegen();
            }
        }, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                itemListener.taschenlampenTick();
            }
        }, 3L, 3L);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                spielerDaten.speichern();
            }
        }, 20L * 300, 20L * 300);
    }

    @Override
    public void onDisable() {
        if (flackerlicht != null) {
            flackerlicht.allesWiederherstellen();
        }
        if (bossKampf != null) {
            bossKampf.entfernen();
        }
        if (spielablauf != null) {
            spielablauf.schilderEntfernen();
        }
        if (gegnerManager != null) {
            gegnerManager.allesEntfernen();
        }
        if (anzeige != null) {
            for (Player p : getServer().getOnlinePlayers()) {
                anzeige.entfernen(p);
            }
        }
        if (spielerDaten != null) {
            spielerDaten.speichern();
        }
    }

    private void befehl(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand befehl = getCommand(name);
        if (befehl == null) {
            getLogger().severe("Befehl '" + name + "' fehlt in der plugin.yml!");
            return;
        }
        befehl.setExecutor(executor);
        befehl.setTabCompleter(completer);
    }

    /** Erlaubt auch das Laden über bukkit.yml oder Multiverse (Generator "Backrooms"). */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String weltName, String id) {
        return weltManager != null ? weltManager.getGenerator() : new WeltManager(this).getGenerator();
    }

    public WeltManager getWeltManager() {
        return weltManager;
    }

    public SpielerDaten getSpielerDaten() {
        return spielerDaten;
    }

    public Anzeige getAnzeige() {
        return anzeige;
    }

    public Spielablauf getSpielablauf() {
        return spielablauf;
    }

    public GegnerManager getGegnerManager() {
        return gegnerManager;
    }

    public BossKampf getBossKampf() {
        return bossKampf;
    }
}
