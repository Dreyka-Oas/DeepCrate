// Run from web/:  node tools/check-assets.mjs
//
// The asset graph. Three things nothing else enforces on a site with no build:
//
//   1. Every href/src in every page resolves to a file that exists. A rename
//      under assets/ is otherwise invisible until a 404 shows up live.
//   2. Every page carries the SAME ordered asset list. The router only swaps
//      <body>, so a script present on one page and missing on another arrives
//      crippled after an internal navigation, and router-swap.js rebinds
//      unguarded on the strength of this check.
//   3. Every file under assets/ is actually reached, and every component
//      stylesheet is imported by components.css exactly once. An orphan is
//      either dead weight in the deploy or a file nobody wired up.
import { existsSync } from "node:fs";
import { dirname, join, normalize } from "node:path";
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const files = walk();
const pages = byExt(files, ".html").sort();
const assets = files.filter((f) => f.startsWith("assets/"));

// The root gate is not a router destination: it carries one script of its own
// and is exempt from rule 2. Rules 1 and 3 still cover it.
const GATE = "index.html";
const routed = pages.filter((p) => p !== GATE);

const resolve = (from, ref) =>
  normalize(ref.startsWith("/") ? ref.slice(1) : join(dirname(from), ref));

const isLocal = (ref) => ref && !/^(https?:|mailto:|tel:|data:|#|\/\/)/i.test(ref);

// ---- 1. every referenced path exists ----
const reached = new Set();
let broken = 0;
for (const page of pages) {
  for (const [, , ref] of read(page).matchAll(/\b(href|src)="([^"]*)"/g)) {
    if (!isLocal(ref)) continue;
    const target = resolve(page, ref.split(/[?#]/)[0]);
    if (!existsSync(target)) { fail(`${page} -> ${ref} (missing ${target})`); broken++; }
    else reached.add(target);
  }
}
if (!broken) pass(`${pages.length} pages: every href/src resolves`);

// ---- 2. identical asset list on every page ----
const assetList = (page) =>
  [...read(page).matchAll(/\b(?:href|src)="([^"]*(?:assets\/[^"]*))"/g)]
    .map(([, ref]) => resolve(page, ref))
    .filter((t) => /\.(js|css)$/.test(t))
    .join("\n");

const reference = assetList(routed[0]);
const drifted = routed.filter((p) => assetList(p) !== reference);
if (drifted.length) {
  for (const page of drifted) {
    const mine = new Set(assetList(page).split("\n"));
    const theirs = new Set(reference.split("\n"));
    const only = (a, b) => [...a].filter((x) => !b.has(x));
    fail(`${page} loads a different asset set than ${routed[0]}\n` +
         `        extra:   ${only(mine, theirs).join(", ") || "(none)"}\n` +
         `        missing: ${only(theirs, mine).join(", ") || "(none)"}`);
  }
} else {
  pass(`${routed.length} pages: identical ordered asset list (${reference.split("\n").length} files)`);
}

// ---- 3. imports resolve, exactly once, and nothing under assets/ is orphaned ----
const INDEX = "assets/css/components.css";
const imported = [];
for (const [, ref] of read(INDEX).matchAll(/@import\s+"([^"]+)"/g)) {
  const target = resolve(INDEX, ref);
  imported.push(target);
  if (!existsSync(target)) fail(`${INDEX} imports a missing ${target}`);
  else reached.add(target);
}
const twice = imported.filter((t, i) => imported.indexOf(t) !== i);
if (twice.length) fail(`imported more than once: ${[...new Set(twice)].join(", ")}`);

const components = assets.filter((f) => f.startsWith("assets/css/components/"));
const orphanComponents = components.filter((f) => !imported.includes(f));
if (orphanComponents.length) fail(`under components/ but never imported: ${orphanComponents.join(", ")}`);
else pass(`${components.length} component stylesheets, each imported exactly once`);

const orphanAssets = assets.filter((f) => !reached.has(f));
if (orphanAssets.length) fail(`under assets/ but referenced by nothing: ${orphanAssets.join(", ")}`);
else pass(`${assets.length} assets, all reachable`);

done("assets");
