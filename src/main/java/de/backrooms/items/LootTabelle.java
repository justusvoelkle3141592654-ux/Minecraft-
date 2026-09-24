package de.backrooms.items;

import de.backrooms.tnt.SonderTntItems;
import de.backrooms.tnt.TntTyp;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Loot-Tabelle aus der config.yml. Format je Eintrag:
 * {@code MATERIAL:min:max:chance}, Chance in Prozent (0-100).
 * Sonder-TNT: {@code SONDERTNT_<TYP>:min:max:chance}, z. B. SONDERTNT_MEGA:1:1:10.
 * Backrooms-Items: {@code BACKROOMS_<ITEM>:min:max:chance}, z. B. BACKROOMS_MANDELWASSER:1:2:50.
 */
public class LootTabelle {

    private static final String SONDER_PRAEFIX = "SONDERTNT_";
    private static final String BACKROOMS_PRAEFIX = "BACKROOMS_";

    private final List<Eintrag> eintraege = new ArrayList<Eintrag>();
    private final List<String> fehler = new ArrayList<String>();

    public LootTabelle(List<String> zeilen) {
        for (String zeile : zeilen) {
            Eintrag eintrag = parsen(zeile);
            if (eintrag != null) {
                eintraege.add(eintrag);
            }
        }
    }

    /** Ungültige Zeilen aus der Konfiguration (für Warnungen im Log). */
    public List<String> getFehler() {
        return fehler;
    }

    public boolean istLeer() {
        return eintraege.isEmpty();
    }

    /** Würfelt den Loot aus und verteilt ihn auf zufällige freie Plätze. */
    public void fuellen(Inventory inventar, Random random) {
        List<ItemStack> beute = new ArrayList<ItemStack>();
        for (Eintrag e : eintraege) {
            if (random.nextDouble() * 100.0 < e.chance) {
                beute.add(e.erzeugen(random));
            }
        }
        // Die Truhe soll nie ganz leer sein
        if (beute.isEmpty() && !eintraege.isEmpty()) {
            Eintrag e = eintraege.get(random.nextInt(eintraege.size()));
            beute.add(e.erzeugen(random));
        }

        List<Integer> freiePlaetze = new ArrayList<Integer>();
        for (int i = 0; i < inventar.getSize(); i++) {
            ItemStack vorhanden = inventar.getItem(i);
            if (vorhanden == null || vorhanden.getType() == Material.AIR) {
                freiePlaetze.add(i);
            }
        }
        Collections.shuffle(freiePlaetze, random);
        for (int i = 0; i < beute.size() && i < freiePlaetze.size(); i++) {
            inventar.setItem(freiePlaetze.get(i), beute.get(i));
        }
    }

    private Eintrag parsen(String zeile) {
        if (zeile == null) {
            return null;
        }
        String[] teile = zeile.trim().split(":");
        if (teile.length != 4) {
            fehler.add(zeile + " (Format: MATERIAL:min:max:chance)");
            return null;
        }
        String name = teile[0].trim().toUpperCase();
        int min;
        int max;
        double chance;
        try {
            min = Integer.parseInt(teile[1].trim());
            max = Integer.parseInt(teile[2].trim());
            chance = Double.parseDouble(teile[3].trim());
        } catch (NumberFormatException ex) {
            fehler.add(zeile + " (Zahl ungültig)");
            return null;
        }
        if (min < 1 || max < min) {
            fehler.add(zeile + " (min muss >= 1 und max >= min sein)");
            return null;
        }

        ItemStack vorlage;
        if (name.startsWith(SONDER_PRAEFIX)) {
            TntTyp typ = TntTyp.vonId(name.substring(SONDER_PRAEFIX.length()));
            if (typ == null) {
                fehler.add(zeile + " (unbekannter Sonder-TNT-Typ)");
                return null;
            }
            vorlage = SonderTntItems.erstellen(typ, 1);
        } else if (name.startsWith(BACKROOMS_PRAEFIX)) {
            BackroomsItem item = BackroomsItem.vonId(name.substring(BACKROOMS_PRAEFIX.length()));
            if (item == null) {
                fehler.add(zeile + " (unbekanntes Backrooms-Item)");
                return null;
            }
            vorlage = item.erstellen(1);
        } else {
            Material material = Material.matchMaterial(name);
            if (material == null || material == Material.AIR) {
                fehler.add(zeile + " (unbekanntes Material)");
                return null;
            }
            vorlage = new ItemStack(material, 1);
        }
        return new Eintrag(vorlage, min, max, chance);
    }

    private static final class Eintrag {
        private final ItemStack vorlage;
        private final int min;
        private final int max;
        private final double chance;

        private Eintrag(ItemStack vorlage, int min, int max, double chance) {
            this.vorlage = vorlage;
            this.min = min;
            this.max = max;
            this.chance = chance;
        }

        private ItemStack erzeugen(Random random) {
            ItemStack item = vorlage.clone();
            int anzahl = min + random.nextInt(max - min + 1);
            item.setAmount(Math.min(anzahl, item.getMaxStackSize()));
            return item;
        }
    }
}
