<!-- ────────────────────────────────────────────────────────────────────────
     FORM FIELDS. Not part of the description body.
     Paste this file whole into the project description with the editor set
     to Markdown and not to its visual mode. HTML comments render as nothing,
     so this block is harmless where it sits.

     The body below matches store/modrinth.md, deliberately: one description,
     two shops. CurseForge keeps style="color:...", so the red lines only show
     their colour here. Modrinth strips it and prints the text plain, so over
     there the lithium warning and the one about a module pulled out are
     quoted with > to stay visible. Those two quotes are the only difference
     between the two bodies.

     Summary:
       Six tiers of chest, and a module that lifts every slot from 64 to 512.
       Bolt on up to sixteen more rows, page through what no longer fits, sort
       the lot with one button. Empty a crate before pulling its module out.

     Licence      MIT, and set the modpack permission to the open one: under
                  MIT nobody has to ask, so a project page still demanding it
                  contradicts the file shipped inside the jar.
     Environment  Required on the server and on every client. The client draws
                  the screen and works out the sort order, the server holds the
                  slots and their counts. Read from fabric.mod.json, which
                  declares environment "*" and a client entrypoint.
                  CONFIRM before publishing by joining a modded server with a
                  vanilla client; the split is read from the source, not tried.
     Categories   Storage, API and Library
     Links        Website  https://deepcrate.pages.dev
                  Source   https://github.com/Dreyka-Oas/DeepCrate
                           Checked anonymously on 6 September 2026: the
                           repository answers 200, as does its issues page.
                  No CurseForge or Modrinth URL exists: not published.
     ──────────────────────────────────────────────────────────────────── -->

# DeepCrate

**Six tiers of chest that hold 512 of an item in a single slot.**

Required on the server and on every client.

## Before you install

<span style="color:#c62828">**With lithium installed, hoppers and pipes stop filling a crate past 64.**</span>
Lithium replaces the hopper wholesale and keeps its own copy of the inventory it is feeding, so against
a container that answers more than 64 it takes the items out of the hopper and never writes them in.
That was measured, not assumed: eight blocks of dirt destroyed per run, patch or no patch. A crate
therefore tells automation 64 a slot for as long as lithium is there, which costs you the feature and
keeps your items. Your own hands go through the screen and are unaffected either way.
Whoever runs the server can take that decision back: `limitAutomationWithLithium` in
`config/oas/deepcrate.json`, set to false, gives the feature back and gives up those items.

## The six crates

Each tier is the crate before it surrounded by eight of the new material, and each adds a row of nine.
The ladder follows how unpleasant the material is to fetch rather than the usual iron to diamond climb.

| Crate | Surround the previous one with | Slots |
|---|---|---|
| Copper | copper ingots, around a copper chest | 27 |
| Iron | iron ingots | 36 |
| Amethyst | amethyst shards | 45 |
| Prismarine | prismarine crystals | 54 |
| Breeze | breeze rods | 63 |
| Echo | echo shards | 72 |

Set two of the same tier side by side and they merge into one screen, the way chests do, sharing a
single module.

## 512 to a slot

A slot holds 64 on its own. The slot hanging off the left edge of the screen takes a capacity module
and lifts the whole crate at once, to 128, 256 or 512. A greyed plate sits there while it is empty, so
it reads as what it is. Only one fits: push a second in and the first comes back to your hand. The
first module is an amethyst shard ringed with copper ingots, then lapis lazuli around that one, then
redstone around that.

<span style="color:#c62828">**Pull a module out, close the screen, and everything above the new capacity lands on the ground.**</span>
A full double echo crate losing its 512 module puts around a thousand stacks on one block, and none of
that survives five minutes if you are not standing there picking it up. Empty a crate before you take
its module out.

What leaves a crate is ordinary. A hand, a hopper and a dropped item still carry 64, so the crate gives
you one vanilla stack at a time and breaking one drops its contents cut into stacks of 64.

## Rows you bolt on

Under the capacity slot sits a second one, holding a stack of up to sixteen row modules, each of them a
chest ringed with planks. One module is one row of nine, and sixteen is the ceiling on every tier
alike: a copper crate stops at nineteen rows, an echo one at twenty-four. Fill a copper crate's stack
and it outgrows an echo crate that has none.

Adding or removing one reopens the screen, since a menu's slot list is fixed once it is built. That
happens at the start of the next tick rather than inside your click, so whatever you are carrying stays
in your hand. Rows you take back drop what was sitting in them, same rule as the capacity module.

Both halves of a double crate grow together, one module giving one row on each side, because the two
are shown as one screen. A crate is only cut back once the last screen on it closes, so nobody shrinks
the grid under your open menu.

## Sorting, and finding

Two buttons stand above the top left corner of the screen. One orders by name, the other by how full
each pile is, and both gather identical stacks into one up to whatever the module allows, which is
where your free slots come from. Press the same button again and it reverses; the drawing tells you
which way the next press will go, A over Z or Z over A.

The order is worked out on your machine and sent whole. A server carries no language files, so it
cannot know that you read Pierre where the next player reads Stone, and sorting by a name nobody sees
is not sorting. A pair of crates is sorted as one run of slots, so the letters do not start over at the
seam. Modules stay in their own tab, untouched.

A search field runs along the inventory line under the grid, from the right of its label to the edge
of the panel. Type in it and anything whose name does not match goes grey rather than disappearing,
so you keep seeing where your things are, and the screen turns to the first page that holds a match.

## Pages, not a scrollbar

Four rows fit on a page. Past that the screen splits and the pages share the rows evenly, so six rows
give two pages of three instead of one full page and one nearly empty. The numbers stack down the right
edge, and a number carries a dot when its page is holding something. Shift-clicking reaches every slot,
including the pages you are not looking at.

The page you are on never reaches the server, which is what keeps a slot index from being steered from
outside.

## Settings, without leaving the game

`/deepcrateconfig` opens them: a search box across the top, the categories down the left, one row per
option on the right, each with a reset back to its default. Every change leaves as it happens, so
there is no save button and nothing to lose by pressing Escape.

It asks for permission level 2, the same one a gamemaster command asks for, and the packet carrying a
change checks that permission again on its own. Everything still lands in
`config/oas/deepcrate.json`, so a text editor and the screen say the same thing.

## For other mods

Register a crate tier, register a capacity module, or take over the slot and page counts. Declare your
class under `"deepcrate"` in the `entrypoints` block of your `fabric.mod.json` and it gets called at
init. A module points at an item tag rather than at one item, so dropping your item into
`deepcrate:module_512` makes it a module with no code at all.

A crate writes its slots in its own format, the item on one side and its count on the other, because
the stack codec of the base game refuses any count above 99. A slot can go to 32767 before the save
format runs out. The three shipped modules stop at 512 by choice, and yours does not have to.

What you have just read is the whole of it.
