# DeepCrate

Six tiers of chest, a capacity module that lifts every slot from 64 to 512, and paged screens for
the tiers that no longer fit one page. Fabric, Minecraft 1.21.11, Java 21. Needed on the server and
on every client.

## The six crates

Ordered by how dangerous the material is to fetch, not by the usual iron-gold-diamond ladder. Each
tier is the previous crate surrounded by eight of the new material, and adds a row of nine.

| Crate | Material | Slots |
|---|---|---|
| Copper | copper ingot, around a copper chest | 27 |
| Iron | iron ingot | 36 |
| Amethyst | amethyst shard | 45 |
| Prismarine | prismarine crystals | 54 |
| Breeze | breeze rod | 63 |
| Echo | echo shard | 72 |

Two crates of the same tier placed side by side merge into one screen, as chests do, sharing a
single module.

## Capacity

A slot holds 64. A module in the slot hanging off the left edge of the screen raises the whole crate:
128, 256 or 512. The slot shows a greyed plate while it is empty, so it reads as what it is. One module at a time, inserting another hands the previous one back. Pull the module out
and close the screen, and everything above the new capacity drops on the ground.

That last rule is expensive at the top of the ladder, and it is a deliberate choice rather than an
oversight. A full double echo crate losing its 512 module spills 69 000 items, which is a thousand
stacks on one block: the game drops them as whole stacks rather than in the ten-to-thirty
pieces it normally uses, but the tick is still heavy and no player picks all of that back up before
the five-minute despawn. Emptying a crate before pulling its module out is the only safe order.

The three shipped modules stop at 512 by choice, not by capability. A slot can hold up to 32767,
which is where the save format's own count field runs out, and an addon module may ask for any
number up to that.

The count of a slot travels over the network as a variable-length integer, so a large number costs
nothing there. It is the save file that sets the ceiling: the item stack codec of the base game
refuses any count above 99, which is why a crate writes its slots in its own format, the item on one
side and its count on the other. What a hand, a hopper stack or a dropped item can carry is still
64, so taking from a crate hands out one vanilla stack at a time and breaking one drops its content
cut into stacks of 64.

## Rows

Under the capacity module sits a second slot, and it takes a stack of up to sixteen row modules. Each
one adds a row of nine, so a copper crate can end up with as many slots as an echo one, and an echo
crate with twenty-four rows. A row module is a chest surrounded by planks.

Adding or taking one back reopens the screen, because a menu's slot list is fixed once it is built.
That happens at the start of the next tick rather than inside the click, so nothing carried in hand
ends up on the ground. Taking modules back follows the same rule as the capacity module: whatever sat
in the rows that are gone drops at the crate's feet.

On a double crate both halves grow by the same number of rows, one module giving one row on each
side, since the two are shown as a single screen.

A crate is only cut back once the last screen on it closes. Shrinking it under someone else's open
screen would leave their menu holding slots the crate no longer has, and their next tick would ask
for an index past the end.

## Sorting

Two buttons stand above the top left corner of the screen, on the button sprite the rest of the game
uses. One orders by name, the other by how full each pile is; both gather identical stacks into one,
up to whatever the module allows, which is where the free slots come from. Pressing the same button
again reverses it, and the drawing shows which way the next press will go, A over Z or Z over A.

The order is worked out on the player's machine and sent whole, as a list of items. A server holds
no language files, so it cannot know that this player reads Pierre where another reads Stone, and
sorting by a name nobody sees is not sorting. What comes back is a rearrangement of what the crate
already had, so a made-up list costs its sender a messy crate and nothing more.

A pair is sorted as one run of slots rather than one half at a time, so the letters do not start over
at the seam. Modules stay in their own tab, untouched.

## Pages

A page holds four rows at most. Past that the screen splits, and the pages share the rows evenly:
six rows give two pages of three, not one full page and one nearly empty. The page numbers stack down the right edge; there is
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

## Settings

`config/oas/deepcrate.json`, written on first launch and rewritten at every one after, so an option
added by an update shows up there by itself. A value outside its range is pulled back in and the
corrected value is what stays on disk.

| Option | Default | Range | What it decides |
|---|---|---|---|
| `crate.baseCapacity` | 64 | 1 to 32767 | what a slot holds with no capacity module in the crate |
| `crate.limitAutomationWithLithium` | true | true or false | whether a crate answers 64 to automation while lithium is installed |
| `crate.maxRowsPerPage` | 4 | 1 to 6 | rows on one page before the screen splits the crate |
| `crate.rowModuleStackLimit` | 16 | 1 to 20 | row modules one crate takes, which is also the item's stack limit |
| `screen.abbreviateAbove` | 999 | 999 to 2147483647 | the count past which a slot shows 2k instead of 2048 |

The first four are read on the server and reach every client of it. The fifth is read where the
screen is drawn, so it belongs to whoever is looking. Lowering `rowModuleStackLimit` never shrinks a
crate already placed: the rows come from the stack that is actually there, and only a new insertion is
refused.

## For other mods

One entry point, one `register` call and a lang key. Declare your class under `"deepcrate"` in the
`entrypoints` block of your `fabric.mod.json` and implement `DeepCrateAddon`:

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

Eleven things are reachable from outside, and nothing here is a special case the mod keeps for
itself: every shipped tier, module, sort order and tooltip line goes through the same calls.

| Point | Where | What it adds |
|---|---|---|
| `DeepCrateApi.registerTier` | `onDeepCrateInit` | a crate, with its block, its columns and its own rows |
| `DeepCrateApi.registerModule` | `onDeepCrateInit` | a capacity module, pointed at an item tag |
| `DeepCrateApi.registerRowModule` | `onDeepCrateInit` | a module that buys rows rather than capacity |
| `DeepCrateApi.registerModuleSlot` | `onDeepCrateInit` | a cell on the module tab, which grows with it |
| `CrateLayoutCallback.EVENT` | `onDeepCrateInit` | the last word on a crate's columns and rows per page |
| `CrateCapacityCallback.EVENT` | `onDeepCrateInit` | the last word on what a slot holds |
| `ConfigSchema.registerHolder` | `onDeepCrateConfig` | your own options, in the same settings file |
| `ConfigBounds.registerGroup` | `onDeepCrateConfig` | the range each of them is clamped to |
| `DeepCrateClientApi.registerSortOrder` | client init | a sort button, with its icon and its comparator |
| `CrateScreenCallback.EVENT` | client init | a widget on the crate screen |
| `CrateTooltipCallback.EVENT` | client init | a line in the tooltip of a crate slot |

A module points at an item tag rather than at one item, so adding your item to `deepcrate:module_512`
is enough to make it a module, with no code at all.

The screen hands you a `CrateScreenArea`: the panel's corner, its size, a way to add a widget, and
`keepClickable`. Name any rectangle you draw past the edge of the panel through that last one. The
game counts a click outside a container screen as a click into the world, and releasing one there
throws on the ground whatever the player is carrying. The two shipped buttons and the module tab are
named the same way.

`onScreenInit` fires again on every layout, which includes every window resize, and the screen throws
its widgets away between two of those. Add yours again rather than keeping one across calls.

Four public constants are gone, because each of them is now a setting an admin decides:
`DeepCrateApi.BASE_CAPACITY` and `CrateLayout.MAX_ROWS_PER_PAGE` and `RowModule.STACK_LIMIT` read as
`CrateConfig.baseCapacity`, `CrateConfig.maxRowsPerPage` and `CrateConfig.rowModuleStackLimit`, and
`DeepCrateApi.AUTOMATION_LIMITED` as `DeepCrateApi.automationLimited()`. Keeping the first three
deprecated would have been worse than removing them: `javac` copies the value of a compile-time
constant into your class file, so an addon built against them would carry 64, 4 and 16 for good and
compute capacities the server does not have. `DeepCrateApi.MAX_CAPACITY` stays, because a packet
writing a short is not going to change.

Your own options go on a holder class of your own, registered from `onDeepCrateConfig()`, which
`DeepCrateAddon` gives you beside `onDeepCrateInit()`:

```java
    @Override
    public void onDeepCrateConfig() {
        ConfigSchema.registerHolder(MyAddonConfig.class, "myaddon");
        ConfigBounds.registerGroup(MyAddonBounds::register);
    }
```

That pass runs before the settings file is read, which is before any registry is filled. Touch
nothing outside the config package from it: reaching `RegistryInit` there runs its class initialiser
ahead of the read and freezes `rowModuleStackLimit` at its default.

## Build and test

    cd mod
    ./gradlew build          # jar in build/libs/
    ./gradlew test           # storage rules, paging arithmetic, save format
    ./gradlew runClient      # dev client, world "DeepCrate" under run/saves/

    ./gradlew runClientGameTest   # drives a real client and photographs it

`runClientGameTest` builds a fixed scene with commands, a stone platform in cleared air at noon, and
takes its pictures from inside the game rather than off the compositor. They land in
`build/run/clientGameTest/screenshots/`. A chest of the game stands in the same scene as the control
every shot is read against. `mod/scripts/headless-test.sh` starts a dedicated server, or a client
inside an invisible sway session, for the cases those shots do not cover.

## Layout

`mod/` holds the mod, `web/` is reserved for a showcase page. The design notes are in
`docs/superpowers/`.
