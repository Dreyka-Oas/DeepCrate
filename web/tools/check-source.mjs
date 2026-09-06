// Run from web/:  node tools/check-source.mjs
//
// KEEP. Source hygiene, the budgets this tree holds itself to:
// no file over 150 lines, no folder over 8 files, no CSS class or custom
// property declared and never used, and no comment pointing at a file that no
// longer exists. The last one sounds cosmetic and is not: this tree accumulated
// a dozen references to an app.js deleted long ago, and every one of them sent
// a reader looking for code that was somewhere else entirely.
import { walk, read, byExt, selectorsOnly, CLASS_RE, ID_RE, fail, pass, done } from "./lib/tree.mjs";
import { basename, dirname } from "node:path";

const MAX_LINES = 150;
const MAX_FILES_PER_FOLDER = 8;

const files = walk();
const source = byExt(files, ".js", ".mjs", ".css");
const css = byExt(files, ".css");
const consumers = [...byExt(files, ".html"), ...byExt(files, ".js")].map(read).join("\n");

// ---- 1. file length ----
const long = source
  .map((f) => [f, read(f).split("\n").length])
  .filter(([, n]) => n > MAX_LINES);
if (long.length) for (const [f, n] of long) fail(`${f}: ${n} lines (max ${MAX_LINES})`);
else pass(`${source.length} source files, none over ${MAX_LINES} lines`);

// ---- 2. folder width ----
// Scoped to code: a folder of eleven wiki pages is eleven public URLs, not a
// missing sub-package, and splitting it would rewrite them for nothing.
const perFolder = new Map();
for (const f of source) perFolder.set(dirname(f), (perFolder.get(dirname(f)) || 0) + 1);
const wide = [...perFolder].filter(([, n]) => n > MAX_FILES_PER_FOLDER);
if (wide.length) for (const [d, n] of wide) fail(`${d}/: ${n} files (max ${MAX_FILES_PER_FOLDER})`);
else pass(`${perFolder.size} folders, none over ${MAX_FILES_PER_FOLDER} files`);

// ---- 3. CSS classes declared but never used ----
const used = new Set();
for (const m of consumers.matchAll(/class="([^"]*)"/g)) for (const c of m[1].split(/\s+/)) used.add(c);
for (const m of consumers.matchAll(/className\s*=\s*["'`]([^"'`]+)/g)) for (const c of m[1].split(/\s+/)) used.add(c);
for (const m of consumers.matchAll(/classList\.\w+\(\s*["'`]([^"'`]+)/g)) used.add(m[1]);
for (const m of consumers.matchAll(/(?:querySelector|querySelectorAll|matches|closest)\(\s*["'`]([^"'`]+)/g))
  for (const c of m[1].matchAll(CLASS_RE)) used.add(c[1]);

const declared = new Map();
for (const f of css) for (const m of selectorsOnly(read(f)).matchAll(CLASS_RE)) {
  if (!declared.has(m[1])) declared.set(m[1], f);
}
const deadClasses = [...declared].filter(([c]) => !used.has(c));
if (deadClasses.length) for (const [c, f] of deadClasses) fail(`.${c} styled in ${f}, used nowhere`);
else pass(`${declared.size} CSS classes, all used`);

// ---- 4. custom properties declared but never read ----
const cssText = css.map(read).join("\n");
const propUsed = new Set();
for (const m of (cssText + consumers).matchAll(/var\(\s*(--[\w-]+)/g)) propUsed.add(m[1]);
for (const m of consumers.matchAll(/(?:setProperty|getPropertyValue)\(\s*["'`](--[\w-]+)/g)) propUsed.add(m[1]);
for (const m of consumers.matchAll(/style="[^"]*?(--[\w-]+)\s*:/g)) propUsed.add(m[1]);
const deadProps = new Set();
for (const f of css) for (const m of read(f).matchAll(/^\s*(--[\w-]+)\s*:/gm)) {
  if (!propUsed.has(m[1])) deadProps.add(`${m[1]} (${f})`);
}
if (deadProps.size) for (const p of deadProps) fail(`custom property declared, never read: ${p}`);
else pass("every custom property is read somewhere");

// ---- 4b. custom properties read but never declared ----
// L'inverse de la règle précédente, et le vrai piège : retirer un token de
// tokens.css laisse un var() silencieusement indéfini, la règle CSS tombe et
// rien ne le signale. Les propriétés natives (--*) posées en ligne comptent.
const propDeclared = new Set();
for (const f of css) for (const m of read(f).matchAll(/^\s*(--[\w-]+)\s*:/gm)) propDeclared.add(m[1]);
for (const m of consumers.matchAll(/style="[^"]*?(--[\w-]+)\s*:/g)) propDeclared.add(m[1]);
for (const m of consumers.matchAll(/setProperty\(\s*["'`](--[\w-]+)/g)) propDeclared.add(m[1]);
const undeclared = [...propUsed].filter((p) => !propDeclared.has(p));
if (undeclared.length) for (const p of undeclared) fail(`custom property read, never declared: ${p}`);
else pass(`${propDeclared.size} custom properties, each declared before it is read`);

// ---- 5. ids styled or looked up but absent from the markup ----
const knownIds = new Set();
for (const m of consumers.matchAll(/\bid\s*=\s*"([^"]+)"/g)) knownIds.add(m[1]);
for (const m of consumers.matchAll(/\.id\s*=\s*["'`]([^"'`]+)/g)) knownIds.add(m[1]);
const missingIds = new Set();
for (const f of css) for (const m of selectorsOnly(read(f)).matchAll(ID_RE)) {
  if (!knownIds.has(m[1])) missingIds.add(`#${m[1]} (${f})`);
}
for (const m of consumers.matchAll(/getElementById\(\s*["'`]([^"'`]+)/g)) {
  if (!knownIds.has(m[1])) missingIds.add(`#${m[1]} (getElementById)`);
}
if (missingIds.size) for (const i of missingIds) fail(`id targeted but never rendered: ${i}`);
else pass("every id targeted by CSS or JS exists");

// ---- 6. filenames named in comments must exist ----
// Two names deliberately point outside this tree: the sync script lives in the
// sibling feedback-hub repo, and this check names the app.js whose leftover
// references are the reason it exists.
const ELSEWHERE = {
  "assets/js/core/site-lang.js": ["sync-project.mjs"],
  "tools/check-source.mjs": ["app.js"],
};
const known = new Set(files.map((f) => basename(f)));
const stale = new Set();
for (const f of [...source, ...byExt(files, ".md")]) {
  read(f).split("\n").forEach((line, i) => {
    if (!/\/\/|\/\*|\*|<!--|\|/.test(line)) return;
    for (const m of line.matchAll(/\b([\w-]+\.(?:js|mjs|css))\b/g)) {
      if (known.has(m[1]) || (ELSEWHERE[f] || []).includes(m[1])) continue;
      stale.add(`${m[1]} named in ${f}:${i + 1}`);
    }
  });
}
if (stale.size) for (const s of stale) fail(`comment points at a file that does not exist: ${s}`);
else pass("every filename named in a comment exists");

done("source");
