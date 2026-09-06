// Run from web/:  node tools/check-i18n.mjs
//
// KEEP. The two language tables under assets/js/i18n/ must hold the SAME keys. Nothing
// else catches a key added to fr.js and forgotten in en.js: SITE.s() falls back to
// French so the page still renders, which is the right runtime behaviour and
// exactly what makes the omission invisible until a reader hits it.
//
// The files are plain browser scripts, not modules, so they are run in a vm
// sandbox with a fake `window` rather than imported. Dependency-free, like every
// other check here, and it compares what the browser actually receives instead
// of a regex guess at the source.
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { fail, pass, done } from "./lib/tree.mjs";

const LANGS = ["fr", "en"];

function table(lang) {
  const sandbox = { window: {} };
  runInNewContext(readFileSync(`assets/js/i18n/${lang}.js`, "utf8"), sandbox);
  const strings = sandbox.window.SITE && sandbox.window.SITE.strings;
  if (!strings || !strings[lang]) {
    fail(`assets/js/i18n/${lang}.js does not register window.SITE.strings.${lang}`);
    return null;
  }
  return strings[lang];
}

// Every leaf as a [path, value] pair. Array indices come out as path segments
// too, so a table carrying 14 phases where the other carries 15 is a reported
// difference rather than a silent truncation at render time. The separator is
// " › " and not "." because the keys themselves contain dots (/wiki/mechanic-6.html).
const leaves = (value, prefix = "") =>
  value !== null && typeof value === "object"
    ? Object.keys(value).flatMap((k) => leaves(value[k], prefix ? prefix + " › " + k : k))
    : [[prefix, value]];

const tables = Object.fromEntries(LANGS.map((l) => [l, table(l)]));
if (LANGS.some((l) => !tables[l])) done("i18n");

const keys = Object.fromEntries(LANGS.map((l) => [l, new Set(leaves(tables[l]).map(([k]) => k))]));

let drift = 0;
for (const [a, b] of [["fr", "en"], ["en", "fr"]]) {
  for (const key of keys[a]) {
    if (!keys[b].has(key)) { fail(`${key} is in ${a}.js but not in ${b}.js`); drift++; }
  }
}
if (!drift) pass(`${keys.fr.size} keys, identical in ${LANGS.join(" and ")}`);

// An empty value renders as a blank label, and no other check would notice.
let blank = 0;
for (const lang of LANGS) {
  for (const [key, value] of leaves(tables[lang])) {
    if (typeof value !== "string" || value.trim() === "") {
      fail(`${lang}.js › ${key} is not a non-empty string`);
      blank++;
    }
  }
}
if (!blank) pass("every value is a non-empty string");

done("i18n");
