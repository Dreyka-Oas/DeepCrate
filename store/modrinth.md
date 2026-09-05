<!-- ─────────────────────────────────────────────────────────────────────────
     FORM FIELDS. Not part of the description body.
     Paste this file whole into the description with the editor set to
     Markdown and not to its visual mode. HTML comments render as nothing,
     so this block is harmless where it sits.

     The body below matches store/curseforge.md, deliberately: one
     description, two shops. CurseForge keeps style="color:...", Modrinth
     strips it and prints that text plain. So the lithium warning and the one
     about a module pulled out are quoted with > here, which Modrinth does
     render, and left unquoted there. Those two quotes are the only
     difference between the two bodies.

     Summary (Modrinth caps it at 256 characters, this one is 212):
       Six tiers of chest, and a module that lifts every slot from 64 to 512.
       Bolt on up to sixteen more rows, page through what no longer fits, sort
       the lot with one button. Empty a crate before pulling its module out.

     Licence      Custom. Select "Custom" and leave its URL field empty: the
                  repository is private, so a link to the LICENSE file in it
                  answers 404 to everybody but its owner. The file is packed
                  into the jar by tasks.jar in mod/build.gradle.kts, which is
                  where a reader gets it. Not an open-source licence: free to
                  play, free to share unmodified provided the author is
                  credited and the link named in section 2 is given, never to
                  sell, everything else on request. The modpack answer is in
                  the body.
     Environment  Client: required. It draws the crate screen and works out
                  the sort order that gets sent back.
                  Server: required. It holds the slots and their counts.
                  Read from fabric.mod.json, which declares environment "*"
                  and a client entrypoint. CONFIRM before publishing by
                  joining a modded server with a vanilla client; the split is
                  read from the source, not tried.
     Tags         Storage, Utility, Library
     Links        Source   leave empty. github.com/Dreyka-Oas/DeepCrate is a
                           private repository: gh repo view prints visibility
                           PRIVATE and an anonymous request gets 404, so the
                           field would ship a dead link as the only way out of
                           the page. Fill it the day the repository is opened,
                           and drop the sentence about a closed source from
                           the licence section at the same time.
                  Website  none. web/ is reserved and holds no page yet.
                  No CurseForge or Modrinth URL exists: not published.
     ───────────────────────────────────────────────────────────────────── -->

# DeepCrate

**Six tiers of chest that hold 512 of an item in a single slot.**

Required on the server and on every client.

## Before you install

> <span style="color:#c62828">**With lithium installed, hoppers and pipes stop filling a crate past 64.**</span>
> Lithium replaces the hopper wholesale and keeps its own copy of the inventory it is feeding, so against
> a container that answers more than 64 it takes the items out of the hopper and never writes them in.
> That was measured, not assumed: eight blocks of dirt destroyed per run, patch or no patch. A crate
> therefore tells automation 64 a slot for as long as lithium is there, which costs you the feature and
> keeps your items. Your own hands go through the screen and are unaffected either way.

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

> <span style="color:#c62828">**Pull a module out, close the screen, and everything above the new capacity lands on the ground.**</span>
> A full double echo crate losing its 512 module puts around a thousand stacks on one block, and none of
> that survives five minutes if you are not standing there picking it up. Empty a crate before you take
> its module out.

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

## For other mods

Register a crate tier, register a capacity module, or take over the slot and page counts. Declare your
class under `"deepcrate"` in the `entrypoints` block of your `fabric.mod.json` and it gets called at
init. A module points at an item tag rather than at one item, so dropping your item into
`deepcrate:module_512` makes it a module with no code at all.

A crate writes its slots in its own format, the item on one side and its count on the other, because
the stack codec of the base game refuses any count above 99. A slot can go to 32767 before the save
format runs out. The three shipped modules stop at 512 by choice, and yours does not have to.

## Licence and modpacks

Not open source, and the source is not published either: the repository is closed, and the mod ships
as a jar carrying its licence file inside. Playing it is free, with nothing asked in return, and the
licence says that will never change.

Passing it on is free as well, on the conditions that file sets: the jar goes on unmodified with the
licence beside it, the author is credited with the link named there, and nothing is charged or earned
anywhere along the way. <span style="color:#c62828">**Never to be sold.**</span>

A modpack, a fork, or reusing the code needs written permission first, and it is usually a yes. Where
to ask is in the licence file, which travels in every download.

No site for the mod yet. What you have just read is the whole of it.
