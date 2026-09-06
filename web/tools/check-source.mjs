// Run from web/:  node tools/check-source.mjs
//
// Dead weight in a tree with no build step, which no compiler is going to
// complain about. Two directions, both of which have already bitten:
//
//   1. A class written in a stylesheet and used by no page and no script. It
//      survives every rename around it and ships in the deploy for nothing.
//   2. A class used in a page or built in a script that no stylesheet defines.
//      That one renders as plain unstyled markup, which is the worse half.
import { walk, read, byExt, selectorsOnly, CLASS_RE, fail, pass, done } from "./lib/tree.mjs";

const CSS = byExt(walk("assets/css"), ".css");
const PAGES = byExt(walk("."), ".html");
const SCRIPTS = byExt(walk("assets/js"), ".js");

// Classes a browser is never told about by us: the state classes JS adds and
// removes, and the ones a stylesheet only ever names next to another selector.
const RUNTIME = new Set(["reveal-ready", "in-view"]);

const defined = new Set();
for (const f of CSS) {
  for (const m of selectorsOnly(read(f)).matchAll(CLASS_RE)) defined.add(m[1]);
}

// class="a b" in the HTML, plus className = "..." and classList.add("...") in
// the scripts, which is how the rendered components get theirs.
const used = new Set(RUNTIME);
for (const f of PAGES) {
  for (const m of read(f).matchAll(/class="([^"]*)"/g)) {
    for (const name of m[1].split(/\s+/)) if (name) used.add(name);
  }
}
for (const f of SCRIPTS) {
  const source = read(f);
  for (const m of source.matchAll(/className\s*=\s*"([^"]*)"/g)) {
    for (const name of m[1].split(/\s+/)) if (name) used.add(name);
  }
  for (const m of source.matchAll(/classList\.(?:add|remove|toggle)\("([^"]*)"\)/g)) used.add(m[1]);
  for (const m of source.matchAll(/class="([^"]*)"/g)) {
    for (const name of m[1].split(/\s+/)) if (name) used.add(name);
  }
}

const orphanRules = [...defined].filter((c) => !used.has(c)).sort();
if (orphanRules.length) fail(`defined in CSS, used nowhere: ${orphanRules.join(", ")}`);
else pass(`${defined.size} classes defined, every one of them used`);

const unstyled = [...used].filter((c) => !defined.has(c)).sort();
if (unstyled.length) fail(`used in the markup, defined by no stylesheet: ${unstyled.join(", ")}`);
else pass(`${used.size} classes used, every one of them defined`);

done("source");
