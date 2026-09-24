# Backrooms

Backrooms-Abenteuer als Plugin für **Spigot / Paper 1.12.2** (Java 8), mit eigenem
**Resource Pack** für die Item-Bilder.

- Eigene Gegner und ein Warden-Endboss, gebaut als **Blockmodelle**: unsichtbare
  Rüstungsständer tragen Blöcke und bewegen sich mit einem unsichtbaren Mob mit.
  Dafür braucht man kein Resource Pack.
- Eigene Items (Pistole, Bazooka, Backrooms-Klinge, Taschenlampe …) mit eigenen Bildern
  aus dem **Backrooms-Resource-Pack**. Ohne Pack funktionieren alle Items genauso, sie
  sehen dann nur wie das Grund-Item aus (z. B. eine Diamanthacke).

## Build

Voraussetzungen: JDK 8 oder neuer, Maven 3, Python 3 (nur für das Resource Pack).

```bash
mvn clean package                  # Plugin  -> target/Backrooms-3.0.0.jar
python3 resourcepack/erstellen.py  # Pack    -> target/Backrooms-Resourcepack.zip
```

## Installation

1. `Backrooms-3.0.0.jar` in den Ordner `plugins/` des Spigot/Paper-**1.12.2**-Servers legen
   (eine ältere `Backrooms-*.jar` oder `SonderTNT-*.jar` vorher löschen).
2. Server neu starten. Beim ersten Start wird die Welt `backrooms` erzeugt.
3. **Resource Pack** – eine der beiden Möglichkeiten:
   - **Automatisch:** `Backrooms-Resourcepack.zip` irgendwo hochladen, wo es einen
     *direkten* Download-Link gibt, und den Link in `plugins/Backrooms/config.yml` unter
     `resourcepack.url` eintragen. Spieler bekommen das Pack dann beim Betreten der
     Backrooms angeboten.
   - **Von Hand:** Jeder Spieler legt die ZIP-Datei in seinen `resourcepacks`-Ordner
     und aktiviert sie unter *Optionen → Ressourcenpakete*.

## Spielablauf

1. **`/start`** – Ausrüstung bekommen und ab in **Level 0**.
2. In jedem Level die **Smaragd-Säule** finden und den **Knopf** daran drücken.
   Das führt ins nächste Level.
   - Der **Ausgangs-Kompass** zeigt zur nächsten Säule. Rechtsklick nennt Entfernung und Richtung.
   - In der Nähe schwebt über der Säule ein Schild „AUSGANG (Knopf drücken)“.
   - Rechts am Bildschirm stehen Level, Entfernung zum Ausgang und besiegte Gegner.
3. **Level 0 → Level 1 → Level 2 → Boss-Arena.** Beim Betreten der Arena erscheint der
   **Warden** sofort, mit Boss-Leiste oben am Bildschirm.
4. Warden besiegt: Du bekommst das **Warden-Herz** und kehrst nach 5 Sekunden an den Ort
   zurück, an dem du `/start` benutzt hast.

**Tod:** Du behältst alle Items und startest am Anfang des **aktuellen** Levels neu.
Die Backrooms sind unzerstörbar: kein Abbauen, kein Bauen, Explosionen zerstören keine Blöcke.

### Level (helles Design)

| Level      | Aussehen                                                  | Gegner                                   |
|------------|-----------------------------------------------------------|------------------------------------------|
| Level 0    | „Die Lobby“: hellgelbe Wände, weiße Decke, Neonröhren     | Tapetenkriecher, Grinser                 |
| Level 1    | „Das Parkhaus“: weißer/grauer Beton, gelbe Linien, Vorratstruhen | Schattenhund, Partyballon, Tapetenkriecher |
| Level 2    | „Die Rohre“: enge Gänge mit Rohren unter der Decke        | Rohrgeist, Grinser, Schattenhund          |
| Boss-Arena | Halle aus dunklem Prismarin mit Lichtbändern             | Warden                                    |

### Gegner (eigene Blockmodelle)

| Gegner          | Aussehen                                                  | Leben | Schaden |
|-----------------|-----------------------------------------------------------|-------|---------|
| Tapetenkriecher | flacher Körper in Tapetenfarbe, rotes Auge, Zaun-Beine; klettert Wände hoch | 10 | 2 |
| Grinser         | schwarze Gestalt mit leuchtendem Kürbisgrinsen             | 16    | 3       |
| Schattenhund    | schwarzer Hund, schnell                                    | 12    | 2       |
| Partyballon     | hüpfendes Geschenk mit bunten Ballons und Musiknoten       | 12    | 2       |
| Rohrgeist       | Körper aus Beton und Rohren, leuchtender Kopf              | 20    | 3       |
| **Warden**      | etwa 3,5 Blöcke groß, dunkeltürkis, leuchtende Brust und Hörner | 200 | 5 |

Schaden: 2 = 1 Herz, der Wert gilt vor der Rüstung.

**Warden-Fähigkeiten** (bewusst nicht zu schwer):
- alle 15 Sekunden 2 Sekunden **Dunkelheit** (Blindheit)
- alle 8 Sekunden ein **Schallschlag** mit 4 Schaden
- bei halbem Leben ruft er einmalig **2 Schattenhunde**

Alle Werte stehen in der `config.yml`.

### Items

| Item              | Wirkung                                                              |
|-------------------|----------------------------------------------------------------------|
| Backrooms-Klinge  | Schwert mit **30 Angriffsschaden**                                    |
| Pistole           | Rechtsklick: Schuss (7 Schaden), keine Munition, kurze Nachladezeit   |
| Bazooka           | Rechtsklick: Rakete, Explosion mit 25 Schaden im Umkreis, keine Blockschäden |
| Granate           | Rechtsklick: werfen, Explosion mit 15 Schaden                        |
| Taschenlampe      | In der Hand: Lichtkegel. Der Punkt, auf den du schaust, leuchtet (nur für dich sichtbar) |
| Medkit            | Rechtsklick: volle Gesundheit                                        |
| Mandelwasser      | Rechtsklick: heilt 4 Herzen, entfernt Blindheit/Gift/Langsamkeit     |
| Energieriegel     | Essen: satt und 20 Sekunden schnell                                  |
| Adrenalinspritze  | Rechtsklick: 20 Sekunden schneller laufen und höher springen         |
| Ausgangs-Kompass  | zeigt zur nächsten Ausgangs-Säule                                    |
| Warden-Herz       | Trophäe für den Sieg                                                 |
| Sonder-TNT        | Mega, Feuer, Blitz, Lift, Cluster (zünden sofort beim Platzieren)    |

**Wie das Resource Pack die Items zeigt:**
- Waffen und Werkzeuge sind unzerstörbare Diamanthacken mit festen Modell-Nummern (1–7).
  Normale Diamanthacken bleiben normal.
- Mandelwasser, Energieriegel, Granate und die Pistolenkugel ersetzen das Bild von
  Ghast-Träne, Keks, Feuerwerksstern und Schneeball.

### /start gibt

- Backrooms-Klinge, Pistole, Bazooka, Taschenlampe, Ausgangs-Kompass
- Diamantrüstung (Schutz II, Haltbarkeit III), wird direkt angezogen, wenn frei
- 2 Medkits, 1 Adrenalinspritze, 5 Mandelwasser, 8 Energieriegel, 5 Granaten
- je 8 Sonder-TNT jeder Sorte

`/start` funktioniert nur außerhalb der Backrooms, also einmal pro Durchgang.

## Befehle

| Befehl                                       | Recht                     | Beschreibung                             |
|----------------------------------------------|---------------------------|------------------------------------------|
| `/start`                                     | `backrooms.start` (alle)  | Ausrüstung + ab in Level 0               |
| `/backrooms level <0\|1\|2\|boss> [Spieler]` | `backrooms.admin` (OP)    | in ein Level teleportieren               |
| `/backrooms verlassen [Spieler]`             | `backrooms.admin`         | Backrooms verlassen                       |
| `/backrooms item <Spieler> <item> [anzahl]`  | `backrooms.admin`         | Items geben (z. B. `pistole`, `bazooka`, `mega_tnt`) |
| `/backrooms gegner <typ>`                    | `backrooms.admin`         | Gegner an der eigenen Position spawnen    |
| `/backrooms info`                            | `backrooms.admin`         | wer ist in welchem Level                  |
| `/backrooms reload`                          | `backrooms.admin`         | `config.yml` neu laden                    |

`backrooms.bauen` (OP) erlaubt Bauen in den Backrooms, aber nur im Kreativmodus.
Alle Befehle haben Tab-Vervollständigung.

## Konfiguration

Alles steht kommentiert in `plugins/Backrooms/config.yml`, zum Beispiel:
- Resource-Pack-Link
- Startausrüstung
- Waffenschaden und Nachladezeiten
- Gegner: Leben, Schaden, Häufigkeit je Level, Beute
- Warden
- Flackerlicht
- Vorratstruhen-Loot
- Sonder-TNT

Bitte in der Datei keine Umlaute verwenden. Nach Änderungen `/backrooms reload` ausführen.

## Getestet

Getestet auf Paper 1.12.2 (Build 1620) mit Java 8 und einem 1.12.2-Testclient (ohne Grafik):

- `/start` mit Ausrüstung
- Knopf-Ausgänge durch alle Level
- Warden mit Modell und Boss-Leiste
- Klinge (30 Schaden), Pistole, Bazooka, Granate
- Medkit, Mandelwasser, Taschenlampen-Lichtfleck
- Gegner-Modelle und Treffer auf Modellteile
- Sieg mit Warden-Herz und Rückkehr

Wie die Blockmodelle und die Item-Bilder im Spiel **aussehen**, konnte ich ohne echten
Spiel-Client nicht ansehen.
