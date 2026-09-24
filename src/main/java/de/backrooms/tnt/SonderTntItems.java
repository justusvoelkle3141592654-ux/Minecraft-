package de.backrooms.tnt;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Erstellt Sonder-TNT-Items und erkennt sie wieder.
 *
 * In 1.8.8 gibt es keine PersistentDataContainer. Deshalb bekommt jedes
 * Sonder-TNT eine feste Markierungszeile in der Lore
 * (z. B. "SonderTNT-Typ: MEGA"). Die Lore kann im Survival-Modus
 * nicht verändert werden (auch nicht per Amboss), der Anzeigename schon.
 */
public final class SonderTntItems {

    /** Text der Markierungszeile (ohne Farbcodes). */
    public static final String MARKIERUNG = "SonderTNT-Typ: ";

    private SonderTntItems() {
    }

    public static ItemStack erstellen(TntTyp typ, int anzahl) {
        ItemStack item = new ItemStack(Material.TNT, Math.max(1, anzahl));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(typ.getAnzeigename());

        List<String> lore = new ArrayList<String>();
        for (String zeile : typ.getBeschreibung()) {
            lore.add(ChatColor.GRAY + zeile);
        }
        lore.add(ChatColor.DARK_GRAY + "Zündet sofort beim Platzieren.");
        lore.add(ChatColor.DARK_GRAY + MARKIERUNG + typ.name());
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /** @return der Typ des Items oder {@code null}, wenn es kein Sonder-TNT ist. */
    public static TntTyp typVon(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return null;
        }
        for (String zeile : meta.getLore()) {
            String klartext = ChatColor.stripColor(zeile);
            if (klartext != null && klartext.startsWith(MARKIERUNG)) {
                return TntTyp.vonId(klartext.substring(MARKIERUNG.length()).trim());
            }
        }
        return null;
    }

    public static boolean istSonderTnt(ItemStack item) {
        return typVon(item) != null;
    }
}
