#!/usr/bin/env python3
"""Builds the eighteen chest textures of the mod out of the block textures of the game.

Run it from the mod directory, with the block textures of the client jar unpacked where BLOCKS
points and the chest of the game beside them, which the lock mapping is checked against:

    python3 scripts/make_chest_textures.py

The point of this file is the sampling. A crate is not one box but three, and each lays its six
faces on the sheet at rectangles that depend on its own size. Painting every rectangle from the top
of the material makes the lid show rows 0 to 4 of it and the body show rows 0 to 9 again, so the same
pixels appear twice on one surface.

Worse, the lid runs from 9 to 14 and the body from 0 to 10: they share a row, and there two faces sit
in exactly the same plane. Nothing can decide which of the two a pixel belongs to, so the pair
flickers as the camera moves unless both carry the same colour. The chest of the game has the same
row shared and paints it the same on both, which is why its own rows 14 and 42 are identical.

So the material is sampled along the crate rather than along the rectangle. The first row of a side
face is the low end of its box, so the body reads rows 0 to 9 of a fourteen row column and the lid
rows 9 to 13, and row 9 lands on both sides of the shared plane. Sideways nothing is offset, because
a crate is made of one block's material and two of those side by side repeat in the world as well.
"""
import sys
import zipfile
from pathlib import Path

from PIL import Image

ASSETS = Path("src/main/resources/assets/deepcrate/textures/entity/chest")
UNPACKED = Path("/tmp/mcassets")
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.11/minecraft-client.jar"
BLOCKS = UNPACKED / "assets/minecraft/textures/block"
VANILLA = UNPACKED / "assets/minecraft/textures/entity/chest"

TIERS = {
    "coal": "coal_block",
    "copper": "copper_block",
    "iron": "iron_block",
    "redstone": "redstone_block",
    "lapis": "lapis_block",
    "gold": "gold_block",
    "amethyst": "amethyst_block",
    "quartz": "quartz_block_side",
    "emerald": "emerald_block",
    "diamond": "diamond_block",
    "netherite": "netherite_block",
}

# The frame the game draws around every face of a chest, and how dark the two faces you only ever
# see with the lid up are.
FRAME = 0.78
INSIDE = 0.42

LOCK_DARK = (0x6C, 0x6C, 0x70, 0xFF)
LOCK_LIGHT = (0xAA, 0xAA, 0xAE, 0xFF)

DEPTH = 14
LID_HEIGHT = 5
BODY_HEIGHT = 10
SINGLE_WIDTH, HALF_WIDTH = 14, 15


def faces(u0, v0, width, height):
    """Where the six faces of a box of this size land, given the offset it was declared with."""
    return {
        "down": (u0 + DEPTH, v0, width, DEPTH),
        "up": (u0 + DEPTH + width, v0, width, DEPTH),
        "west": (u0, v0 + DEPTH, DEPTH, height),
        "north": (u0 + DEPTH, v0 + DEPTH, width, height),
        "east": (u0 + DEPTH + width, v0 + DEPTH, DEPTH, height),
        "south": (u0 + DEPTH + width + DEPTH, v0 + DEPTH, width, height),
    }


def paint(image, material, rectangle, row, inside=False, seam=None):
    """One face, read from the material at the given row of the crate, framed, dimmed if inward.

    seam names the column where the other half of the same crate is joined, 'first' or 'last'. Its
    frame is left off so the two halves run into one another rather than reading as two crates
    pushed together, which is what the chest of the game does at the same edge.
    """
    px = image.load()
    mx = material.load()
    u0, v0, width, height = rectangle
    for y in range(height):
        for x in range(width):
            red, green, blue, _ = mx[x % material.width, (row + y) % material.height]
            framed = y in (0, height - 1) or (x == 0 and seam != "first") or (x == width - 1 and seam != "last")
            shade = (FRAME if framed else 1.0) * (INSIDE if inside else 1.0)
            px[u0 + x, v0 + y] = (round(red * shade), round(green * shade), round(blue * shade), 255)


# The six columns of a single crate's lock: metal, lit down one side, dark along the bottom.
LOCK_COLUMNS = (
    [LOCK_DARK] * 4,
    [LOCK_DARK] * 4,
    [LOCK_LIGHT, LOCK_LIGHT, LOCK_LIGHT, LOCK_DARK],
    [LOCK_DARK] * 4,
    [LOCK_DARK] * 4,
    [LOCK_LIGHT, LOCK_LIGHT, LOCK_LIGHT, LOCK_DARK],
)

# A half's lock is one pixel wide, not two, so its six faces land on different pixels than a
# single's. Each entry says which columns of a single lock the four columns of a half take, and
# None is the side the other half covers, which the game leaves blank.
HALF_LOCK = {"left": (None, 2, 3, 4), "right": (0, 1, None, 5)}


def paint_lock(image, half=None):
    px = image.load()
    for x in range(6):
        for y in range(5):
            px[x, y] = (0, 0, 0, 0)

    columns = LOCK_COLUMNS if half is None else [None if c is None else LOCK_COLUMNS[c] for c in HALF_LOCK[half]]
    for cap in range(1, 3 if half else 5):
        px[cap, 0] = LOCK_DARK

    for x, column in enumerate(columns):
        if column is None:
            continue
        for row, colour in enumerate(column):
            px[x, row + 1] = colour


# Which column of each face touches the other half. The front is the one face whose pixels run
# against the x axis, so its joined edge is the opposite end from the other three.
SEAMS = {
    None: {},
    "left": {"north": "first", "up": "first", "down": "first", "south": "last"},
    "right": {"north": "last", "up": "last", "down": "last", "south": "first"},
}


def build(material, width, half=None):
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    lid = faces(0, 0, width, LID_HEIGHT)
    body = faces(0, 19, width, BODY_HEIGHT)
    seams = SEAMS[half]

    paint(image, material, lid["down"], 0, inside=True, seam=seams.get("down"))
    paint(image, material, lid["up"], 0, seam=seams.get("up"))
    paint(image, material, body["down"], 0, seam=seams.get("down"))
    paint(image, material, body["up"], 0, inside=True, seam=seams.get("up"))

    for side in ("west", "north", "east", "south"):
        seam = seams.get(side)
        paint(image, material, body[side], 0, seam=seam)
        # The first row of a side face is the low end of its box, so the lid starts nine rows up,
        # where the body's last row is: the one row the two boxes share.
        paint(image, material, lid[side], BODY_HEIGHT - 1, seam=seam)

    return image


SHARED_ROWS = (DEPTH, 19 + DEPTH + BODY_HEIGHT - 1)


def check_shared_row(image, what):
    """The two faces that sit in the same plane must carry the same pixels, or the pair flickers."""
    px = image.load()
    top, bottom = SHARED_ROWS
    disagree = [x for x in range(64) if px[x, top] != px[x, bottom]]
    if disagree:
        raise SystemExit(f"{what}: rows {top} and {bottom} differ at columns {disagree[:8]}")


def check_lock_mapping():
    """The mapping from a single lock to a half's is not invented, so it is checked against the game."""
    single = Image.open(VANILLA / "normal.png").convert("RGBA").load()
    caps = {"left": (2, 4), "right": (1, 3)}
    for half, sources in HALF_LOCK.items():
        expected = Image.open(VANILLA / f"normal_{half}.png").convert("RGBA").load()
        wanted = {(1, 0): single[caps[half][0], 0], (2, 0): single[caps[half][1], 0]}
        for x, source in enumerate(sources):
            if source is not None:
                for row in range(4):
                    wanted[(x, row + 1)] = single[source, row + 1]

        for x in range(6):
            for y in range(5):
                if wanted.get((x, y), (0, 0, 0, 0)) != expected[x, y]:
                    raise SystemExit(f"the lock mapping disagrees with the {half} half of the chest of the game")


def unpack_what_the_game_gives_us():
    if BLOCKS.is_dir() and VANILLA.is_dir():
        return

    if not CLIENT_JAR.is_file():
        raise SystemExit(f"no client jar at {CLIENT_JAR}; run a gradle task once so loom fetches it")

    wanted = [f"assets/minecraft/textures/block/{block}.png" for block in TIERS.values()]
    wanted += [f"assets/minecraft/textures/entity/chest/normal{half}.png" for half in ("", "_left", "_right")]
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        jar.extractall(UNPACKED, members=wanted)


def main():
    unpack_what_the_game_gives_us()
    check_lock_mapping()
    # The rule comes from the chest of the game, which follows it everywhere it shows: the two
    # pixels where its own right half breaks it are on the edge the other half covers.
    check_shared_row(Image.open(VANILLA / "normal.png").convert("RGBA"), "the chest of the game")

    ASSETS.mkdir(parents=True, exist_ok=True)

    for tier, block in TIERS.items():
        material = Image.open(BLOCKS / f"{block}.png").convert("RGBA")
        for suffix, width in (("", SINGLE_WIDTH), ("_left", HALF_WIDTH), ("_right", HALF_WIDTH)):
            half = suffix[1:] or None
            image = build(material, width, half)
            paint_lock(image, half)
            check_shared_row(image, f"{tier}{suffix}")
            image.save(ASSETS / f"{tier}_crate{suffix}.png")

        print(f"{tier}: single crate and both halves")


if __name__ == "__main__":
    sys.exit(main())
