# Prompt: Backrooms-Plugin für Minecraft 1.12.2 nachbauen

Kopiere alles unterhalb der Linie in eine andere KI.

---

Du bist ein erfahrener Java-Entwickler für Minecraft-Server-Plugins. Baue mir ein komplettes Plugin nach der folgenden Beschreibung. Liefere **alle Dateien vollständig** (keine Platzhalter wie „… Rest wie oben“): `pom.xml`, `plugin.yml`, `config.yml`, alle Java-Klassen, ein Python-Skript für das Resource Pack und eine `README.md` auf Deutsch.

## Wichtige Regeln

- **Plattform:** Spigot/Paper **1.12.2**, Java 8, Maven. Build mit `mvn clean package`.
- **Nur Methoden verwenden, die es in der Spigot-API 1.12.2 wirklich gibt.** Wenn du dir bei einer Methode, einem `Material`-, `Sound`- oder `Particle`-Namen nicht sicher bist, sag das ausdrücklich oder nimm eine sichere Alternative. Nichts erfinden.
- 1.12.2 verwendet noch die **alten Materialnamen mit Datenwerten** (z. B. `STAINED_CLAY` mit Farbe 4, `CONCRETE`, `SMOOTH_BRICK`, `IRON_FENCE`, `SEA_LANTERN`, `PRISMARINE` Daten 2 = dunkel). Die neuen 1.13-Namen gibt es nicht.
- Paket: `de.backrooms`, Hauptklasse `de.backrooms.BackroomsPlugin`, Plugin-Name `Backrooms`.
- Code-Kommentare, Chat-Texte und Item-Namen auf **Deutsch**. In der `config.yml` keine Umlaute.
- Wenn etwas unklar ist: **nachfragen statt raten.**

## Maven

- Repository: `https://hub.spigotmc.org/nexus/content/groups/public/`
- Abhängigkeit: `org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT`, Scope `provided`
- `source`/`target` 1.8, Encoding UTF-8, Resource-Filtering an (für `${project.version}` in der `plugin.yml`)
- `finalName`: `Backrooms-${project.version}`

## Spielidee

Spieler tippen `/start`, bekommen eine Ausrüstung und fallen in die „Backrooms“. Das ist eine eigene Welt mit drei Labyrinth-Leveln übereinander und einer Boss-Arena. In jedem Level suchen sie eine Ausgangs-Säule und drücken den Knopf daran. So geht es Level 0 → Level 1 → Level 2 → Boss-Arena. Dort kämpfen sie gegen den **Warden**. Wer ihn besiegt, bekommt eine Trophäe und kehrt an den Ort zurück, an dem er `/start` benutzt hat.

Der Schwierigkeitsgrad soll **leicht** sein (für Kinder geeignet).

## 1. Welt und Level

Beim Plugin-Start wird die Welt `backrooms` geladen oder mit einem eigenen `ChunkGenerator` erstellt.

**Einstellungen der Welt:**
- `setSpawnFlags(false,false)`, Monster- und Tier-Spawnlimit 0
- Gamerules: `doMobSpawning=false`, `doDaylightCycle=false`, `mobGriefing=false`, `keepInventory=true`
- kein Regen, PvP laut Config (Standard: aus)

**Generator:** `generateChunkData` mit `createChunkData(world)` und `ChunkData.setBlock(x, y, z, id, datenByte)` (für Farben).

Alles wird **deterministisch** aus dem Welt-Seed und den Weltkoordinaten berechnet. Nimm dafür eine Hash-Funktion (z. B. SplitMix64-artig) mit einem Zufallswert in [0,1). So passen die Chunks nahtlos zusammen, und der Kompass kann Ausgänge berechnen, ohne Blöcke zu lesen.

**Level-Tabelle:**

| Level | Name / Untertitel | Zellgröße | Boden-Y | Lufthöhe | Ausgangs-Chance je Zelle |
|---|---|---|---|---|---|
| 0 | „Level 0“ / „Die Lobby“ | 8 | 20 | 4 | 1/120 |
| 1 | „Level 1“ / „Das Parkhaus“ | 12 | 50 | 6 | 1/70 |
| 2 | „Level 2“ / „Die Rohre“ | 6 | 80 | 3 | 1/150 |
| Boss | „Boss-Arena“ / „Der Warden“ | – | 110 | 12 | – |

Die Decke liegt bei Boden-Y + Lufthöhe + 1. Das Level eines Ortes wird aus der Y-Höhe bestimmt (Toleranz ±2).

**Labyrinth:**
- Raster aus Zellen. Auf den Rasterlinien (`x % zelle == 0` bzw. `z % zelle == 0`) stehen Wandsegmente, an den Kreuzungen immer Säulen.
- Jedes Segment ist per Hash **offen**, hat eine **Tür** (2 breit, 3 hoch, darüber Wand) oder ist eine **volle Wand**.
  - Level 0: 35 % offen, 40 % Tür
  - Level 1: 45 % offen, 35 % Tür
  - Level 2: 30 % offen, 42 % Tür
- Große offene Hallen: Bereiche von 6×6 Zellen mit 12 % Chance (nicht in Level 2).
- Die Startzelle (0,0) und alle Ausgangszellen haben immer offene Wände. Im Umkreis von 2 Zellen um den Start gibt es keinen Ausgang.
- **Start** in jedem Level: Mitte der Zelle (0,0).
- **Boss-Start:** (0.5, 111, 16.5), Blickrichtung Norden.

**Materialien (helles Design):**
- **Level 0:**
  - Boden: weiße Terrakotta
  - Wände: glatter Sandstein
  - Decke: weißer Beton
  - Licht: 2 Seelaternen in jeder Zellmitte
- **Level 1:**
  - Boden: hellgrauer Beton mit gelben Beton-Linien
  - Wände: weißer Beton
  - Säulen: Quarz
  - Decke: hellgrauer Beton
  - Licht: Seelaternen (90 % der Zellen) plus 4 Zusatzlampen je Zelle
- **Level 2:**
  - Boden: Steinziegel
  - Wände: hellgrauer Beton
  - Decke: grauer Beton
  - Licht: Glowstone in 65 % der Zellen
  - Rohre: Eisengitter eine Reihe unter der Decke
- **Boss-Arena** (x und z von −20 bis +20):
  - Boden: schwarzer Beton mit hellblauen Ringen
  - Wände: dunkles Prismarin mit Seelaternen-Bändern
  - Säulen: 3×3 bei (±10, ±10)
  - Decke: viele Seelaternen

**Ausgang:**
- In der Mitte der Ausgangszelle eine 2 Blöcke hohe **Smaragdblock-Säule**, Glowstone in der Decke darüber.
- Ein **Steinknopf** auf jeder Seite in Höhe Boden + 2. Knopf-Datenwerte: 1 = zeigt nach Osten, 2 = Westen, 3 = Süden, 4 = Norden.
- Ein Rechtsklick auf so einen Knopf in den Backrooms bringt ins nächste Level.

**Weitere Welt-Elemente:**
- **Vorratstruhen:** Ein `BlockPopulator` stellt in neuen Chunks Truhen mit Loot aus der Config auf, am häufigsten in Level 1.
- **Flackerlicht:** Alle 10 Ticks wird mit kleiner Chance eine Lampe in Spielernähe (Level 0 und 1) kurz durch den Deckenblock ersetzt und danach wiederhergestellt. Beim Deaktivieren des Plugins wird alles wiederhergestellt.
- **Unzerstörbar:**
  - Kein Abbauen und kein Bauen. Ausnahmen: Sonder-TNT, und Spieler mit `backrooms.bauen` im Kreativmodus.
  - Explosionen: `blockList().clear()`.
  - Kein Verbrennen, keine Eimer.
  - `CreatureSpawnEvent` ist nur mit dem Grund CUSTOM erlaubt.

## 2. Eigene Gegner als Blockmodelle

Jeder Gegner besteht aus zwei Teilen:
- einem **unsichtbaren** Vanilla-Mob für Bewegung und Trefferfläche: Unsichtbarkeit ohne Partikel, stumm, ohne Ausrüstung, `setRemoveWhenFarAway(false)`, Leben und Tempo über Attribute
- einem **Blockmodell** aus unsichtbaren Rüstungsständern, die einen Block als Helm tragen und **jeden Tick** zur Position des Mobs teleportiert werden. Das Modell dreht sich mit dem Yaw des Mobs.

**Details zum Modell:**
- Ein Helm-Block ist 0,625 Blöcke groß. Die Blockmitte liegt etwa **1,69** Blöcke über den Füßen des Ständers.
- Positionen werden in Modell-Einheiten (0,625) angegeben: rechts / hoch / vor.
  - Vorwärts = (−sin(yaw), cos(yaw))
  - rechts = (−cos(yaw), −sin(yaw))
- **Keine Marker-Ständer für Modellteile verwenden.** Marker werden mit dem Licht an ihren Füßen gezeichnet, und die liegen bei tiefen Teilen im Boden. Die Teile wären dann schwarz.
- Deshalb normale Ständer benutzen und **jeden Schaden an ihnen abbrechen**. Treffer auf ein Modellteil werden an den zugehörigen Mob weitergegeben:
  - Nahkampf: `mob.damage(schaden, spieler)`
  - Pistolenkugel: Pistolenschaden
- `PlayerArmorStandManipulateEvent` abbrechen.
- Alle Plugin-Ständer werden per Metadaten markiert. Nicht markierte Mobs und Ständer in der Backrooms-Welt werden regelmäßig entfernt (nach einem Neustart ohne Metadaten).

**Gegner** (Name, Grund-Mob, Leben, Schaden, Aussehen):

| Gegner | Grund-Mob | Leben | Schaden | Aussehen |
|---|---|---|---|---|
| Tapetenkriecher | Spinne | 10 | 2 | 4 Blöcke glatter Sandstein als flacher Körper, Redstoneblock als Auge vorne, 4 Zäune als Beine |
| Grinser | Zombie | 16 | 3 | schwarze Wolle, Kohleblock als Körper, Arme aus schwarzer Wolle, Kürbislaterne als Kopf |
| Schattenhund | wütender Wolf | 12 | 2 | schwarze Wolle als Körper, Kohleblock als Kopf, graue Wolle als Schwanz, 4 Zaun-Beine |
| Partyballon | Slime, Größe 2 | 12 | 2 | rotes Wollgeschenk, gelbe Schleife, Zaun als Schnur, 4 bunte Woll-Ballons; spielt Noten-Geräusche |
| Rohrgeist | Zombie | 20 | 3 | Eisengitter als Beine und Arme, hellgrauer Beton als Körper, Seelaterne als Kopf |
| **Warden** | Eisengolem | 200 | 5 | etwa 3,5 Blöcke groß: Beine und Rumpf aus dunklem Prismarin, Seelaterne als leuchtende Brust, lange Arme aus türkisem Beton, Kopf aus dunklem Prismarin, Hörner aus Prismarinziegeln mit Seelaternen an den Spitzen |

**Verhalten:**
- Der **Schaden** der Gegner kommt aus der Config, nicht aus den Vanilla-Werten (im `EntityDamageByEntityEvent` setzen).
- Gegner greifen nur Spieler an und kämpfen nicht untereinander.
- Jede Sekunde bekommen sie den nächsten Spieler im Überlebensmodus **im selben Level** als Ziel.
- Slimes teilen sich nicht, Gegner brennen nicht.
- **Spawnen:**
  - alle 8 Sekunden
  - höchstens 3 Gegner im Umkreis von 40 Blöcken um einen Spieler, nur Gegner im selben Level zählen
  - Abstand 14–28 Blöcke, nur auf freiem Boden
  - Tabelle je Level aus der Config: Level 0 = Tapetenkriecher und Grinser, Level 1 = Schattenhund, Partyballon und Tapetenkriecher, Level 2 = Rohrgeist, Grinser und Schattenhund
- Gegner ohne Spieler im selben Level in 64 Blöcken Umkreis werden entfernt.
- **Beute:** 20 % Mandelwasser, 20 % Energieriegel, 10 % Granate.

**Boss-Kampf:**
- Der Warden **spawnt sofort**, wenn ein Spieler die Arena betritt: Blitz-Effekt, Titel „DER WARDEN“.
- Oben am Bildschirm zeigt eine **Boss-Leiste** (`Bukkit.createBossBar`) seine Lebenspunkte.
- **Fähigkeiten** (mild):
  - alle 15 Sekunden 2 Sekunden Blindheit für alle in der Arena
  - alle 8 Sekunden ein „Schallschlag“ auf den nächsten sichtbaren Spieler: Partikelstrahl und 4 Schaden
  - bei halbem Leben ruft er einmalig 2 Schattenhunde
- **Sieg:**
  - Titel „GESCHAFFT!“, jeder bekommt das Warden-Herz
  - nach 5 Sekunden zurück an den gespeicherten Rückkehr-Ort
  - danach 15 Sekunden Pause, bevor ein neuer Warden erscheinen kann
- Ist niemand mehr in der Arena, verschwindet der Warden nach 60 Sekunden.

## 3. Eigene Items

Alle Items tragen einen Namen, eine Beschreibung und eine Markierungszeile in der Lore (`Backrooms-Item: <NAME>`). Daran erkennt das Plugin sie.

**Waffen und Werkzeuge:**
- Das sind **unzerstörbare Diamanthacken** (`setUnbreakable(true)`, `HIDE_UNBREAKABLE`, `HIDE_ATTRIBUTES`).
- Die Haltbarkeit ist die Modell-Nummer für das Resource Pack.

| Nr. | Item | Wirkung |
|---|---|---|
| 1 | Backrooms-Klinge | **30 Angriffsschaden** bei Nahkampf (`ENTITY_ATTACK`/`ENTITY_SWEEP_ATTACK`) |
| 2 | Pistole | Rechtsklick: Schneeball ohne Schwerkraft, Tempo 3, 7 Schaden, 0,35 s Nachladezeit, keine Munition |
| 3 | Bazooka | Rechtsklick: Rakete (Schneeball ohne Schwerkraft mit Flammen- und Rauchspur), Explosion mit 25 Schaden im Radius 4, 2 s Nachladezeit |
| 4 | Taschenlampe | siehe unten |
| 5 | Medkit | Rechtsklick: volle Gesundheit, wird verbraucht |
| 6 | Warden-Herz | Trophäe |
| 7 | Adrenalinspritze | Rechtsklick: 20 Sekunden Tempo II und Sprungkraft II |

**Stapelbare Items** (ihr Bild ersetzt das Resource Pack global):

| Item | Grund-Item | Wirkung |
|---|---|---|
| Mandelwasser | Ghast-Träne | Rechtsklick: +8 Leben, entfernt Blindheit, Übelkeit, Gift, Wither, Langsamkeit |
| Energieriegel | Keks | normal essen; zusätzlich +6 Hunger und 20 Sekunden Tempo |
| Granate | Feuerwerksstern | Rechtsklick: Wurf mit Schwerkraft, Explosion mit 15 Schaden im Radius 3,5 |

Dazu kommt der **Ausgangs-Kompass** (Vanilla-Kompass). Er zeigt per `setCompassTarget` zum nächsten Ausgang. Rechtsklick nennt Entfernung und Himmelsrichtung.

**Explosionen:**
- Kein Blockschaden: nur Partikel, Sound und Schaden an lebenden Entities, abgeschwächt mit dem Abstand.
- Der Schütze, Rüstungsständer und Spieler (wenn PvP aus ist) werden ausgenommen.
- Während des Explosionsschadens ein Flag setzen, damit der Schaden nicht als Klingenschlag mit 30 zählt.

**Taschenlampe:**
- Alle 3 Ticks wird der Block, auf den der Spieler schaut (`getTargetBlock(null, 24)`, nur volle Blöcke), **nur für diesen Spieler** per `sendBlockChange` als Seelaterne angezeigt. Dadurch berechnet sein Client echtes Licht.
- Den vorherigen Block wiederherstellen, sobald sich das Ziel ändert oder die Lampe weggelegt wird.
- End-Rod-Partikel als Lichtstrahl.

**Weitere Regeln für Items:**
- `PlayerInteractEvent` nur für `EquipmentSlot.HAND` auswerten.
- Den Klick bei den eigenen Items abbrechen, damit die Hacke nicht umgräbt. Ausnahmen: Energieriegel, und Klicks auf Knöpfe.
- Backrooms-Items lassen sich nicht als Block platzieren.

## 4. Sonder-TNT (als Items)

Normale TNT-Items mit eigenem Namen und Lore-Markierung `SonderTNT-Typ: <TYP>`. Beim Platzieren wird der Block einen Tick später durch gezündetes TNT ersetzt, der Typ steht als Metadaten am `TNTPrimed`. Aus einem Werfer (Dispenser) funktioniert es genauso.

| Typ | Wirkung |
|---|---|
| Mega | Explosionsstärke 15 |
| Feuer | Stärke 4; setzt zusätzlich Feuer im Radius 5 und zündet Entities an |
| Blitz | Stärke 4; dann 5 Blitze nacheinander im Radius 6. Den Boden von der Explosionshöhe aus suchen, **nicht** mit `getHighestBlockYAt`, sonst schlägt der Blitz auf dem Dach ein |
| Lift | keine Explosion (`ExplosionPrimeEvent` abbrechen); schleudert alles im Radius 7 hoch, einmaliger Fallschutz |
| Cluster | Stärke 4; danach 6 kleine TNT mit Stärke 2 |

## 5. Spielablauf und Anzeige

**`/start`** (für alle Spieler, nur außerhalb der Backrooms):
1. Rückkehr-Ort in `spieler.yml` speichern.
2. Ausrüstung geben:
   - Klinge, Pistole, Bazooka, Taschenlampe, Kompass
   - Diamantrüstung mit Schutz II und Haltbarkeit III, direkt anziehen, wenn der Platz frei ist
   - 2 Medkits, 1 Adrenalinspritze, 5 Mandelwasser, 8 Energieriegel, 5 Granaten
   - je 8 Sonder-TNT jeder Sorte
3. Nach Level 0 teleportieren, mit Titel.
4. Das Resource Pack schicken, falls in der Config eine URL steht.

**Beim Level-Wechsel:** Titel mit Level-Name und Untertitel, Portal-Sound.

**Seitenleiste** (eigenes Scoreboard je Spieler, jede Sekunde aktualisiert):
- Level-Name
- Untertitel
- „Ausgang: 57m“, in der Boss-Arena stattdessen „Warden 150HP“
- „Besiegt: 3“

Über dem nächsten Ausgang (bis 40 Blöcke Entfernung) schwebt ein Marker-Rüstungsständer mit dem Namen „AUSGANG (Knopf drücken)“. In der Nähe steigen grüne Partikel auf.

**Tod in den Backrooms:** Items und Erfahrung behalten, keine Drops. Respawn am Start des aktuellen Levels. In der Boss-Arena erscheint der Warden dann wieder.

**Speicherung:** `spieler.yml` enthält Rückkehr-Ort, besiegte Gegner und Siege.

## 6. Befehle und Rechte

| Befehl | Recht |
|---|---|
| `/start` | `backrooms.start` (Standard: alle) |
| `/backrooms level <0\|1\|2\|boss> [Spieler]` | `backrooms.admin` (OP) |
| `/backrooms verlassen [Spieler]` | `backrooms.admin` |
| `/backrooms item <Spieler> <item> [anzahl]` | `backrooms.admin` |
| `/backrooms gegner <typ>` | `backrooms.admin` |
| `/backrooms info` | `backrooms.admin` |
| `/backrooms reload` | `backrooms.admin` |

- Bei `item` heißen die Sonder-TNT `mega_tnt`, `feuer_tnt`, `blitz_tnt`, `lift_tnt` und `cluster_tnt`.
- `backrooms.bauen` (OP) erlaubt Bauen, aber nur im Kreativmodus.
- Alle Befehle mit **Tab-Vervollständigung**.

## 7. config.yml

Alle Zahlen oben sollen einstellbar sein:
- Resource-Pack-URL
- Startausrüstung
- Waffenschaden, Radien, Nachladezeiten
- Flackerlicht (an/aus, Chance 0,10)
- Gegner: an/aus, Intervall, Grenzen, Abstände, Leben, Schaden, Spawn-Tabelle je Level, Beute
- Boss: Leben, Schaden, Schallschlag-Schaden und -Intervall, Blindheits-Intervall
- Vorratstruhen: Chance je Level und Loot-Liste im Format `MATERIAL:min:max:chance%`. Dazu `BACKROOMS_<ITEM>` und `SONDERTNT_<TYP>`.
- Sonder-TNT-Werte

`/backrooms reload` lädt alles neu.

## 8. Resource Pack (Python-Skript)

Schreibe `resourcepack/erstellen.py`. Es darf nur die Python-Standardbibliothek nutzen: PNG selbst mit `zlib` und `struct` schreiben.

**Ergebnis:** `resourcepack/pack/` und `target/Backrooms-Resourcepack.zip`.

**Inhalt:**
- `pack.mcmeta` mit `"pack_format": 3` (das Format für 1.12).
- `models/item/diamond_hoe.json`:
  - Elternmodell `item/handheld`, Textur `items/diamond_hoe`
  - `overrides` mit `{"damaged":0,"damage":0}` → normales Modell
  - dann für jede Modell-Nummer n = 1…7: `{"damaged":0,"damage":(n-0.5)/1561}` → `item/backrooms/<name>`
  - am Ende `{"damaged":1,"damage":0}` → normales Modell
- `models/item/backrooms/<name>.json`: `item/handheld` für Waffen, `item/generated` für Medkit und Herz.
- `cookie.json`, `ghast_tear.json`, `firework_charge.json` und `snowball.json` ersetzen: Energieriegel, Mandelwasser, Granate und eine gelbe Kugel.
- Texturen in `textures/items/backrooms/` als **16×16-Pixelgrafiken:**
  - Klinge: türkis leuchtend, diagonal wie ein Schwert, goldene Parierstange
  - Pistole: grau, diagonal
  - Bazooka: grünes Rohr mit rotem Ring
  - Taschenlampe: gelber Kopf, Lichtstrahl
  - Medkit: weiß mit rotem Kreuz
  - Warden-Herz: türkises Herz
  - Adrenalinspritze
  - Mandelwasser-Flasche: cremefarben mit blauem Etikett
  - Energieriegel: orange Verpackung
  - Granate: grün
  - Kugel: gelb

## 9. README

Auf Deutsch:
- Build
- Installation (Plugin in den `plugins`-Ordner, Resource Pack per URL oder von Hand)
- Spielablauf
- Level
- Gegner-Tabelle
- Items
- Befehle
- Konfiguration

---

Bitte liefere zuerst eine kurze Übersicht der Klassen und danach alle Dateien vollständig.
