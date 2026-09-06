// Run from web/:  node tools/check-lang.mjs
//
// The two trees under lang/ are provably symmetric rather than symmetric by
// habit. Two rules per page:
//
//   1. A page under lang/<code>/ declares <html lang="<code>">. Nothing else
//      ties the tree a page sits in to the language it renders: DC.lang() reads
//      that attribute at runtime, so a page left on the wrong value would
//      quietly serve the other language's strings out of assets/js/i18n/.
//
//   2. A page under lang/<code>/ carries a rel="alternate" hreflang="fr" href
//      equal to /lang/fr/ plus its path below its own tree, and the same for
//      "en". Without it a page can drift out of the pair, renamed or moved or
//      forgotten in the other tree, and nothing notices: both trees are
//      hand-written, and hreflang is the only place the pairing is recorded.
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

// ---- 3. the two trees hold the same file names ----
const below = (code) => new Set(byExt(walk(`lang/${code}`), ".html").map((f) => f.slice(`lang/${code}/`.length)));
const [fr, en] = CODES.map(below);
let missing = 0;
for (const [a, b, aCode, bCode] of [[fr, en, "fr", "en"], [en, fr, "en", "fr"]]) {
  for (const page of a) {
    if (!b.has(page)) { fail(`${page} exists under lang/${aCode} but not under lang/${bCode}`); missing++; }
  }
}
if (!missing) pass(`${fr.size} pages, the same set in both trees`);

done("lang");
