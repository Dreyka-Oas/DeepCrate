// Run from web/:  node tools/check-lang.mjs
//
// KEEP, apart from the CODES list: a site that adds a third language adds it
// there and creates the matching tree under lang/.
//
// The two trees under lang/ are provably symmetric, not just symmetric by
// convention. Two rules, per page:
//
//   1. A page under lang/<code>/ declares <html lang="<code>">. Nothing else
//      ties the tree a page sits in to the language it renders: SITE.lang()
//      reads this attribute at runtime, so a page left on the wrong value
//      would silently serve the other language's strings from assets/js/i18n/.
//
//   2. A page under lang/<code>/ carries a rel="alternate" hreflang="fr" href
//      equal to /lang/fr/ plus its path below its own tree, and likewise for
//      "en". Without this, a page can drift out of the fr/en pair (renamed,
//      moved, forgotten in the other tree) and nothing notices: the two trees
//      are hand-written now, and hreflang is the only place the pairing is
//      written down.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const CODES = ["fr", "en"];
const PAGES = CODES.flatMap((code) => byExt(walk(`lang/${code}`), ".html").map((f) => [code, f]));

// ---- 1. <html lang> matches the tree ----
let langMismatch = 0;
for (const [code, f] of PAGES) {
  const m = read(f).match(/<html\s+lang="([^"]*)"/);
  if (!m) { fail(`${f}: no <html lang="..."> found`); langMismatch++; }
  else if (m[1] !== code) { fail(`${f}: <html lang="${m[1]}">, expected "${code}"`); langMismatch++; }
}
if (!langMismatch) pass(`${PAGES.length} pages, <html lang> matches their tree`);

// ---- 2. hreflang alternates point at the counterpart in each tree ----
let altMismatch = 0;
for (const [code, f] of PAGES) {
  const below = f.slice(`lang/${code}/`.length);
  const body = read(f);
  for (const other of CODES) {
    const expected = `/lang/${other}/${below}`;
    const re = new RegExp(`rel="alternate"\\s+hreflang="${other}"\\s+href="([^"]*)"`);
    const m = body.match(re);
    if (!m) { fail(`${f}: no hreflang="${other}" alternate`); altMismatch++; }
    else if (m[1] !== expected) { fail(`${f}: hreflang="${other}" points at ${m[1]}, expected ${expected}`); altMismatch++; }
  }
}
if (!altMismatch) pass(`${PAGES.length} pages, hreflang alternates match their counterpart`);

done("lang");
