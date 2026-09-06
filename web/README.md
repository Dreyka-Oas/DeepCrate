# DeepCrate, the site

Static, no build, no dependency. Two hand-written language trees under `lang/`, a root page that
sends a visitor into one of them, and a set of checks that run under plain `node`.

## Looking at it locally

Root-relative hrefs mean `file://` resolves them against the disk root and every link answers 404.
Serve the folder instead:

    python3 -m http.server 8123 --bind 127.0.0.1

Then open `http://127.0.0.1:8123/`, never the `.html` file itself.

That server sends `Last-Modified` and no `Cache-Control`, so a browser holds the stylesheets and a
CSS edit does not show up on reload. It cost half an hour once, chasing a padding bug that was
already fixed on disk. When editing CSS, serve with `Cache-Control: no-store` instead, or read the
computed value rather than trusting the picture.

## The layers a page carries

Order in `<head>`, and none of it deferred: `core/dc-lang` (it can redirect, and everything reads
`DC.s` off it), the two string tables, `core/dc-theme`, `chrome/dc-boot`, the four sound files, then
`chrome/components`. The parser has to reach the `<dc-*>` tags with the elements already defined, or
undefined content flashes before the swap.

`chrome/dc-boot.js` is the opening screen: two halves meeting on a seam, a cell filling between
them, and the site behind. It plays on a full load only, since the router never re-runs the `<head>`,
and it does not exist at all under reduced-motion or with JS off. A click, Escape, Enter or space
cuts it short, and a cap ends it whatever the font server is doing.

`fx/sound/` is four files: the engine (one context, one master gain, the room tone, the mute stored
in `dc-sound`), the shared burst graph, the four voices, and the nav button. Nothing is loaded from
disk, it is all synthesised. Nothing plays before a first real gesture either, which is a browser
rule and not a choice: a `click` dispatched over CDP does not always count as one, so an audit that
wants the context running should send a keypress. The voices are wired in one place each, the router
for `tap` and `slide`, the theme button for `latch`, and `data-sfx` on a revealed block for `shard`.

`lang/*/oas.html` is where the footer signature goes. It presents the workshop rather than the mod,
and it is the one page in the tree with no sidebar and no breadcrumb.

## The checks

    node tools/check.mjs

Six of them, and one command gates a commit. `check-assets` walks every `href` and `src`, refuses a
page whose script and stylesheet list differs from its siblings, and reports anything under
`assets/` that nothing points at. `check-lang` proves the two trees are symmetric, by `<html lang>`,
by `hreflang` and by file name. `check-i18n` runs both string tables in a sandbox and compares their
keys. `check-source` finds classes defined and never used, and used and never defined.
`check-tells` covers the typography, the horizontal rule and the filler words. `check-design`
measures variety: easing curves, reveal distances, background textures, page skeletons.

## What the checks do not cover

Two things only a browser answers, and neither is in `check.mjs`. Responsive behaviour, read at the
widths a phone, a tablet and a desktop actually use rather than guessed from the media queries; the
nav breakpoint and the guide padding were both wrong and both looked fine in the source. And an
accessibility audit, which caught colour-only links, an unreachable code block and dark ink on a
fill too dark to carry it.

Centering is worth a grep before the browser pass, since a page held in the middle by `margin: auto`
and a couple of `text-align: center` reads as a layout until the window narrows:

    grep -rnE 'text-align:\s*center|margin(-inline)?:\s*(0|auto)|justify-content:\s*center|place-items:\s*center' assets lang

Every hit gets read where it sits. Here they are a digit inside a step badge, the page gutter on
`.container`, and the language gate, which is two links and has nothing to reflow. The real test is
that the first mechanic band goes from two columns to one and that the nav and the hero mark change
with the width, not that the middle column gets thinner.

    agent-browser --headed --session <name> open http://127.0.0.1:8123/lang/fr/
    agent-browser --headed --session <name> set viewport 390 844
    agent-browser --headed --session <name> a11y
    agent-browser --headed --session <name> set media dark

Run the audit on both themes. Half of what it found showed up in one and not the other.

## Deploying

Build command empty, output directory this folder.

    npx wrangler pages deploy . --project-name=deepcrate

Pushing to GitHub does not put the site online. The deploy command is what does.

## Layout

    index.html          language gate, no content of its own
    _headers            security headers, one policy for every response
    lang/fr, lang/en    one tree per language, the same file names on both sides
    assets/css          tokens, base, and one file per component
    assets/js           core, chrome, fx, router, i18n
    assets/img          frames taken by the mod's own client test
    tools               the checks, dependency-free

Every picture under `assets/img/` comes out of `./gradlew runClientGameTest` in `../mod/`. Those
carrying interface are halved with a box filter, an exact half of a four-times scale, so the text
lands back on whole pixels, and they render with `image-rendering: pixelated`.

The two world views carry no interface and take `.shot--world`, which renders them smooth: scaling
one by a fraction with `pixelated` drops rows of pixels unevenly. `hero.png` is the only one kept at
its native 1920, because it is the only figure that draws full width, up to 1720, where a half-size
source would upscale visibly. The rest never exceed the column they sit in.

Nothing here is drawn by hand or staged: a picture that is not the mod running has no place on the
page.
