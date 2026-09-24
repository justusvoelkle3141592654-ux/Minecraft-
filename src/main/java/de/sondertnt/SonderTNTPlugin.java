package de.sondertnt;

import de.sondertnt.befehle.SonderTntBefehl;
import de.sondertnt.befehle.StrukturBefehl;
import de.sondertnt.struktur.StrukturManager;
import de.sondertnt.struktur.StrukturPopulator;
import de.sondertnt.tnt.RezeptManager;
import de.sondertnt.tnt.SonderTntListener;
import de.sondertnt.tnt.TntEffekte;
import de.sondertnt.tnt.TntZuender;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public class SonderTNTPlugin extends JavaPlugin implements Listener {

    public static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "SonderTNT"
            + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private RezeptManager rezeptManager;
    private StrukturManager strukturManager;
    private StrukturPopulator populator;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        TntZuender zuender = new TntZuender(this);
        TntEffekte effekte = new TntEffekte(this);
        rezeptManager = new RezeptManager(this);
        strukturManager = new StrukturManager(this);
        populator = new StrukturPopulator(this, strukturManager);

        getServer().getPluginManager().registerEvents(new SonderTntListener(this, zuender, effekte), this);
        getServer().getPluginManager().registerEvents(this, this);

        SonderTntBefehl tntBefehl = new SonderTntBefehl(this);
        registriereBefehl("sondertnt", tntBefehl, tntBefehl);
        StrukturBefehl strukturBefehl = new StrukturBefehl(strukturManager);
        registriereBefehl("struktur", strukturBefehl, strukturBefehl);

        int rezepte = rezeptManager.registrieren();
        for (World welt : getServer().getWorlds()) {
            populatorHinzufuegen(welt);
        }
        getLogger().info(rezepte + " Rezept(e) registriert, Weltgenerierung "
                + (getConfig().getBoolean("weltgenerierung.aktiviert", false) ? "aktiv." : "deaktiviert."));
    }

    @Override
    public void onDisable() {
        if (rezeptManager != null) {
            rezeptManager.entfernen();
        }
        for (World welt : getServer().getWorlds()) {
            populatorEntfernen(welt);
        }
    }

    /**
     * Lädt die config.yml neu und registriert die Rezepte passend zu den Einstellungen.
     *
     * @return Anzahl der aktiven Rezepte
     */
    public int neuLaden() {
        reloadConfig();
        int rezepte = rezeptManager.registrieren();
        strukturManager.lootNeuLaden();
        return rezepte;
    }

    /** Welten, die nach dem Start geladen werden, bekommen den Populator ebenfalls. */
    @EventHandler
    public void beiWeltInit(WorldInitEvent event) {
        populatorHinzufuegen(event.getWorld());
    }

    private void registriereBefehl(String name, org.bukkit.command.CommandExecutor executor,
                                   org.bukkit.command.TabCompleter completer) {
        PluginCommand befehl = getCommand(name);
        if (befehl == null) {
            getLogger().severe("Befehl '" + name + "' fehlt in der plugin.yml!");
            return;
        }
        befehl.setExecutor(executor);
        befehl.setTabCompleter(completer);
    }

    private void populatorHinzufuegen(World welt) {
        populatorEntfernen(welt);
        welt.getPopulators().add(populator);
    }

    /**
     * Entfernt Populatoren dieses Plugins. Der Vergleich über den Klassennamen
     * erfasst auch Instanzen aus einem früheren Laden des Plugins (z. B. nach /reload).
     */
    private void populatorEntfernen(World welt) {
        Iterator<BlockPopulator> it = welt.getPopulators().iterator();
        while (it.hasNext()) {
            if (it.next().getClass().getName().equals(StrukturPopulator.class.getName())) {
                it.remove();
            }
        }
    }
}
