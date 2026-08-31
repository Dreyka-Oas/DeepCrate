# DeepCrate

An iron chest whose 27 slots each hold 128 items instead of 64. Fabric, Minecraft 1.21.11,
Java 21. Needed on the server and on every client.

## Why 128 and not more

The count of a slot travels over the network as a variable-length integer, so a large number is
free there. It is the save file that sets the ceiling: the item stack codec of the base game
refuses any count above 99, which is why a crate writes its slots in its own format, the item on
one side and its count on the other (`StoredSlot`). The number itself could be raised without
touching anything else; 128 is a choice, not a limit.

What a hand, a hopper or a dropped item can carry is still 64. Taking from a crate therefore
hands out one vanilla stack at a time, and breaking a crate drops its content cut into stacks of
64.

## Automation

Hoppers and pipes see a crate through the usual container interface and fill it to 64 per slot,
because the base game's merge test compares against the item's own stack size. Filling a slot to
128 is done by hand, or by shift-clicking from the inventory. Lifting that would mean patching
the hopper itself, which the optimisation mods people run also patch.

## Build and test

    cd mod
    ./gradlew build          # jar in build/libs/
    ./gradlew test           # storage rules and the save format
    ./gradlew runClient      # dev client, world "DeepCrate" under run/saves/

`mod/scripts/headless-test.sh` starts a dedicated server, or a client inside an invisible sway
session, and takes screenshots. The screenshot half does not work here yet: Loom launches the game
from the Gradle daemon, which hands it the desktop compositor rather than the headless one, so the
window opens on the real screen and the capture comes back black. The server half works and is
what the in-game checks used.

## Layout

`mod/` holds the mod, `web/` is reserved for a showcase page. The design note is in
`docs/superpowers/specs/`.
