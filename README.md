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
The cell under the capacity one takes a stack of sixteen, so a copper crate can end up with more slots
than an echo one, and an echo crate with twenty-four rows. Rows are read from every cell and added
together, so a cell an addon put there holding row modules extends the crate further rather than
replacing what the first gave.

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

`DeepCrateAddon` is the entry point, and its two methods are two passes of the mod's own start-up.
`onDeepCrateConfig()` runs before the settings file is read, `onDeepCrateInit()` once the six shipped
tiers are in place and before anything reads a registry. Declare the class under `"deepcrate"`, and
the client half under `"client"`, as usual:

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

The options are plain public static fields, enumerated by reflection, so adding one is adding a field:

```java
package com.example.slatecrate;

/** Options of this mod, written under "slatecrate" in config/oas/deepcrate.json. */
public final class SlateCrateConfig {
    private SlateCrateConfig() {}

    public static int slateCrateRows = 10;
}
```

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

Two files follow from that code. A sort order reads its wording from `screen.slatecrate.sort.by_mod`
and `screen.slatecrate.sort.by_mod_reversed`, in your own lang files, because the drawing shows what
the next press will do rather than what the last one did; its icon is sixteen wide and thirty-two
tall, the plain way up top and the reversed one under it. A cell's empty icon is a sprite, so
`slatecrate:container/slot/polish` is read from `textures/gui/sprites/container/slot/polish.png`.

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

`mod/` holds the mod. `web/` holds the site, static with no build step, two hand-written language
trees and its own checks under `web/tools/`; its pictures come out of `runClientGameTest`. The
design notes are in `docs/superpowers/`.

## Licence

MIT, © 2026 Dreyka Oas. Play it, share it, fork it, build an addon on it and publish that addon, all
without asking. Keep the copyright and permission notice with any substantial copy of the code, which
is the whole of what MIT requires. See [LICENSE](LICENSE). A mention is welcome as a courtesy, never
as a condition.
