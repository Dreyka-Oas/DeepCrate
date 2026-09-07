# DeepCrate

Eleven tiers of chest, capacity modules that lift a slot from 64 up to 512, and paged screens for the
tiers that no longer fit one page. A Fabric mod, needed on the server and on every client.

## The eleven crates

Ordered by how deep the material sits rather than by the usual iron-gold-diamond ladder. Each tier is
the previous crate surrounded by eight blocks of the new material, and adds a row of nine. Only the
first starts from something else, a chest of the game.

| Crate | Material | Slots |
|---|---|---|
| Coal | block of coal, around a chest | 27 |
| Copper | block of copper | 36 |
| Iron | block of iron | 45 |
| Redstone | block of redstone | 54 |
| Lapis | block of lapis lazuli | 63 |
| Gold | block of gold | 72 |
| Amethyst | block of amethyst | 81 |
| Quartz | block of quartz | 90 |
| Emerald | block of emerald | 99 |
| Diamond | block of diamond | 108 |
| Netherite | block of netherite | 117 |

Two crates of the same tier placed side by side merge into one screen, as chests do, sharing a
single module.

## Capacity

A slot holds 64, and what changes that sits on the tab hanging off the left edge of the screen. The
tab is a column of cells rather than a fixed pair: the mod ships two, one taking a single capacity
module and one taking a stack of sixteen row modules, and another mod adds its own through
`registerModuleSlot`. A cell shows a greyed plate while it is empty, so it reads as what it is.

Which cell an item sits in no longer decides what it does. A capacity module raises the whole crate
from wherever it is put, and the crate takes the strongest offer rather than the sum: two 256 modules
still give 256, a 256 next to a 512 gives 512. Rows follow the opposite rule and add up, which is the
one difference between the two.

Pull the modules out and close the screen, and everything above the new capacity drops on the ground.
What that costs on a large crate, and why the order was kept anyway, is measured in
`DEFAUTS-CONNUS.md`.

The three shipped modules stop at 512 by choice, not by capability. A slot goes up to 32767 and an
addon module may ask for any number up to that. The ceiling is the menu's: the current capacity
reaches the client through a data slot, and that packet writes a short. The save file is bounded by
the same number, its `Count` field being an integer with that range.

The count of a slot travels over the network as a variable-length integer, so a large number costs
nothing there. What shapes the save is the item stack codec of the base game, which refuses any count
above 99: a crate therefore writes its slots in its own format, the item on one side and its count on
the other. What a hand, a hopper stack or a dropped item can carry is still 64, so taking from a crate
hands out one vanilla stack at a time and breaking one drops its content cut into stacks of 64.

## Rows

A row module is a chest surrounded by planks, and each one adds a row of nine to the crate holding it.
The cell under the capacity one takes a stack of sixteen, so a coal crate can end up with more slots
than a netherite one, and a netherite crate with twenty-nine rows. Rows are read from every cell and
added together, so a cell an addon put there holding row modules extends the crate further rather
than replacing what the first gave.

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

`/deepcrateconfig` opens them in game, and that is the way in. The screen has a search box across the
top, the categories down the left and one row per option on the right, each with a reset back to its
default. Every change leaves as it happens, so there is no save button and nothing to lose by pressing
Escape. It asks for permission level 2, the same one a gamemaster command asks for, and the packet
carrying a change checks that permission again on its own: gating the command alone would hand the
file to anybody able to send a packet.

Behind it, `config/oas/deepcrate.json`, written on first launch and rewritten at every one after, so
an option added by an update shows up there by itself. A value outside its range is pulled back in
and the corrected value is what stays on disk, whether it came from the screen or from a text editor.

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

Twelve things are reachable from outside, and nothing here is a special case the mod keeps for itself:
every shipped tier, module, cell, sort order and tooltip line goes through the same calls.

| Point | Where | What it adds |
|---|---|---|
| `DeepCrateApi.registerTier` | `onDeepCrateInit` | a crate, with its block, its rows and its columns |
| `DeepCrateApi.registerModule` | `onDeepCrateInit` | a capacity module, pointed at an item tag |
| `DeepCrateApi.registerRowModule` | `onDeepCrateInit` | a module that buys rows rather than capacity |
| `DeepCrateApi.registerModuleSlot` | `onDeepCrateInit` | a cell on the module tab, which grows with it |
| `DeepCrateApi.onTierRegistered` | `onDeepCrateInit` | a call whenever a tier is registered after yours |
| `CrateLayoutCallback.EVENT` | `onDeepCrateInit` | the last word on a crate's rows per page and pages |
| `CrateCapacityCallback.EVENT` | `onDeepCrateInit` | the last word on what a slot holds, item by item |
| `ConfigSchema.registerHolder` | `onDeepCrateConfig` | your own options, in the same settings file |
| `ConfigBounds.registerGroup` | `onDeepCrateConfig` | the range each of them is clamped to |
| `DeepCrateClientApi.registerSortOrder` | client init | a sort button, with its icon and its comparator |
| `CrateScreenCallback.EVENT` | client init | a widget on the crate screen |
| `CrateTooltipCallback.EVENT` | client init | a line in the tooltip of a crate slot |

Getting the jar onto your compile classpath comes first, and it is the step that has to be exact.
DeepCrate ships remapped, so Loom has to map it back into the names your development environment
uses. `modImplementation` does that; `compileOnly files(...)` does not, and the difference shows up
as `cannot access class_2960` and a dozen errors like it, which reads as a broken API and is only a
broken dependency line.

Fabric API goes on that same classpath, and at compile time rather than at runtime, even for an addon
that never calls it directly. Two things depend on it. The events above are Fabric `Event` objects, so
`CrateLayoutCallback.EVENT.register(...)` does not type-check without it, and it fails as
`class file for net.fabricmc.fabric.api.event.Event not found`. And registering a block calls
`Blocks.register`, which Mojang declares private and which Fabric API's access widener opens; Loom
carries that widener over to whoever declares the module, so `modRuntimeOnly` leaves you with
`register(...) has private access in Blocks` on the very example below.

```kotlin
dependencies {
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11")
    modImplementation(files("libs/deepcrate-1.0.0.jar"))
}
```

That `libs/` is a folder of your own project, beside your build script, and it exists so your code has
something to compile against. It is not how anyone installs DeepCrate: a player drops the same jar in
`mods/` like any other mod, and yours goes in beside it. Take the jar from the release you are
building against, or build it from this tree with `cd mod && ./gradlew build`. In the Groovy DSL the
same line is `modImplementation files("libs/deepcrate-1.0.0.jar")`.

Which versions to build against are in `mod/gradle.properties`, except two that live in
`mod/build.gradle.kts`: the fabric-loom version on the `plugins` block, and
`loom.officialMojangMappings()`, which is the mapping set this whole API is named in. Building an
addon against any other mapping set renames every type it exposes and nothing lines up.

Then the loader has to be told. The modid is `deepcrate`, which is also the resource namespace,
though not the Java package:

```json
  "depends": {
    "deepcrate": "*",
    "fabric-api": "*"
  }
```

`DeepCrateAddon` is the entry point, and its two methods are two passes of the mod's own start-up.
`onDeepCrateConfig()` runs before the settings file is read and is a default method you may leave
out; `onDeepCrateInit()` is the one you have to write, and it runs once the eleven shipped tiers are
in place and before anything reads a registry. Both passes go to the same instance of your class, so
a field set in the first is still there in the second. Both also finish before any `"client"` entry
point starts, which is what lets a sort order or a widget read an option your holder declared.
Declare the class under `"deepcrate"`, and the client half under `"client"`, as usual:

```json
  "entrypoints": {
    "deepcrate": ["com.example.slatecrate.SlateCrateAddon"],
    "client": ["com.example.slatecrate.SlateCrateAddonClient"]
  }
```

The whole of it in three files, compiled against this tree rather than written from memory:

```java
package com.example.slatecrate;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.CrateLayoutCallback;
import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateAddon;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateCapacityCallback;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.api.module.RowModule;
import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.config.ConfigBounds;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SlateCrateAddon implements DeepCrateAddon {
    /** Every tier anyone registers, this mod's own included, for its guide page. */
    public static List<CrateTier> knownTiers = List.of();

    @Override
    public void onDeepCrateConfig() {
        ConfigSchema.registerHolder(SlateCrateConfig.class, "slatecrate");
        ConfigBounds.registerGroup(registrar -> registrar.b("slateCrateRows", 1, 12));
    }

    @Override
    public void onDeepCrateInit() {
        Block block = Blocks.register(
            ResourceKey.create(Registries.BLOCK, id("slate_crate")),
            DeepCrateBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE)
        );
        Items.registerBlock(block);
        DeepCrateApi.registerTier(new CrateTier(id("slate_crate"), SlateCrateConfig.slateCrateRows, 9, block));

        DeepCrateApi.registerModule(new CrateModule(id("module_1024"), 1024, itemTag("module_1024")));
        DeepCrateApi.registerRowModule(new RowModule(id("module_triple_row"), 3, itemTag("module_triple_row")));
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(id("polish"), 2, 4, id("container/slot/polish"), CrateModuleSlot.tagged(itemTag("polish")))
        );

        DeepCrateApi.onTierRegistered(() -> knownTiers = DeepCrateApi.tiers());

        // A crate wider than a chest gets shorter pages, so its panel stays inside a 240-pixel screen.
        CrateLayoutCallback.EVENT.register(
            (tier, rows, layout) -> tier.columns() <= CrateTier.DEFAULT_COLUMNS ? layout : new CrateLayout(2, (rows + 1) / 2)
        );
        CrateCapacityCallback.EVENT.register((tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("slatecrate", path);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }
}
```

Several of those numbers are positions rather than sizes, so here is what each record takes.
`CrateTier` is `(id, rows, columns, block)`, with a three-argument form that fills in
`CrateTier.DEFAULT_COLUMNS`, which is nine. `CrateModule` is `(id, capacity, tag)` and `RowModule` is
`(id, rows, tag)`. `CrateModuleSlot` is `(id, order, stackLimit, emptyIcon, filter)`, where `order`
places the cell in the column with the smallest at the top, and `stackLimit` is what one cell
accepts. `CrateLayout` is `(rowsPerPage, pageCount)`, in that order. They are records, so each of
those names is also the accessor: `tier.id()`, `tier.rows()`, `tier.columns()`, `tier.block()`,
`layout.rowsPerPage()`, and so on down the list.

The two events fold rather than vote. Each listener is handed what the one before it returned, so
returning the argument untouched is how an addon steps aside on a crate it has no opinion about, and
a later listener has the last word only because it saw the earlier answer.

Reading the registries is open too, and none of it needs an event: `DeepCrateApi.tiers()`, `tier(id)`
and `tierOf(block)` for the crates, `modules()`, `moduleFor(stack)` and `capacityOf(stack)` for the
capacity modules, `rowModules()`, `rowModuleFor(stack)` and `rowsOf(stack)` for the row modules,
`moduleSlots()` and `moduleSlot(id)` for the cells, `capacityAmong(stacks)`, `rowsAmong(stacks)` and
`layoutFor(tier, rows)` for what the mod itself computes, plus `automationLimited()` and
`MAX_CAPACITY`. On the client, `DeepCrateClientApi` adds `sortOrders()`, `sortOrder(id)`,
`order(sortOrder, container, reversed)` and `nameOf(item)`, that last one giving the translated,
displayable name of an item type rather than of a renamed stack.

The options are plain public static fields, enumerated by reflection, so adding one is adding a field:

```java
package com.example.slatecrate;

/** Options of this mod, written under "slatecrate" in config/oas/deepcrate.json. */
public final class SlateCrateConfig {
    private SlateCrateConfig() {}

    public static int slateCrateRows = 10;
}
```

A field is picked up when it is public, static and not final, and of a type the settings file can
carry: `int`, `long`, `float`, `double` or `boolean`. The label it wears on the settings screen comes
from a translation key rather than from its own name, and the key belongs to the host mod: the camel
case becomes snake case behind `deepcrate.option.`, so `slateCrateRows` reads
`deepcrate.option.slate_crate_rows`, written in your lang files and never under your own namespace.
Leave it out and the screen shows the raw key. The registrar handed to `registerGroup` has one
method, `b(name, min, max)`, and it bounds numbers only; naming a boolean there clamps nothing.

The second argument of `registerHolder` is the category, which is the object your options are written
under in the settings file and the heading they sit under on the screen. Its key is built the same
way, `deepcrate.category.` followed by the string you passed, so `"slatecrate"` reads
`deepcrate.category.slatecrate` and belongs in your lang files too.

The three points that draw something live on the client side, where the language and the screen are:

```java
package com.example.slatecrate;

import oas.dreyka.deepcrate.client.screen.hook.CrateScreenCallback;
import oas.dreyka.deepcrate.client.screen.hook.CrateTooltipCallback;
import oas.dreyka.deepcrate.client.sort.CrateSortOrder;
import oas.dreyka.deepcrate.client.sort.DeepCrateClientApi;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.Comparator;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class SlateCrateAddonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DeepCrateClientApi.registerSortOrder(
            new CrateSortOrder(
                id("by_mod"),
                2,
                id("textures/gui/sort/by_mod.png"),
                (totals, collator) -> Comparator.comparing((Item item) -> BuiltInRegistries.ITEM.getKey(item).getNamespace())
                    .thenComparing(DeepCrateClientApi::nameOf, collator)
            )
        );

        CrateScreenCallback.EVENT.register((screen, area) -> {
            Button button = Button.builder(Component.translatable("screen.slatecrate.notes"), ignored -> {})
                .bounds(area.left() + area.width() + 3, area.top() + area.height() - 20, 20, 20)
                .build();
            area.addWidget(button);
            // Past the right edge of the panel, where the game counts a click as one into the world:
            // without this line, releasing one on the button drops what the player is carrying.
            area.keepClickable(button.getX(), button.getY(), button.getWidth(), button.getHeight());
        });

        CrateTooltipCallback.EVENT.register((menu, slot, itemStack, lines) -> {
            if (slot instanceof DeepCrateSlot) {
                lines.add(Component.translatable("screen.slatecrate.in_a_crate"));
            }
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("slatecrate", path);
    }
}
```

`CrateSortOrder` is `(id, order, icon, rule)`, where `order` places the button in the row with the
smallest first, and `rule` is handed `(Map<Item, Long> totals, Collator collator)`: `totals` is how
many of each item type the crate holds, every slot counted, and the collator sorts names in the
language the player reads.

Two files follow from that code. A sort order reads its wording from `screen.slatecrate.sort.by_mod`
and `screen.slatecrate.sort.by_mod_reversed`, in your own lang files, because the drawing shows what
the next press will do rather than what the last one did; its icon is sixteen wide and thirty-two
tall, the plain way up top and the reversed one under it. The two icons are named differently and it
is easy to get backwards. A sort order's icon is a whole texture path, extension and all, the way the
shipped ones are written as `deepcrate:textures/gui/sort/name.png`. A cell's empty icon is a sprite
instead, so `slatecrate:container/slot/polish` is read from
`textures/gui/sprites/container/slot/polish.png`, at the sixteen by sixteen the game draws a slot at.

Both `order` numbers start at zero and the mod has already taken zero and one in each: the capacity
cell and the row cell on one side, the name and count buttons on the other. Ties are not an error, and
they are not refused either; the sort is stable, so two things sharing a number stay in the order they
were registered, which is a coin toss between two mods. Start at two.

An item tag named in a module or a cell is an ordinary data file, under
`data/slatecrate/tags/item/module_1024.json`. The folder is `item`, singular, the same as the four
the mod ships under `data/deepcrate/tags/item/`: `module_128`, `module_256` and `module_512` for the
capacity modules, `module_row` for the one that buys rows.

A tier is a block, so it needs what any block needs and the mod supplies none of it for you. Under
`assets/<yours>/`, that is `blockstates/slate_crate.json`, a model in `models/block/`, one in
`models/item/`, the item definition in `items/slate_crate.json` and the textures they point at, plus
`block.<yours>.slate_crate` in both lang files. Copy the shapes from
`mod/src/main/resources/assets/deepcrate/` rather than writing them from memory.

What you do not have to wire is the block entity. Registering a tier is enough for its block to join
the shared type, whenever it is registered, so a crate with no assets at all still opens, stores and
saves; it just stands there as a black and violet cube.

### What stays closed, and why

A kind of crate stays Java. A tier carries a `Block`, blocks go into the registry while the game
loads its mods, and that registry is frozen long before the first data file is read: a crate declared
in JSON would arrive with nothing to stand on. A module is the opposite, since it points at an item
tag rather than at one item, so adding your item to `deepcrate:module_512` makes it a module with no
code at all.

The layout event has the last word on paging, but not on hiding rows. A layout whose pages cannot
cover the crate is refused, the balanced one is used instead and the reason is logged, because the
rows past its end would be drawn nowhere and reachable by nothing.

The capacity event is not clamped for you. `registerModule` refuses a module above 32767, the event
does not, and a limit above that comes back to bite at save time when the `Count` field refuses the
number.

### What bites an addon first

The screen hands you a `CrateScreenArea`: the panel's corner, its size, a way to add a widget, and
`keepClickable`. Name any rectangle you draw past the edge of the panel through that last one. The
game counts a click outside a container screen as a click into the world, and releasing one there
throws on the ground whatever the player is carrying. The two shipped buttons and the module tab are
named the same way.

`onScreenInit` fires again on every layout, which includes every window resize, and the screen throws
its widgets away between two of those. Add yours again rather than keeping one across calls.

The config pass runs before the settings file is read, which is before any registry is filled. Touch
nothing outside the config package from it: reaching `RegistryInit` there runs its class initialiser
ahead of the read and freezes `rowModuleStackLimit` at its default.

Four public constants are gone, because each of them is now a setting an admin decides:
`DeepCrateApi.BASE_CAPACITY` and `CrateLayout.MAX_ROWS_PER_PAGE` and `RowModule.STACK_LIMIT` read as
`CrateConfig.baseCapacity`, `CrateConfig.maxRowsPerPage` and `CrateConfig.rowModuleStackLimit`, and
`DeepCrateApi.AUTOMATION_LIMITED` as `DeepCrateApi.automationLimited()`. Keeping the first three
deprecated would have been worse than removing them: `javac` copies the value of a compile-time
constant into your class file, so an addon built against them would carry 64, 4 and 16 for good and
compute capacities the server does not have. `DeepCrateApi.MAX_CAPACITY` stays, because a packet
writing a short is not going to change.

## Build and test

Minecraft, the Fabric loader, Fabric API and `mod_version` are pinned in `mod/gradle.properties`.
Loom's own version and `officialMojangMappings()` cannot live there, since a plugin is resolved before
the properties are, so both sit in `mod/build.gradle.kts` and a toolchain bump edits two files rather
than one. Java comes from the toolchain rather than a path. `mod_version` is repeated in `mod/src/main/resources/fabric.mod.json` and the two must
not drift.

The package base is `oas.dreyka`, the resource namespace and `archives_base_name` are both
`deepcrate`. Those three are not the same string on purpose: the namespace is written into saves, the
package is not.

    cd mod
    ./gradlew build           # jar in build/libs/, gametests compiled and run
    ./gradlew test            # 55 JUnit tests: storage, paging, save format, settings
    ./gradlew runGameTest     # 45 gametests, needs a world, no window
    ./gradlew runClientGameTest   # drives a real client and photographs it
    ./gradlew runClient       # a playable dev client, opens a window

`check` depends on `compileGametestJava`, so a gametest that stopped compiling fails the build instead
of surfacing the next time somebody launches a client. `runClientGameTest` builds a fixed scene with
commands, a stone platform in cleared air at noon, and takes its pictures from inside the game rather
than off the compositor. They land in `build/run/clientGameTest/screenshots/`. A chest of the game
stands in the same scene as the control every shot is read against.

`runClient` is for playing, not for verifying: it takes over the screen and the speakers. To watch the
mod run without either, `mod/scripts/headless-test.sh start --both --timeout 300` puts the client in
an invisible sway session on a virtual output, still rendered by the graphics card, and
`mod/scripts/headless-test.sh shot <name>` brings a picture back.

`main` is what ships. `test` holds the JUnit classes, `gametest` the ones that need a world and the
two client tests that take screenshots. Neither reaches the jar: `jar` packages `sourceSets.main`
alone, and the gametests carry their own mod metadata under `deepcrate-gametest`, so the shipped
`fabric.mod.json` never names an entry point the player's jar lacks.

    unzip -l build/libs/*.jar | grep -ciE '/dev/|gametest|Test\.class|junit'

That prints 0. It is a count rather than an exclude rule because the exclusion works by omission,
which a later edit could undo without anything complaining.

`config/oas/deepcrate.json` is written on first launch under `run/` and `run/server/`. It is read
before the registries are filled, which matters: `rowModuleStackLimit` is read during
`RegistryInit`'s static initialisation, so a load happening after it would leave the option looking
functional and doing nothing.

## Layout

`mod/` holds the mod. `web/` holds the site, static with no build step, two hand-written language
trees and its own checks under `web/tools/`; its pictures come out of `runClientGameTest`. The
design notes are in `docs/superpowers/`.

## Licence

MIT, © 2026 Dreyka Oas. Play it, share it, fork it, build an addon on it and publish that addon, all
without asking. Keep the copyright and permission notice with any substantial copy of the code, which
is the whole of what MIT requires. See [LICENSE](LICENSE). A mention is welcome as a courtesy, never
as a condition.
