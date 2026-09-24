package de.backrooms.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Neue Backrooms-Items (Spigot/Paper 1.12.2).
 *
 * Aussehen über das Backrooms-Resource-Pack:
 * - Waffen und Werkzeuge sind unzerstörbare Diamanthacken mit einer bestimmten
 *   Haltbarkeit (Modell-Nummer). Das Pack zeigt dafür ein eigenes Modell,
 *   normale Diamanthacken sehen weiter normal aus.
 * - Stapelbare Verbrauchsgüter nutzen ein seltenes Vanilla-Item, dessen Bild das
 *   Pack ersetzt (Keks, Ghast-Träne, Feuerwerksstern).
 * Ohne Pack funktionieren alle Items genauso, sie sehen nur wie das Grund-Item aus.
 *
 * Erkannt werden die Items über eine Markierungszeile in der Lore.
 */
public enum BackroomsItem {

    KLINGE("klinge", Material.DIAMOND_HOE, 1, ChatColor.AQUA + "" + ChatColor.BOLD + "Backrooms-Klinge",
            "30 Angriffsschaden!", "Leuchtet im Dunkeln der Backrooms."),
    PISTOLE("pistole", Material.DIAMOND_HOE, 2, ChatColor.GRAY + "" + ChatColor.BOLD + "Pistole",
            "Rechtsklick: Schießen", "Keine Munition nötig."),
    BAZOOKA("bazooka", Material.DIAMOND_HOE, 3, ChatColor.GREEN + "" + ChatColor.BOLD + "Bazooka",
            "Rechtsklick: Rakete abfeuern", "Explodiert, zerstört aber keine Blöcke."),
    TASCHENLAMPE("taschenlampe", Material.DIAMOND_HOE, 4, ChatColor.YELLOW + "" + ChatColor.BOLD + "Taschenlampe",
            "In der Hand: leuchtet dorthin,", "wohin du schaust."),
    MEDKIT("medkit", Material.DIAMOND_HOE, 5, ChatColor.RED + "" + ChatColor.BOLD + "Medkit",
            "Rechtsklick: volle Gesundheit."),
    WARDEN_HERZ("wardenherz", Material.DIAMOND_HOE, 6, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden-Herz",
            "Beweis, dass du den Warden", "besiegt hast und entkommen bist."),
    ADRENALIN("adrenalin", Material.DIAMOND_HOE, 7, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Adrenalinspritze",
            "Rechtsklick: 20 Sekunden", "schneller und höher springen."),
    MANDELWASSER("mandelwasser", Material.GHAST_TEAR, 0, ChatColor.AQUA + "" + ChatColor.BOLD + "Mandelwasser",
            "Rechtsklick: heilt 4 Herzen und", "vertreibt Blindheit und Gift."),
    ENERGIERIEGEL("energieriegel", Material.COOKIE, 0, ChatColor.GOLD + "" + ChatColor.BOLD + "Energieriegel",
            "Essen: satt und 20 Sekunden schnell."),
    GRANATE("granate", Material.FIREWORK_CHARGE, 0, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Granate",
            "Rechtsklick: werfen.", "Explodiert beim Aufprall."),
    KOMPASS("kompass", Material.COMPASS, 0, ChatColor.GREEN + "" + ChatColor.BOLD + "Ausgangs-Kompass",
            "Zeigt zum nächsten Ausgang.", "Rechtsklick: Entfernung anzeigen.");

    /** Text der Markierungszeile (ohne Farbcodes). */
    public static final String MARKIERUNG = "Backrooms-Item: ";

    private final String id;
    private final Material material;
    private final short modell;
    private final String anzeigename;
    private final List<String> beschreibung;

    BackroomsItem(String id, Material material, int modell, String anzeigename, String... beschreibung) {
        this.id = id;
        this.material = material;
        this.modell = (short) modell;
        this.anzeigename = anzeigename;
        this.beschreibung = Arrays.asList(beschreibung);
    }

    public String getId() {
        return id;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    /** Modell-Nummer (Haltbarkeit der Diamanthacke) oder 0. */
    public short getModell() {
        return modell;
    }

    public ItemStack erstellen(int anzahl) {
        ItemStack item = new ItemStack(material, Math.max(1, anzahl), modell);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(anzeigename);
        List<String> lore = new ArrayList<String>();
        for (String zeile : beschreibung) {
            lore.add(ChatColor.GRAY + zeile);
        }
        lore.add(ChatColor.DARK_GRAY + MARKIERUNG + name());
        meta.setLore(lore);
        if (material == Material.DIAMOND_HOE) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** @return das Backrooms-Item oder {@code null}. */
    public static BackroomsItem von(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        for (String zeile : item.getItemMeta().getLore()) {
            String klartext = ChatColor.stripColor(zeile);
            if (klartext != null && klartext.startsWith(MARKIERUNG)) {
                return vonId(klartext.substring(MARKIERUNG.length()).trim());
            }
        }
        return null;
    }

    public static BackroomsItem vonId(String text) {
        if (text == null) {
            return null;
        }
        for (BackroomsItem item : values()) {
            if (item.id.equalsIgnoreCase(text) || item.name().equalsIgnoreCase(text)) {
                return item;
            }
        }
        return null;
    }
}
