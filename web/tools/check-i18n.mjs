// Run from web/:  node tools/check-i18n.mjs
//
// The two tables under assets/js/i18n/ must hold the SAME keys. Nothing else
// catches a key added to fr.js and forgotten in en.js: DC.s() falls back to
// French so the page still renders, which is the right runtime behaviour and
// exactly what makes the omission invisible until a reader hits it.
//
// The files are plain browser scripts, not modules, so they run in a vm sandbox
// with a fake `window` rather than being imported. That compares what the
// browser actually receives instead of a regex guess at the source.
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { fail, pass, done } from "./lib/tree.mjs";

const LANGS = ["fr", "en"];

function table(lang) {
  const sandbox = { window: {} };
  runInNewContext(readFileSync(`assets/js/i18n/${lang}.js`, "utf8"), sandbox);
  const strings = sandbox.window.DC && sandbox.window.DC.strings;
  if (!strings || !strings[lang]) {
    fail(`assets/js/i18n/${lang}.js does not register window.DC.strings.${lang}`);
    return null;
  }
  return strings[lang];
}

const tables = Object.fromEntries(LANGS.map((l) => [l, table(l)]));
if (LANGS.some((l) => !tables[l])) done("i18n");

const keys = Object.fromEntries(LANGS.map((l) => [l, new Set(Object.keys(tables[l]))]));

let drift = 0;
for (const [a, b] of [["fr", "en"], ["en", "fr"]]) {
  for (const key of keys[a]) {
    if (!keys[b].has(key)) { fail(`${key} is in ${a}.js but not in ${b}.js`); drift++; }
  }
}
if (!drift) pass(`${keys.fr.size} keys, identical in ${LANGS.join(" and ")}`);

// An empty value renders as a blank label and no other check would notice.
let blank = 0;
for (const lang of LANGS) {
  for (const [key, value] of Object.entries(tables[lang])) {
    if (typeof value !== "string" || value.trim() === "") {
      fail(`${lang}.js: ${key} is not a non-empty string`);
      blank++;
    }
  }
}
if (!blank) pass("every value is a non-empty string");

// Every key the components file asks for exists. A typo in a DC.s("...") call
// renders the key itself on screen, which reads as a broken label and nothing
// else here would catch it.
// Three shapes carry a key: a direct s("...") call, the second member of a nav
// or sidebar entry, and the label a sidebar group opens on.
const source = readFileSync("assets/js/chrome/components.js", "utf8");
const asked = new Set([
  ...[...source.matchAll(/\bs\("([A-Za-z]+)"\)/g)].map((m) => m[1]),
  ...[...source.matchAll(/,\s*"([a-z][A-Za-z]+)"\]/g)].map((m) => m[1]),
  ...[...source.matchAll(/\["([a-z][A-Za-z]+)",\s*\[/g)].map((m) => m[1])
]);
let unknown = 0;
for (const key of asked) {
  if (!keys.fr.has(key)) { fail(`components.js asks for "${key}", absent from fr.js`); unknown++; }
}
if (!unknown) pass(`${asked.size} keys asked for by components.js, all present`);

done("i18n");
