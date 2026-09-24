#!/usr/bin/env python3
"""
Erzeugt das Backrooms-Resource-Pack (Minecraft 1.12.2, pack_format 3).

Aufruf (im Projektordner):
    python3 resourcepack/erstellen.py

Ergebnis:
    resourcepack/pack/                    (Inhalt des Packs)
    target/Backrooms-Resourcepack.zip     (fertiges Pack zum Installieren)

Die Bilder werden hier als 16x16-Pixelgrafik beschrieben, es wird nur die
Python-Standardbibliothek benötigt.
"""
import json
import math
import os
import struct
import zipfile
import zlib

HIER = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HIER, "pack")
ZIP = os.path.join(os.path.dirname(HIER), "target", "Backrooms-Resourcepack.zip")

# Diamanthacke hat 1561 Haltbarkeit. Modell-Nummer = Haltbarkeit des Items.
HACKE_MAX = 1561

FARBEN = {
    ".": None,
    "k": (25, 25, 30), "d": (70, 72, 80), "g": (120, 124, 135), "l": (185, 190, 200),
    "w": (245, 245, 250), "y": (250, 215, 60), "Y": (200, 150, 30), "o": (240, 140, 40),
    "r": (215, 45, 45), "R": (140, 25, 25), "b": (130, 85, 45), "B": (80, 50, 25),
    "c": (60, 220, 235), "C": (20, 110, 125), "s": (190, 250, 255), "e": (80, 170, 70),
    "E": (40, 95, 40), "t": (235, 215, 170), "p": (230, 120, 200), "m": (170, 80, 220),
}


# ------------------------------------------------------------------ PNG

def png_schreiben(pfad, bild):
    """bild: 16 Zeilen mit je 16 Zeichen aus FARBEN."""
    hoehe = len(bild)
    breite = len(bild[0])
    roh = b""
    for zeile in bild:
        assert len(zeile) == breite, (pfad, zeile)
        roh += b"\x00"
        for zeichen in zeile:
            farbe = FARBEN[zeichen]
            roh += bytes(farbe + (255,)) if farbe else b"\x00\x00\x00\x00"

    def block(typ, daten):
        inhalt = typ + daten
        return struct.pack(">I", len(daten)) + inhalt + struct.pack(">I", zlib.crc32(inhalt) & 0xFFFFFFFF)

    kopf = struct.pack(">IIBBBBB", breite, hoehe, 8, 6, 0, 0, 0)
    daten = b"\x89PNG\r\n\x1a\n" + block(b"IHDR", kopf) + block(b"IDAT", zlib.compress(roh, 9)) + block(b"IEND", b"")
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "wb") as f:
        f.write(daten)


# ------------------------------------------------------------------ Zeichenhilfen

def leer():
    return [["."] * 16 for _ in range(16)]


def setze(c, x, y, f):
    if 0 <= x < 16 and 0 <= y < 16:
        c[y][x] = f


def diagonal(c, summen, von_x, bis_x):
    """Diagonales Band (von unten links nach oben rechts): Pixel mit x + y = Summe."""
    for summe, farbe in summen.items():
        for x in range(von_x, bis_x + 1):
            setze(c, x, summe - x, farbe)


def fertig(c):
    return ["".join(z) for z in c]


def umrandung(c, farbe="k"):
    """Setzt einen Rand um alle gefüllten Pixel."""
    alt = [z[:] for z in c]
    for y in range(16):
        for x in range(16):
            if alt[y][x] != ".":
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and alt[ny][nx] != ".":
                    c[y][x] = farbe
                    break


# ------------------------------------------------------------------ Bilder

def klinge():
    c = leer()
    diagonal(c, {15: "s", 16: "c", 17: "C"}, 6, 14)       # leuchtende Klinge
    for i, (x, y) in enumerate([(3, 8), (4, 9), (5, 10), (6, 11), (7, 12)]):
        setze(c, x, y, "y" if i != 2 else "Y")              # Parierstange
    diagonal(c, {15: "b", 16: "B"}, 2, 4)                   # Griff
    setze(c, 1, 14, "Y")                                     # Knauf
    umrandung(c)
    return fertig(c)


def pistole():
    c = leer()
    diagonal(c, {15: "l", 16: "d", 17: "d"}, 6, 13)          # Lauf / Schlitten
    setze(c, 14, 1, "k")
    setze(c, 14, 2, "g")                                     # Mündung
    for x, y in [(7, 10), (8, 11), (9, 12), (6, 11), (7, 12), (8, 13)]:
        setze(c, x, y, "B")                                  # Griff
    for x, y in [(8, 12), (7, 11)]:
        setze(c, x, y, "b")
    setze(c, 9, 10, "k")                                     # Abzug
    umrandung(c)
    return fertig(c)


def bazooka():
    c = leer()
    diagonal(c, {14: "e", 15: "e", 16: "E", 17: "E"}, 1, 14)  # Rohr
    diagonal(c, {14: "r", 15: "r", 16: "R", 17: "R"}, 11, 11)  # roter Ring vorne
    diagonal(c, {14: "d", 15: "d", 16: "d", 17: "d"}, 1, 2)    # hinteres Ende
    for x, y in [(9, 10), (10, 11), (8, 11), (9, 12)]:
        setze(c, x, y, "B")                                  # Griff
    setze(c, 6, 6, "d")
    setze(c, 7, 5, "d")                                      # Visier
    umrandung(c)
    return fertig(c)


def taschenlampe():
    c = leer()
    diagonal(c, {15: "g", 16: "d"}, 3, 9)                    # Griff
    setze(c, 6, 9, "r")                                      # Schalter
    diagonal(c, {14: "y", 15: "y", 16: "Y", 17: "Y"}, 10, 11)  # Kopf
    diagonal(c, {14: "s", 15: "w", 16: "w", 17: "s"}, 12, 12)  # Glas
    umrandung(c)
    for x, y in [(15, 0), (14, 0), (15, 1), (13, 0), (15, 2)]:
        setze(c, x, y, "y")                                  # Lichtstrahl
    return fertig(c)


def medkit():
    return [
        "................",
        "................",
        ".....kkkkkk.....",
        ".....k....k.....",
        ".kkkkkkkkkkkkkk.",
        ".kwwwwwwwwwwwwk.",
        ".kwwwwwrrwwwwwk.",
        ".kwwwwwrrwwwwwk.",
        ".kwwwrrrrrrwwwk.",
        ".kwwwrrrrrrwwwk.",
        ".kwwwwwrrwwwwwk.",
        ".kwwwwwrrwwwwwk.",
        ".kllllllllllllk.",
        ".kkkkkkkkkkkkkk.",
        "................",
        "................",
    ]


def wardenherz():
    c = leer()
    for y in range(16):
        for x in range(16):
            # Herzkurve, skaliert auf 16x16
            u = (x - 7.5) / 5.3
            v = (6.6 - y) / 4.6
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                if u < -0.1 and v > 0.2:
                    c[y][x] = "s"
                elif u > 0.3 or v < -0.4:
                    c[y][x] = "C"
                else:
                    c[y][x] = "c"
    umrandung(c)
    return fertig(c)


def adrenalin():
    c = leer()
    diagonal(c, {15: "l"}, 11, 14)                           # Nadel
    setze(c, 15, 0, "w")
    diagonal(c, {15: "s", 16: "w"}, 4, 10)                   # Glaskörper
    diagonal(c, {15: "m", 16: "p"}, 6, 10)                   # Flüssigkeit
    diagonal(c, {15: "g", 16: "g"}, 2, 3)                    # Kolben
    for x, y in [(0, 14), (1, 15), (2, 12), (3, 13)]:
        setze(c, x, y, "d")                                  # Griffplatte
    umrandung(c)
    return fertig(c)


def mandelwasser():
    return [
        "................",
        "......kkkk......",
        "......kbbk......",
        ".....kkkkkk.....",
        ".....kslllk.....",
        "....kslllllk....",
        "...kslttttllk...",
        "...kstttttttk...",
        "...ksccccccck...",
        "...kscwwwwcck...",
        "...ksccccccck...",
        "...ksttttttlk...",
        "...ksttttttlk...",
        "....kkkkkkkk....",
        "................",
        "................",
    ]


def energieriegel():
    return [
        "................",
        "................",
        "................",
        "................",
        "................",
        ".kkkkkkkkkkkkkk.",
        "lkooooooooooookl",
        "lkoyyyyyyyyyyokl",
        "lkoyrrryyrrryokl",
        "lkoyyyyyyyyyyokl",
        "lkooooooooooookl",
        ".kkkkkkkkkkkkkk.",
        "................",
        "................",
        "................",
        "................",
    ]


def granate():
    return [
        "................",
        "................",
        "......kkkk.lll..",
        "......kggk.l.l..",
        ".....kkggkklll..",
        "....kEEEEEEk....",
        "...kEeEeEeEEk...",
        "...kEEEEEEEEk...",
        "...kEeEeEeEEk...",
        "...kEEEEEEEEk...",
        "...kEeEeEeEEk...",
        "....kEEEEEEk....",
        ".....kkkkkk.....",
        "................",
        "................",
        "................",
    ]


def kugel():
    c = leer()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 3.2:
                c[y][x] = "w" if (x < 7 and y < 7 and d > 1.2) else ("y" if d < 2.2 else "Y")
    umrandung(c, "o")
    return fertig(c)


# ------------------------------------------------------------------ Modelle

HACKEN_ITEMS = [
    # (Modell-Nummer, Name, Elternmodell)
    (1, "klinge", "item/handheld"),
    (2, "pistole", "item/handheld"),
    (3, "bazooka", "item/handheld"),
    (4, "taschenlampe", "item/handheld"),
    (5, "medkit", "item/generated"),
    (6, "wardenherz", "item/generated"),
    (7, "adrenalin", "item/handheld"),
]

# Seltene Vanilla-Items, deren Bild ersetzt wird (für stapelbare Items)
ERSETZT = {
    "cookie": "energieriegel",
    "ghast_tear": "mandelwasser",
    "firework_charge": "granate",
    "snowball": "kugel",
}

BILDER = {
    "klinge": klinge, "pistole": pistole, "bazooka": bazooka, "taschenlampe": taschenlampe,
    "medkit": medkit, "wardenherz": wardenherz, "adrenalin": adrenalin,
    "mandelwasser": mandelwasser, "energieriegel": energieriegel, "granate": granate, "kugel": kugel,
}


def json_schreiben(pfad, daten):
    os.makedirs(os.path.dirname(pfad), exist_ok=True)
    with open(pfad, "w", encoding="utf-8") as f:
        json.dump(daten, f, indent=2)
        f.write("\n")


def main():
    json_schreiben(os.path.join(PACK, "pack.mcmeta"), {
        "pack": {"pack_format": 3, "description": "Backrooms - Items (Pistole, Bazooka, Klinge ...)"}
    })
    modelle = os.path.join(PACK, "assets", "minecraft", "models", "item")
    texturen = os.path.join(PACK, "assets", "minecraft", "textures", "items", "backrooms")

    for name, funktion in BILDER.items():
        bild = funktion()
        assert len(bild) == 16, name
        png_schreiben(os.path.join(texturen, name + ".png"), bild)
        print("== " + name)
        print("\n".join(bild))

    # Diamanthacke: eigene Modelle je Haltbarkeit (nur für unzerstörbare Items)
    overrides = [{"predicate": {"damaged": 0, "damage": 0}, "model": "item/diamond_hoe"}]
    for nummer, name, eltern in HACKEN_ITEMS:
        schwelle = (nummer - 0.5) / HACKE_MAX
        overrides.append({"predicate": {"damaged": 0, "damage": round(schwelle, 8)},
                          "model": "item/backrooms/" + name})
        json_schreiben(os.path.join(modelle, "backrooms", name + ".json"), {
            "parent": eltern, "textures": {"layer0": "items/backrooms/" + name}
        })
    overrides.append({"predicate": {"damaged": 1, "damage": 0}, "model": "item/diamond_hoe"})
    json_schreiben(os.path.join(modelle, "diamond_hoe.json"), {
        "parent": "item/handheld",
        "textures": {"layer0": "items/diamond_hoe"},
        "overrides": overrides,
    })

    for vanilla, name in ERSETZT.items():
        json_schreiben(os.path.join(modelle, vanilla + ".json"), {
            "parent": "item/generated", "textures": {"layer0": "items/backrooms/" + name}
        })

    os.makedirs(os.path.dirname(ZIP), exist_ok=True)
    with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_DEFLATED) as z:
        for wurzel, _, dateien in os.walk(PACK):
            for datei in sorted(dateien):
                voll = os.path.join(wurzel, datei)
                z.write(voll, os.path.relpath(voll, PACK))
    print("Fertig: " + ZIP)


if __name__ == "__main__":
    main()
