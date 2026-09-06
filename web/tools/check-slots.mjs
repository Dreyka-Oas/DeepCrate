// Run from web/:  node tools/check-slots.mjs
//
// KEEP. Every source file has to say whether it is yours to change or whether
// it works as it stands, and the two home pages have to stay the same page in
// two languages. Without this the markers rot the way stale comments always
// do: a file gets added, nobody classifies it, and the next person is back to
// reading the whole tree to find out what is safe to delete.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";
import { existsSync } from "node:fs";

const HEAD_LINES = 20;
const MARKER = /^\s*(?:\/\/|\/\*|\*)?\s*(?:SLOT|KEEP)\b/m;

// ---- 1. every source file is classified ----
const source = [...byExt(walk("assets"), ".css", ".js"), ...byExt(walk("tools"), ".mjs")];
let unmarked = 0;
for (const f of source) {
  const head = read(f).split("\n").slice(0, HEAD_LINES).join("\n");
  if (MARKER.test(head)) continue;
  fail(`${f}: no SLOT or KEEP marker in the first ${HEAD_LINES} lines`);
  unmarked++;
}
if (!unmarked) pass(`${source.length} source files, each marked SLOT or KEEP`);

// ---- 2. the two home pages hold the same sections ----
// A band deleted in one tree and left in the other is the one mistake the
// other checks cannot see: both pages still parse, both still load the same
// assets, and the site simply says two different things depending on the
// language button.
const FR = "lang/fr/index.html";
const EN = "lang/en/index.html";
const count = (f, re) => (read(f).match(re) || []).length;
const PAIRS = [
  ["optional-section fences", /SECTION FACULTATIVE/g, /OPTIONAL SECTION/g],
  ["home sections", /<section class="home-/g, /<section class="home-/g],
  ["separators", /<site-ledger/g, /<site-ledger/g],
];
let drift = 0;
for (const [what, frRe, enRe] of PAIRS) {
  const a = count(FR, frRe);
  const b = count(EN, enRe);
  if (a !== b) { fail(`${what}: ${a} in ${FR}, ${b} in ${EN}`); drift++; }
  else if (a === 0) { fail(`${what}: none found in either home page`); drift++; }
}
if (!drift) pass("the two home pages carry the same bands, each one fenced");

// ---- 3. the component manifest and the folder agree ----
// Deleting a band means the markup, the stylesheet and the @import line. The
// third one is the one people skip, and a stylesheet left imported after its
// markup is gone shows up in check-source.mjs as a rule with no class, which
// points at the wrong culprit. Catch it here instead.
const MANIFEST = "assets/css/components.css";
const imported = [...read(MANIFEST).matchAll(/@import\s+"([^"]+)"/g)].map((m) => m[1]);
const onDisk = byExt(walk("assets/css/components"), ".css").map((f) =>
  f.replace("assets/css/", ""));
let mismatch = 0;
for (const rel of imported) {
  if (!existsSync(`assets/css/${rel}`)) { fail(`${MANIFEST} imports ${rel}, which does not exist`); mismatch++; }
}
for (const rel of onDisk) {
  if (!imported.includes(rel)) { fail(`assets/css/${rel} exists but ${MANIFEST} never imports it`); mismatch++; }
}
if (!mismatch) pass(`${imported.length} components, each imported once and present on disk`);

// ---- 4. the map is still there ----
// SLOTS.md is what a filling AI reads first. check-source.mjs already proves
// every filename it names exists, so all that is left is proving it exists at
// all and still opens on the two markers it explains.
const MAP = "SLOTS.md";
if (!existsSync(MAP)) fail(`${MAP} is missing: the tree has markers nobody can read as a whole`);
else {
  const map = read(MAP);
  const holes = ["SLOT", "KEEP", "check.mjs"].filter((w) => !map.includes(w));
  if (holes.length) fail(`${MAP} never mentions: ${holes.join(", ")}`);
  else pass(`${MAP} present, and it explains both markers`);
}

done("slots");
