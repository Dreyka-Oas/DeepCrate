// Run from web/:  node tools/check-design.mjs
//
// Perfect uniformity is the tell, not the value picked. This measures variety:
// how many distinct easing curves, how many reveal distances, how many stacked
// background textures, and how many different page skeletons in the guide.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const allCss = byExt(walk("assets/css"), ".css").map(read).join("\n");

// ---- 1. the curve half the web already uses ----
if (/cubic-bezier\(\s*0?\.16\s*,\s*1\s*,\s*0?\.3\s*,\s*1\s*\)/.test(allCss) || allCss.includes("--ease-out-expo"))
  fail("cubic-bezier(0.16, 1, 0.3, 1) / --ease-out-expo is back");
else pass("the signature curve is not in use");

// ---- 2. variety of curves ----
const curves = new Set([...allCss.matchAll(/cubic-bezier\([^)]+\)/g)].map((m) => m[0].replace(/\s/g, "")));
if (curves.size < 3) fail(`${curves.size} distinct easing curve(s), three minimum`);
else pass(`${curves.size} distinct easing curves`);

// ---- 3. variety of reveals ----
const dists = new Set([...allCss.matchAll(/translateY\((-?[\d.]+)px\)/g)].map((m) => m[1]));
if (dists.size < 3) fail(`${dists.size} distinct reveal distance(s), three minimum`);
else pass(`${dists.size} distinct reveal distances`);

// ---- 4. one background texture at most ----
const TEXTURES = [
  ["full-page grain", /feTurbulence|var\(--grain\)/],
  ["radial glow on body", /body\s*\{[^}]*radial-gradient/],
  ["hero grid", /hero__grid/]
];
const on = TEXTURES.filter(([, re]) => re.test(allCss)).map(([n]) => n);
if (on.length > 1) fail(`${on.length} stacked background textures: ${on.join(", ")}`);
else pass(`${on.length} background texture`);

// ---- 5. skeleton variety in the guide ----
// The signature is the ordered run of structuring blocks. Eight pages with one
// signature is a template applied by machine, not a layout. The scope is the
// French tree; the English openings are out of scope by choice rather than by
// omission, since both trees are hand-written and nothing forces a page to
// share its counterpart's structure.
const BLOCKS = /wiki-breadcrumb|<h1|wiki-lede|class="shot|<h2|param-table|callout|class="steps|class="code/g;
const WIKI = byExt(walk("lang/fr/wiki"), ".html");
const sigs = new Map();
for (const f of WIKI) {
  const sig = (read(f).match(BLOCKS) || []).slice(0, 5).join(">");
  sigs.set(sig, (sigs.get(sig) || 0) + 1);
}
const worst = Math.max(...sigs.values());
if (worst > 4) fail(`${worst} guide pages share the same opening (four at most)`);
else pass(`${sigs.size} distinct openings across ${WIKI.length} guide pages`);

done("design");
