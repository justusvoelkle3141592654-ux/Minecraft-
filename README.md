# Backrooms

Backrooms-Abenteuer als Plugin für **Spigot / Paper 1.8.8** (Java 8). Es funktioniert auch
mit **Eaglercraft 1.8**-Clients, weil nur Vanilla-Inhalte verwendet werden:

- nur Vanilla-Blöcke, -Items, -Mobs, -Partikel und -Sounds
- keine Resource Packs, keine Client-Mods, keine eigenen Modelle oder Texturen
- neue Items und Gegner sind Vanilla-Items bzw. -Mobs mit eigenem Namen, eigener
  Beschreibung, Ausrüstung und Spezialfähigkeiten

## Build

Voraussetzungen: JDK 8 oder neuer, Maven 3.

```bash
mvn clean package
```

Die fertige Datei liegt unter `target/Backrooms-2.0.0.jar`. Kopiere sie in den Ordner
`plugins/` des **Spigot/Paper-1.8.8-Servers** (bei Eaglercraft nicht in den Proxy) und
starte den Server neu. Beim ersten Start wird die Welt `backrooms` erzeugt.

## Spielablauf

1. **`/start`** – Du bekommst deine Ausrüstung und fällst in **Level 0**.
2. In jedem Level suchst du den **Ausgang**: ein 3x3-Feld aus **Smaragdblöcken** mit
   Glowstone darüber. Draufstellen bringt dich ins nächste Level.
   - Der **Ausgangs-Kompass** zeigt immer zum nächsten Ausgang (Rechtsklick: Entfernung
     und Himmelsrichtung). In der Nähe steigen grüne Partikel auf.
   - Rechts am Bildschirm siehst du Level, Entfernung zum Ausgang und besiegte Gegner.
3. **Level 0 → Level 1 → Level 2 → Boss-Arena**
4. In der Arena erwacht nach 5 Sekunden der **Warden**. Besiegst du ihn, bekommst du das
   **Warden-Herz** und kehrst nach 5 Sekunden an den Ort zurück, an dem du `/start` benutzt hast.

**Tod:** Du behältst alle Items und startest am Anfang des **aktuellen** Levels neu.

Die Backrooms sind unzerstörbar: kein Abbauen, kein Bauen. Explosionen verletzen,
zerstören aber keine Blöcke. Sonder-TNT darf platziert werden.

### Level

| Level        | Aussehen                                                        | Gegner                                   |
|--------------|-----------------------------------------------------------------|------------------------------------------|
| Level 0      | „Die Lobby“: gelbe Wände, alter Teppich, flackernde Neonröhren   | Hound, Skin-Stealer, Faceling, Smiler     |
| Level 1      | „Das Parkhaus“: Beton, gelbe Markierungen, Vorratstruhen         | Partygoer, Todesmotte, Hound, Skin-Stealer |
| Level 2      | „Die Rohre“: dunkle, enge Gänge mit Rohren unter der Decke       | Smiler, Skin-Stealer, Todesmotte, Hound   |
| Boss-Arena   | dunkle Halle mit Obsidian-Säulen                                | Warden (Endboss)                          |

### Gegner

| Gegner        | Vanilla-Mob            | Besonderheit                                                    |
|---------------|------------------------|-----------------------------------------------------------------|
| Hound         | wütender Wolf          | sehr schnell, stärker, knurrt                                    |
| Smiler        | Enderman               | erscheint nur im Dunkeln, wird von der Taschenlampe geblendet    |
| Skin-Stealer  | Zombie                 | trägt einen Spielerkopf und Kleidung wie ein Spieler, Name versteckt |
| Partygoer     | Skelett                | Kürbiskopf, gelbe Kleidung, schießt mit dem Bogen, Noten-Partikel |
| Faceling      | Zombie-Dorfbewohner    | friedlich – greift erst an, wenn man es angreift                 |
| Todesmotte    | Höhlenspinne           | schnell und giftig                                               |
| **Warden**    | Wither-Skelett         | 300 Leben, dunkeltürkise Rüstung, **Dunkelheit** (Blindheit für alle), **Schallschlag** (Strahl, ignoriert Rüstung), ruft bei halben Leben 3 Hounds |

Gegner kämpfen nicht untereinander. Besiegte Gegner lassen manchmal Mandelwasser oder
Energieriegel fallen.

### Items

| Item              | Grundlage       | Wirkung                                                     |
|-------------------|-----------------|-------------------------------------------------------------|
| Mandelwasser      | Wasserflasche   | heilt 4 Herzen, entfernt Blindheit, Übelkeit, Gift, Wither  |
| Energieriegel     | Keks            | macht satt, 20 Sekunden Tempo                               |
| Taschenlampe      | Fackel          | in der Hand: Nachtsicht; blendet Smiler (nicht platzierbar) |
| Ausgangs-Kompass  | Kompass         | zeigt zum nächsten Ausgang, Rechtsklick = Entfernung        |
| Warden-Herz       | Netherstern     | Trophäe für den Sieg                                        |
| Sonder-TNT        | TNT             | Mega, Feuer, Blitz, Lift, Cluster (siehe unten)             |

### /start gibt

- Diamantschwert (Schärfe III, Haltbarkeit III)
- Diamantrüstung (Schutz II, Haltbarkeit III), wird direkt angezogen, wenn frei
- Ausgangs-Kompass, Taschenlampe, 3 Mandelwasser, 8 Energieriegel
- je 8 Sonder-TNT jeder Sorte

Alle Mengen und Verzauberungsstufen sind in der `config.yml` einstellbar.
`/start` funktioniert nur außerhalb der Backrooms. Pro Durchgang gibt es also einmal Ausrüstung.

### Sonder-TNT

Zünden sofort beim Platzieren. Erkannt werden sie über eine Lore-Zeile.

| Typ     | Wirkung                                                       |
|---------|---------------------------------------------------------------|
| Mega    | Explosion Stärke 15                                           |
| Feuer   | setzt die Umgebung in Brand                                   |
| Blitz   | mehrere Blitze im Umkreis                                     |
| Lift    | schleudert alles in die Luft, kein Schaden, kein Fallschaden  |
| Cluster | zerfällt in mehrere kleine TNT                                |

## Befehle

| Befehl                                      | Recht              | Beschreibung                                |
|---------------------------------------------|--------------------|---------------------------------------------|
| `/start`                                    | `backrooms.start` (alle) | Ausrüstung + ab in Level 0            |
| `/backrooms level <0\|1\|2\|boss> [Spieler]` | `backrooms.admin` (OP)  | in ein Level teleportieren            |
| `/backrooms verlassen [Spieler]`            | `backrooms.admin`  | Backrooms verlassen                          |
| `/backrooms item <Spieler> <item> [anzahl]` | `backrooms.admin`  | Items geben (z. B. `mandelwasser`, `mega_tnt`) |
| `/backrooms gegner <typ>`                   | `backrooms.admin`  | Gegner an der eigenen Position spawnen       |
| `/backrooms info`                           | `backrooms.admin`  | wer ist in welchem Level                     |
| `/backrooms reload`                         | `backrooms.admin`  | `config.yml` neu laden                       |

`backrooms.bauen` (OP) erlaubt Bauen in den Backrooms, aber nur im Kreativmodus.
Alle Befehle haben Tab-Vervollständigung.

## Konfiguration

Alles steht kommentiert in `plugins/Backrooms/config.yml`, zum Beispiel:
- Startausrüstung
- Flackerlicht
- Gegner-Leben und welche Gegner in welchem Level wie oft erscheinen
- Beute
- Warden (Leben, Schallschlag-Schaden, Abstände)
- Loot der Vorratstruhen
- Sonder-TNT

Bitte in der Datei keine Umlaute verwenden. Nach Änderungen `/backrooms reload` ausführen.

## Getestet

Getestet auf Paper 1.8.8 (Build 445) mit Java 8 und einem 1.8.8-Testclient:

- `/start` mit Ausrüstung
- Ausgänge durch alle Level bis zum Boss
- Warden-Kampf, Sieg und Rückkehr
- Tod und Respawn im aktuellen Level
- Gegner je Level, Faceling-Verhalten
- Flackerlicht, Mandelwasser
- Blockschutz, auch bei Mega-TNT

Mit einem echten Eaglercraft-Client wurde nicht getestet.
