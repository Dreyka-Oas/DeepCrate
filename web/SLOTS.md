# Slots

Every file in this template says, in its own first lines, whether it is yours to change or whether it
works as it stands. Two markers, and no file is without one.

`SLOT <name>` means the file holds a decision that belongs to the mod. It tells you what to edit, and
what has to stay true whatever you put there.

`KEEP` means plumbing. It names no mod, it looks the same on every site built from this template, and
editing it breaks something that has nothing to do with your content.

Nothing here is a suggestion you have to follow. A slot can be emptied, replaced by something with a
different shape, or deleted. What follows is the map, so you can tell what deleting costs.

## Start here, in this order

1. `assets/css/tokens.css`, the palette and the two or three typefaces. Everything else reads from it.
2. `assets/js/chrome/site-chrome.js`, the name in the bar, the brand glyph, the navigation links, the
   wiki sidebar, the footer.
3. `assets/favicon.svg`, the same glyph as a file.
4. The two pages `lang/fr/index.html` and `lang/en/index.html`, section by section.
5. The wiki pages, or the ones you keep.
6. `assets/js/i18n/fr.js` and `assets/js/i18n/en.js`, the strings the JavaScript prints.

## Where the name lives

Five places, and a search for the old name has to come back empty before a site ships.

| Place | File |
| --- | --- |
| The bar and the footer | `assets/js/chrome/site-chrome.js` |
| The big two-part wordmark | `lang/fr/index.html` and `lang/en/index.html` |
| Page titles and meta descriptions | every `.html` file, both trees |
| The glyph | `assets/favicon.svg` and the constant in `site-chrome.js` |
| The repository owner and name | the closing section of both `index.html` files, and `wiki/api.html` |

The custom elements are called `site-nav`, `site-sidebar`, `site-footer` and `site-ledger`. They are
deliberately neutral, so nothing has to be renamed. If you rename them anyway, the tag, the
`customElements.define` call and the `display: contents` rule in `assets/css/base.css` move together.

## The home page

The hero stays. The seven bands under it are optional, in any combination, in any order. Each one is
fenced in both `index.html` files by a comment saying what it shows and what to delete.

| Band | What it is for | Files | Optional |
| --- | --- | --- | --- |
| Hero | The name, one sentence, two buttons | `shell.css`, `wordmark.css`, `foot.css` | no |
| Moving backdrop | Something alive behind the hero | `scene.css`, `canvas.css`, `home-backdrop.js` | yes |
| Manifesto | What the mod changes, in one sentence | `manifesto.css` | yes |
| Separator | A row of short codes instead of a rule | `ledger.css`, `site-ledger.js` | yes |
| Tiers | A numbered progression | `tiers.css`, `home-tiers.js` | yes |
| List | Things that compare against each other | `list.css` | yes |
| Statement | A timeline whose last step tips over | `statement.css` | yes |
| Wiki index | The way into the wiki | `index.css` | yes |
| Closing call | One last action before the footer | `reports.css` | yes |

The closing call is the one band whose destination is not yours to pick. If you keep it, its button
goes to `https://github.com/(owner)/(Mod)/issues`, the mod's GitHub Issues page, because that is the
place where bugs are listed and a reader looking for a known problem has to land on the list itself.
Not a contact form, not a third-party feedback service, not a chat room. The same address appears in
`wiki/api.html`, and the two say the same thing or neither is believed.

Deleting a band means three things, and skipping the third is what people get wrong: the markup in
**both** `index.html` files, the stylesheet, and its `@import` line in `assets/css/components.css`.
Leave the import behind and `check-source.mjs` reports a rule with no class.

The bands share their furniture through `exhibit.css`: the eyebrow, the section title, the reveal
rhythm. Change it once and every band follows. That is also the trap, because seven bands built from
one skeleton read as seven copies of the same band. `check-design.mjs` counts how many pages open
with the same run of blocks and refuses past four.

## The moving backdrop

Two halves that ignore each other, so either one can be rewritten or dropped alone.

`assets/css/components/home/hero/scene.css` carries a seven-point contract at the top and an example
under it. The example is a skyline with two sweeping beams. It is not a design to keep. Delete it
whole, write another one, and the site still works. The markup it styles is fenced by a SLOT comment
in both `index.html` files, and the two are replaced together.

`assets/js/page/home-backdrop.js` paints drifting glyphs on a canvas. Swapping the glyph list is a
one-line change. Rewriting `draw()` is the larger one the file is set up for: the plumbing around it
already returns before painting under reduced motion, stops the loop when the canvas leaves the
screen, cleans up on a page swap, and re-reads its colours when the theme changes.

Neither half loads an image or a font. The hero has to paint before anything finishes downloading.

## The wiki

Thirteen pages per tree, and a mod is not expected to have thirteen topics. Delete the ones you do
not need, from **both** trees at once, then remove them from the sidebar in `site-chrome.js` and from
the table of contents in the two `index.html` files.

One of the thirteen stays: `wiki/api.html`, the page addressed to whoever wants to build on the mod
rather than play it. It lists the public package, the registries an addon can reach, the tags that
drive behaviour without code, one example that compiles, and the licence terms. A mod nobody can
extend gets one life, the one its author gives it, so the page exists before anyone asks for it. It
says the same thing as the `For addon authors` section of the mod's own README, and if the two
disagree, neither is worth reading.

A page whose French half exists without its English half fails `check-lang.mjs`, which reads the
`hreflang` pair on every page. That check is the only thing keeping the two trees paired.

`wiki-chart.js` draws one chart from two formulas written in the file itself. Both series are
placeholders. Put your mod's real curve there, and make the numbers agree with the parameter table on
the same page, because a reader who spots the contradiction stops trusting the rest.

## The two string tables

`assets/js/i18n/fr.js` and `assets/js/i18n/en.js` are one slot, not two. They hold what the
JavaScript prints: the aria labels, the theme button, the tier names, the footer line. They are keyed
identically, and `check-i18n.mjs` fails the moment they stop matching. A key added to one is added to
the other in the same edit.

Text that sits in the HTML does not belong here. Text the JavaScript builds does.

## Sound

`assets/js/fx/sound/voices.js` is a slot. The engine under it is not. A site scored with another
mod's material is wearing another mod's clothes, so the voices are worth rewriting even though they
are the last thing anyone notices.

Nothing plays before a first real click. That is a browser rule, not a preference, and the mute state
survives a reload.

## What you do not touch

The space scale, the type scale, the radii and the z-index layers in `tokens.css`. They are the rhythm
of the site and they do not move from one mod to the next.

The `.container` gutter in `assets/css/base.css`. Any layout class landing on the same element writes
`padding-block`, never the `padding` shorthand, or the gutter is reset to zero and the text touches
both screen edges below 1180px. That bug shipped once.

The router. It swaps `<body>` and never `<head>`, which is why every page loads the same ordered list
of assets even when it does not use all of them. `check-assets.mjs` enforces that list. A page that
loads one script fewer breaks navigation towards it.

The language gate in `assets/js/core/site-lang.js`, the reduced-motion helper, the theme switch.

## The checks

```
node tools/check.mjs
```

Seven of them, run from the site root, and they are the reason this template can be gutted without
anyone reading all 61 source files afterwards.

`check-i18n.mjs` pairs the two string tables. `check-assets.mjs` pairs the asset lists.
`check-source.mjs` holds the budgets: no source file over 150 lines, no folder over eight files, no
CSS class styled and never used, no custom property declared and never read, no id targeted and never
rendered, no comment naming a file that no longer exists. `check-tells.mjs` catches the typography a
machine leaves behind and the vocabulary tic of repeating one word every paragraph. `check-design.mjs`
refuses a single easing curve applied everywhere, because uniformity is what reads as machine-made.
`check-lang.mjs` keeps the two trees paired. `check-slots.mjs` is the one that keeps this page honest:
it fails on a source file carrying neither marker, on a home band fenced in one language and not the
other, and on a stylesheet imported without existing.

The word lists inside `check-tells.mjs` are themselves slots. They hold the register of the site they
came from. Yours has other tics, and finding them means writing two or three pages first, then
counting.
