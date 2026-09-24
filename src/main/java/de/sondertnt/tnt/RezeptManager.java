package de.sondertnt.tnt;

import org.bukkit.Material;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;

/**
 * Registriert die Crafting-Rezepte der Sonder-TNT.
 * Jedes Rezept kann in der config.yml unter "rezepte.<typ>" abgeschaltet werden.
 *
 * Alle Zutaten haben den Datenwert 0 (in 1.8.8 setzt setIngredient(char, Material)
 * den Datenwert 0 voraus).
 */
public class RezeptManager {

    private final Plugin plugin;

    public RezeptManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Entfernt alte Rezepte dieses Plugins und registriert die aktivierten neu. */
    public int registrieren() {
        entfernen();
        int anzahl = 0;
        for (TntTyp typ : TntTyp.values()) {
            if (!plugin.getConfig().getBoolean("rezepte." + typ.getId(), true)) {
                continue;
            }
            if (plugin.getServer().addRecipe(rezept(typ))) {
                anzahl++;
            }
        }
        return anzahl;
    }

    /** Entfernt alle Rezepte, deren Ergebnis ein Sonder-TNT ist. */
    public void entfernen() {
        Iterator<Recipe> it = plugin.getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe rezept = it.next();
            if (rezept != null && SonderTntItems.istSonderTnt(rezept.getResult())) {
                it.remove();
            }
        }
    }

    private ShapedRecipe rezept(TntTyp typ) {
        ShapedRecipe rezept = new ShapedRecipe(SonderTntItems.erstellen(typ, 1));
        switch (typ) {
            case MEGA:
                // 8 TNT um einen Obsidian
                rezept.shape("TTT", "TOT", "TTT");
                rezept.setIngredient('T', Material.TNT);
                rezept.setIngredient('O', Material.OBSIDIAN);
                break;
            case FEUER:
                // TNT mit 4 Lohenstaub
                rezept.shape(" B ", "BTB", " B ");
                rezept.setIngredient('T', Material.TNT);
                rezept.setIngredient('B', Material.BLAZE_POWDER);
                break;
            case BLITZ:
                // TNT mit 4 Glowstonestaub
                rezept.shape(" G ", "GTG", " G ");
                rezept.setIngredient('T', Material.TNT);
                rezept.setIngredient('G', Material.GLOWSTONE_DUST);
                break;
            case LIFT:
                // TNT mit 2 Schleimbällen (oben/unten) und 2 Federn (links/rechts)
                rezept.shape(" S ", "FTF", " S ");
                rezept.setIngredient('T', Material.TNT);
                rezept.setIngredient('S', Material.SLIME_BALL);
                rezept.setIngredient('F', Material.FEATHER);
                break;
            case CLUSTER:
                // 5 TNT im X-Muster mit 4 Schwarzpulver
                rezept.shape("TPT", "PTP", "TPT");
                rezept.setIngredient('T', Material.TNT);
                rezept.setIngredient('P', Material.SULPHUR);
                break;
            default:
                throw new IllegalStateException("Kein Rezept für " + typ);
        }
        return rezept;
    }
}
