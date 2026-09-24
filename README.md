# SonderTNT

Plugin für **Spigot / Paper 1.8.8** (Java 8) mit Sonder-TNT und Strukturen aus
Vanilla-Blöcken. Es funktioniert auch mit **Eaglercraft**-Clients:

- nur Vanilla-Blöcke, -Items, -Partikel und -Sounds
- keine Resource Packs, keine Client-Mods, keine eigenen Modelle
- Sonder-TNT sind normale TNT-Items. Erkannt werden sie über eine feste Lore-Zeile
  (`SonderTNT-Typ: <TYP>`), weil es in 1.8.8 noch keine PersistentData gibt.
  Umbenennen im Amboss ändert daran nichts.

## Build

Voraussetzungen: JDK 8 oder neuer, Maven 3.

```bash
mvn clean package
```

Die fertige Datei liegt unter `target/SonderTNT-1.0.0.jar`. Kopiere sie in den
Ordner `plugins/` des Servers und starte ihn neu.

Die Spigot-API 1.8.8 kommt aus dem Spigot-Repository
(`https://hub.spigotmc.org/nexus/content/groups/public/`). Mit JDK 9 oder neuer
wird automatisch gegen die Java-8-API kompiliert (`--release 8`). So können keine
neueren Java-Methoden hineinrutschen.

## Sonder-TNT

| Typ       | Wirkung                                                                 | Rezept (Werkbank)                                   |
|-----------|-------------------------------------------------------------------------|-----------------------------------------------------|
| `mega`    | Explosion mit Stärke 15                                                  | 8 TNT rundherum, Obsidian in der Mitte               |
| `feuer`   | Explosion setzt die Umgebung in Brand, Spieler/Mobs im Radius brennen    | TNT in der Mitte, 4 Lohenstaub oben/unten/links/rechts |
| `blitz`   | Explosion und danach mehrere Blitze im Umkreis                         | TNT in der Mitte, 4 Glowstonestaub (Kreuz)           |
| `lift`    | Schleudert Spieler und Entities hoch, zerstört keine Blöcke, kein Schaden | TNT in der Mitte, Schleimball oben/unten, Feder links/rechts |
| `cluster` | Explosion, danach verteilen sich mehrere kleine TNT                     | 5 TNT im X-Muster, 4 Schwarzpulver dazwischen        |

- Beim Platzieren wird das TNT sofort gezündet, wie normales TNT durch Redstone.
  Der Typ wird am gezündeten TNT gespeichert, damit die richtige Explosion folgt.
- Auch aus einem Werfer (Dispenser) gezündet behält Sonder-TNT seinen Typ.
- Nach dem Lift-TNT gibt es standardmäßig keinen Fallschaden (einmalig, abschaltbar).
- Sonder-TNT kann nicht als Zutat für ein anderes Sonder-TNT benutzt werden. So geht es nicht aus Versehen verloren.
- Jedes Rezept lässt sich in der `config.yml` unter `rezepte:` abschalten.

## Strukturen

| Name         | Größe (B x T x H) | Beschreibung                                           |
|--------------|-------------------|--------------------------------------------------------|
| `turm`       | 7 x 7 x 17        | Steinziegel-Turm, 3 Etagen, Leiter, Zinnen             |
| `ruine`      | 11 x 11 x 6       | Zerbrochene Mauern, Säulenreste, Schutt, Spinnweben    |
| `schatzhaus` | 7 x 9 x 9         | Holzhaus mit Walmdach und Truhe mit Zufallsloot        |
| `festung`    | 15 x 15 x 10      | Ringmauer, 4 Ecktürme, Wehrgang, Tor, Bergfried        |

- Alle Strukturen werden per Bukkit-API aus Vanilla-Blöcken gebaut, ohne WorldEdit und ohne Schematic-Dateien.
- `/struktur bauen <name>` baut die Struktur vor dem Spieler, mit dem Eingang zum Spieler hin.
  **Achtung:** Der Bauraum wird freigeräumt. Was dort steht, wird ersetzt.
  Lücken unter der Grundfläche werden mit Bruchstein aufgefüllt.
- Der Truhen-Loot steht in der `config.yml` unter `strukturen.loot`
  (Format `MATERIAL:min:max:chance`, Chance in Prozent). Sonder-TNT als Loot:
  `SONDERTNT_MEGA:1:1:5` usw.

### Weltgenerierung

Unter `weltgenerierung:` in der `config.yml`:

- `aktiviert` – an/aus (Standard: **aus**)
- `chance-pro-chunk` – Wahrscheinlichkeit pro **neu generiertem** Chunk (Standard 0.002 ≈ 1 von 500)
- `gewichtung` – relative Häufigkeit je Struktur (0 = nie)
- `welten` – auf bestimmte Welten beschränken (leer = alle Oberwelten; Nether/End nie)
- `max-hoehenunterschied` – nur auf ausreichend flachem Boden bauen
- `log` – Koordinaten generierter Strukturen in der Konsole ausgeben

Strukturen erscheinen nur in Chunks, die neu erzeugt werden. Bereits erkundete Gebiete
bleiben unverändert. Jede Struktur wird komplett innerhalb eines Chunks gebaut.
Auf Wasser, Eis, Lava und Laub wird nicht gebaut.

## Befehle

| Befehl                                   | Beschreibung                                  |
|------------------------------------------|-----------------------------------------------|
| `/sondertnt give <Spieler> <typ> [anzahl]` | Sonder-TNT geben (Anzahl 1–2304)            |
| `/sondertnt alle <Spieler> [anzahl]`     | Alle 5 Sonder-TNT (je 64 oder `anzahl`) + 1 Feuerzeug |
| `/sondertnt list`                        | Alle Sonder-TNT-Typen anzeigen                |
| `/sondertnt reload`                      | `config.yml` neu laden (inkl. Rezepte, Loot)  |
| `/struktur bauen <name>`                 | Struktur vor dem Spieler bauen                |
| `/struktur list`                         | Alle Strukturen anzeigen                      |

Im Befehlsblock funktioniert auch `@p`, z. B. `sondertnt alle @p`
(in der `server.properties` muss `enable-command-block=true` stehen).

Alle Befehle haben Tab-Vervollständigung und brauchen die Permission
**`sondertnt.admin`** (Standard: OP).

## Konfiguration

Alle Werte sind in der `config.yml` kommentiert, z. B. Explosionsstärken,
Zündzeit, Blitz-Anzahl, Lift-Höhe und Anzahl der Cluster-TNT. Bitte in der Datei
keine Umlaute verwenden. Nach Änderungen `/sondertnt reload` ausführen.

## Getestet

Getestet auf Paper 1.8.8 (Build 445) mit Java 8 und einem 1.8.8-Testclient:

- alle fünf TNT-Typen, auch aus einem Werfer
- die fünf Rezepte und das Abschalten per reload
- alle Strukturen in allen vier Blickrichtungen, mit Leitern und Truhen-Loot
- die Weltgenerierung

Mit einem echten Eaglercraft-Client wurde nicht getestet.
