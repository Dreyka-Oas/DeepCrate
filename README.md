# DeepCrate

Six tiers of chest, a capacity module that lifts every slot from 64 to 1024, and paged screens for
the tiers that no longer fit one page. Fabric, Minecraft 1.21.11, Java 21. Needed on the server and
on every client.

## The six crates

Ordered by how dangerous the material is to fetch, not by the usual iron-gold-diamond ladder. Each
tier is the previous crate surrounded by eight of the new material, and adds a row of nine.

| Crate | Material | Slots |
|---|---|---|
| Copper | copper ingot, around a chest | 27 |
| Iron | iron ingot | 36 |
| Amethyst | amethyst shard | 45 |
| Prismarine | prismarine crystals | 54 |
| Breeze | breeze rod | 63 |
| Echo | echo shard | 72 |

Two crates of the same tier placed side by side merge into one screen, as chests do, sharing a
single module.

## Capacity

A slot holds 64. A module in the slot hanging off the left edge of the screen raises the whole crate:
128, 256, 512 or 1024. The slot shows a greyed plate while it is empty, so it reads as what it is. One module at a time, inserting another hands the previous one back. Pull the module out
and close the screen, and everything above the new capacity drops on the ground.

That last rule is expensive at the top of the ladder, and it is a deliberate choice rather than an
oversight. A full double echo crate losing its 1024 module spills 138 000 items, which is two
thousand stacks on one block: the game drops them as whole stacks rather than in the ten-to-thirty
pieces it normally uses, but the tick is still heavy and no player picks all of that back up before
the five-minute despawn. Emptying a crate before pulling its module out is the only safe order.

The count of a slot travels over the network as a variable-length integer, so a large number costs
nothing there. It is the save file that sets the ceiling: the item stack codec of the base game
refuses any count above 99, which is why a crate writes its slots in its own format, the item on one
side and its count on the other. What a hand, a hopper stack or a dropped item can carry is still
64, so taking from a crate hands out one vanilla stack at a time and breaking one drops its content
cut into stacks of 64.

## Pages

Past six rows the screen splits, and the pages share the rows evenly: eight rows give two pages of
four, not one full page and one nearly empty. The page numbers stack down the right edge; there is
no scrollbar. Shift-clicking reaches every slot, including the pages that are not open.

Paging is a client-side view. The page number never reaches the server, so no server-side slot index
can be steered from outside, which is what would open the door to duplicated items.

## Hoppers

Hoppers and pipes fill a crate to its full capacity rather than stopping at 64. That takes a patch to
the hopper of the base game, and it is the one part of the mod that depends on what else is
installed.

With lithium, it is off. Lithium replaces the hopper wholesale and keeps its own copy of the target
inventory; against a container that answers more than 64 it takes items out of the hopper and never
writes them in. Measured, not assumed: eight blocks of dirt destroyed per run, with or without the
patch. So when lithium is present a crate tells automation 64 a slot, which costs the feature and
keeps the items. Hands are unaffected either way, they go through the screen.

A hopper against a double crate reaches the half it touches, as it does with a barrel. Joining the
two halves there is what vanilla does for chests, but lithium casts that result back to a block
entity and crashes the server on the first tick.

## For other mods

Three things, because the ask named three. Add a module, add a tier, change the slot and page counts.

```java
public class MyAddon implements DeepCrateAddon {
    @Override
    public void onDeepCrateInit() {
        DeepCrateApi.registerTier(new CrateTier(id("my_crate"), 9, MY_BLOCK));
        DeepCrateApi.registerModule(new CrateModule(id("my_module"), 4096, MY_TAG));
        CrateLayoutCallback.EVENT.register((tier, rows, layout) -> new CrateLayout(3, (rows + 2) / 3));
    }
}
```

Declare the class under `"deepcrate"` in the `entrypoints` block of your `fabric.mod.json`. A module
points at an item tag rather than at one item, so adding your item to `deepcrate:module_512` is
enough to make it a module, with no code at all.

## Build and test

    cd mod
    ./gradlew build          # jar in build/libs/
    ./gradlew test           # storage rules, paging arithmetic, save format
    ./gradlew runClient      # dev client, world "DeepCrate" under run/saves/

`mod/scripts/headless-test.sh` starts a dedicated server, or a client inside an invisible sway
session, and takes screenshots. The screenshot half does not work here yet: Loom launches the game
from the Gradle daemon, which hands it the desktop compositor rather than the headless one, so the
window opens on the real screen and the capture comes back black. The server half works and is what
the in-game checks used.

## Layout

`mod/` holds the mod, `web/` is reserved for a showcase page. The design notes are in
`docs/superpowers/`.
