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

Every picture under `assets/img/` comes out of `./gradlew runClientGameTest` in `../mod/`, halved
with a box filter so the interface stays crisp. Nothing here is drawn by hand or staged: a picture
that is not the mod running has no place on the page.
