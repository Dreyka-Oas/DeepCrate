// Run from web/:  node tools/check-tells.mjs
//
// What the site must not contain: machine-set typography, the filler words of
// both languages, the horizontal rule as a section break, and any pictograph.
// The scope covers the code as well as the pages, because comments are not
// rendered but they are read on the forge, and that is where a tell hides.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const FR_PAGES = byExt(walk("lang/fr"), ".html");
const EN_PAGES = byExt(walk("lang/en"), ".html");
const CODE = [...byExt(walk("assets"), ".css", ".js"), ...byExt(walk("tools"), ".mjs")];
const ALL = ["index.html", ...FR_PAGES, ...EN_PAGES, ...CODE];

// ---- 1. typography ----
// The characters are built from their code point. Written out, this file would
// be the only one in the tree carrying what it forbids, and would then have to
// exempt itself.
const BANNED = [
  [0x2014, "em dash"], [0x2013, "en dash"],
  [0x201c, "curly double quote, opening"], [0x201d, "curly double quote, closing"],
  [0x2018, "curly single quote, opening"], [0x2019, "curly single quote, closing"],
  [0x00ab, "chevron, opening"], [0x00bb, "chevron, closing"],
  [0x202f, "narrow no-break space"], [0x2009, "thin space"],
  [0x00a0, "no-break space"], [0x200b, "zero-width space"],
  [0xfeff, "byte order mark"], [0x00ad, "soft hyphen"]
];
// Encoding the character walks around the list above without changing a pixel.
const ENTITIES = /&(mdash|ndash|laquo|raquo|ldquo|rdquo|lsquo|rsquo|shy|thinsp|nbsp);/g;
let typo = 0;
for (const f of ALL) {
  const lines = read(f).split("\n");
  for (const [i, line] of lines.entries()) {
    for (const [cp, name] of BANNED) {
      if (line.includes(String.fromCharCode(cp))) { fail(`${f}:${i + 1}: ${name}`); typo++; }
    }
    for (const m of line.matchAll(ENTITIES)) { fail(`${f}:${i + 1}: entity ${m[0]}`); typo++; }
  }
}
if (!typo) pass(`${ALL.length} files, no typographic marker`);

// ---- 2. the horizontal rule as a section break, markup and stylesheet ----
// The tag is the obvious half. The other half is a border drawn above or below
// a heading, which renders as the same line and is how the rule crept back in
// once already.
let rules = 0;
for (const f of [...FR_PAGES, ...EN_PAGES, "index.html"]) {
  const lines = read(f).split("\n");
  for (const [i, line] of lines.entries()) {
    if (/<hr\b/i.test(line)) { fail(`${f}:${i + 1}: <hr> used as a section break`); rules++; }
  }
}
for (const f of byExt(walk("assets/css"), ".css")) {
  for (const m of read(f).matchAll(/([^{}]+)\{([^}]*)\}/g)) {
    const [, selector, body] = m;
    if (!/\bh[1-4]\b/.test(selector)) continue;
    if (/border-(top|bottom)\s*:(?!\s*none)/.test(body)) {
      fail(`${f}: ${selector.trim().split("\n").pop()} draws a rule above or below a heading`);
      rules++;
    }
  }
}
if (!rules) pass("no horizontal rule, in markup or stylesheet");

// ---- 3. pictographs ----
let emoji = 0;
for (const f of ALL) {
  const lines = read(f).split("\n");
  for (const [i, line] of lines.entries()) {
    const m = line.match(/\p{Extended_Pictographic}/u);
    if (m) { fail(`${f}:${i + 1}: pictograph ${JSON.stringify(m[0])}`); emoji++; }
  }
}
if (!emoji) pass("no pictograph anywhere in the tree");

// ---- 4. filler ----
// A word here does not name anything measurable. The subject words of the site,
// crate, slot, module, row, are deliberately absent: repeating them is what a
// page about crates does.
const FR_FILLER = ["crucial", "essentiel", "indispensable", "fondamental", "captivant",
                   "par ailleurs", "notamment", "il est important de noter", "en résumé"];
const EN_FILLER = ["delve", "tapestry", "seamless", "robust", "elevate", "unleash",
                   "leverage", "foster", "crucial", "game-changer", "cutting-edge", "streamline"];
let filler = 0;
for (const [sources, words, label] of [
  [[...FR_PAGES, "assets/js/i18n/fr.js"], FR_FILLER, "fr"],
  [[...EN_PAGES, "assets/js/i18n/en.js"], EN_FILLER, "en"]
]) {
  for (const f of sources) {
    const text = read(f).replace(/<[^>]+>/g, " ").toLowerCase();
    for (const w of words) {
      if (text.includes(w)) { fail(`filler "${w}" in ${f} (${label})`); filler++; }
    }
  }
}
if (!filler) pass("no filler word in either tree");

done("tells");
