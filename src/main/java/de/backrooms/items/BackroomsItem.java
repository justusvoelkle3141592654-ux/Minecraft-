package de.backrooms.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Neue Backrooms-Items. Weil Eaglercraft keine Resource Packs erlaubt,
 * sind es Vanilla-Items mit eigenem Namen, eigener Beschreibung und einer
 * Markierungszeile in der Lore, an der das Plugin sie erkennt.
 */
public enum BackroomsItem {

    MANDELWASSER("mandelwasser", Material.POTION, 0, ChatColor.AQUA + "" + ChatColor.BOLD + "Mandelwasser",
            "Heilt 4 Herzen und vertreibt", "Blindheit, Übelkeit und Gift."),
    ENERGIERIEGEL("energieriegel", Material.COOKIE, 0, ChatColor.GOLD + "" + ChatColor.BOLD + "Energieriegel",
            "Macht satt und 20 Sekunden schnell."),
    TASCHENLAMPE("taschenlampe", Material.TORCH, 0, ChatColor.YELLOW + "" + ChatColor.BOLD + "Taschenlampe",
            "In der Hand: Nachtsicht.", "Smiler in der Nähe werden geblendet."),
    KOMPASS("kompass", Material.COMPASS, 0, ChatColor.GREEN + "" + ChatColor.BOLD + "Ausgangs-Kompass",
            "Zeigt zum nächsten Ausgang.", "Rechtsklick: Entfernung anzeigen."),
    WARDEN_HERZ("wardenherz", Material.NETHER_STAR, 0, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden-Herz",
            "Beweis, dass du den Warden", "besiegt hast und entkommen bist.");

    /** Text der Markierungszeile (ohne Farbcodes). */
    public static final String MARKIERUNG = "Backrooms-Item: ";

    private final String id;
    private final Material material;
    private final short daten;
    private final String anzeigename;
    private final List<String> beschreibung;

    BackroomsItem(String id, Material material, int daten, String anzeigename, String... beschreibung) {
        this.id = id;
        this.material = material;
        this.daten = (short) daten;
        this.anzeigename = anzeigename;
        this.beschreibung = Arrays.asList(beschreibung);
    }

    public String getId() {
        return id;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    public ItemStack erstellen(int anzahl) {
        ItemStack item = new ItemStack(material, Math.max(1, anzahl), daten);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(anzeigename);
        List<String> lore = new ArrayList<String>();
        for (String zeile : beschreibung) {
            lore.add(ChatColor.GRAY + zeile);
        }
        lore.add(ChatColor.DARK_GRAY + MARKIERUNG + name());
        meta.setLore(lore);
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
