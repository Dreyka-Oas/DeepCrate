# DeepCrate, the site

Built from the workshop's site template, `.claude/skills/site-template/template/`. The skeleton is
that template's, unchanged: the spacing and type scales, the components and their dimensions, the
router, the checks. What belongs to this mod is the palette, the four faces, the crate glyph, the
moving scene behind the hero, the sound, and every line of text.

[SLOTS.md](SLOTS.md) is the template's own map and is kept as it shipped. It says which files hold a
decision (`SLOT`) and which are plumbing (`KEEP`), and `check-slots.mjs` fails on a source file
carrying neither marker.

## Looking at it locally

Root-relative hrefs mean `file://` resolves them against the disk root and every link answers 404.
Serve the folder instead:

    python3 -m http.server 8123 --bind 127.0.0.1

That server sends `Last-Modified` and no `Cache-Control`, so a browser holds the stylesheets and a
CSS edit does not show up on reload. It cost half an hour once, chasing a padding bug that was
already fixed on disk. When editing CSS, serve with `Cache-Control: no-store` instead, or read the
computed value rather than trusting the picture.

## The checks

    node tools/check.mjs

Seven, and one command gates a commit. They are the template's, with two lists filled in for this
site: the forbidden register in `check-tells.mjs`, and the per-page word ceilings beside it.

## What this site adds to the template

One component, `components/wiki/shots.css`. The template draws everything in CSS and loads no image,
which is the right default for a site that has to paint before the fonts land. This mod has seven
frames taken by its own client gametest, and a guide page that describes a screen without showing it
is asking to be taken at its word. Interface frames render with `image-rendering: pixelated`, the
two world views take `.shot--world` and render smooth, and `hero.png` is the only one kept at its
native 1920 because it is the only figure that draws full width.

Every picture under `assets/img/` comes out of `./gradlew runClientGameTest` in `../mod/`. Nothing
here is staged or drawn by hand: a picture that is not the mod running has no place on the page.

## Where the mod's own decisions live

The palette and the four faces are in `assets/css/tokens.css`. The nav links, the sidebar groups,
the crate glyph and the footer are in `assets/js/chrome/site-chrome.js`. The six tiers are in the
two i18n tables, not in `home-tiers.js`, because the game prints their material names differently in
the two languages. The chart on the rows page is the mod's own rule, base plus nine per module, and
its endpoints match the prose on that page digit for digit.

## What the checks do not cover

Two things only a browser answers. Responsive behaviour, read at the widths a phone, a tablet and a
desktop actually use, and read wide: a container capped at a fixed pixel width with an auto margin
is centering, and it looks fine at 1440 and shameful at 2560. And an accessibility audit, on both
themes, because half of what it finds shows up in one and not the other. Force the theme through
`localStorage` and confirm `data-theme` on `<html>` before trusting the run.

A click dispatched over CDP does not always count as the user gesture the audio context waits for.
Send a keypress and read the context state before concluding the sound is broken.

## Deploying

Build command empty, output directory this folder.

    npx wrangler pages deploy . --project-name=deepcrate

Pushing to GitHub does not put the site online. The deploy command is what does.

## Licence

Two, and they are not the same one. This site is private, all rights reserved, in [LICENSE](LICENSE)
at the root of this folder. The mod it talks about ships under its own terms, in its own `LICENSE`
next to the Java sources: free to play, free to pass on unmodified, never to be sold, everything
else on request. `wiki/api.html` and `wiki/installation.html` describe the mod's terms and point at
the file that travels in every download, never at the one sitting here.
