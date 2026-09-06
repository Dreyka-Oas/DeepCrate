// Run from web/:  node tools/check-tells.mjs
//
// SLOT pour ses listes de mots, KEEP pour le reste. Les sections 1 et 4
// portent le registre du site d'origine et se réécrivent; la typographie de la
// section 3 et les mots creux de la section 5 valent pour tous les sites.
//
// Ce que le site ne doit plus contenir : vocabulaire de document classifié,
// marqueurs d'authenticité, typographie composée à la machine, et le tic de
// registre (un même mot de vocabulaire dans presque chaque paragraphe).
// La section 3 prend les commentaires CSS, JS et .mjs en plus des pages : ils
// ne sont pas rendus, mais ils se lisent sur GitHub, et c'est là que le tic
// s'était réfugié.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const PAGES = byExt(walk("lang/fr"), ".html");
const EN_PAGES = byExt(walk("lang/en"), ".html");
const I18N_JS = ["assets/js/i18n/fr.js", "assets/js/i18n/en.js"];
const CODE = [...byExt(walk("assets"), ".css", ".js"), ...byExt(walk("tools"), ".mjs")];

// ---- 1. vocabulaire de registre interdit ----
// SLOT. Cette liste est celle du site d'origine, qui se donnait des airs de
// dossier classifié et voulait s'en débarrasser. Un autre mod a d'autres tics :
// remplacer par les mots que CE site ne doit pas porter, en majuscules comme
// ils apparaissent dans les pages. Vider la liste désactive la section.
const WORDS = [
  // Ce site parle de rangement, pas de performances ni de promesses. Ces
  // mots-là sont ceux qu'une page de mod attrape sans y penser, et aucun ne
  // désigne un chiffre que le mod pourrait montrer.
  "RÉVOLUTIONNAIRE", "REVOLUTIONARY", "ULTIME", "ULTIMATE",
  "INFINI", "INFINITE", "ILLIMITÉ", "UNLIMITED",
  "INDISPENSABLE", "MUST-HAVE", "SANS EFFORT", "EFFORTLESS",
];
let vocab = 0;
for (const f of [...PAGES, ...EN_PAGES, ...I18N_JS]) {
  const body = read(f);
  for (const w of WORDS) {
    if (!body.includes(w)) continue;
    const line = body.slice(0, body.indexOf(w)).split("\n").length;
    fail(`marqueur interdit "${w}" dans ${f}:${line}`);
    vocab++;
  }
}
if (!vocab) pass(`${PAGES.length + EN_PAGES.length + I18N_JS.length} fichiers, aucun marqueur de document classifié`);

// ---- 2. marqueurs structurels (filigrane, tampon, caviardage) ----
const MARKUP = ["home-hero__watermark", "home-title__stamp", "site-loader__stamp",
                "home-scrollcue", 'class="redact"', "redact__bar", "redact-spark"];
let markup = 0;
for (const f of [...PAGES, ...EN_PAGES, ...byExt(walk("assets"), ".css", ".js")]) {
  for (const m of MARKUP) if (read(f).includes(m)) { fail(`marqueur "${m}" encore présent dans ${f}`); markup++; }
}
if (!markup) pass("aucun filigrane, tampon, indicatif de défilement ni caviardage");

// ---- 3. typographie ----
// Les caractères sont construits par code point : les écrire en clair ferait de
// ce fichier le seul du dépôt à porter ce qu'il interdit, et il devrait alors
// s'exempter lui-même.
//
// Le périmètre couvre les pages, les catalogues ET le code. Les chevrons ne
// vivaient que dans six pages wiki et leurs catalogues, les cadratins que dans
// les commentaires : ne surveiller qu'une moitié laisserait l'autre revenir.
//
// U+00A0 est absent de la liste : la seule occurrence du dépôt est dans
// assets/js/page/home.js, où l'insécable remplace une espace pour l'animation
// caractère par caractère du wordmark. C'est du code, pas de la typographie.
const BANNED = [
  [0x2014, "tiret cadratin"], [0x2013, "demi-cadratin"],
  [0x201c, "guillemet courbe ouvrant"], [0x201d, "guillemet courbe fermant"],
  [0x2018, "apostrophe courbe ouvrante"], [0x2019, "apostrophe courbe fermante"],
  [0x00ab, "guillemet chevron ouvrant"], [0x00bb, "guillemet chevron fermant"],
  [0x202f, "insécable étroite"], [0x2009, "espace fine"],
  [0x200b, "espace sans chasse"], [0xfeff, "marque d'ordre des octets"],
  [0x00ad, "trait d'union conditionnel"],
];
// Encoder le caractère contourne la liste ci-dessus sans rien changer au rendu.
const ENTITIES = /&(mdash|ndash|laquo|raquo|ldquo|rdquo|lsquo|rsquo|shy|thinsp|nbsp);/g;
const TEXT = [...PAGES, ...EN_PAGES, ...CODE];
let typo = 0;
for (const f of TEXT) {
  const lines = read(f).split("\n");
  for (const [i, line] of lines.entries()) {
    for (const [cp, name] of BANNED) {
      if (line.includes(String.fromCharCode(cp))) { fail(`${f}:${i + 1}: ${name}`); typo++; }
    }
    for (const m of line.matchAll(ENTITIES)) { fail(`${f}:${i + 1}: entité ${m[0]}`); typo++; }
  }
}
if (!typo) pass(`${TEXT.length} fichiers, aucun marqueur typographique`);

// ---- 4. tic de registre ----
// Plafond par page, texte balises retirées, dans les deux langues : les pages
// EN sont écrites à la main dans lang/en/, mais elles portent le même tic que
// leur équivalent FR.
// SLOT. Les mots listés sont ceux du registre propre au site : ceux qu'on se
// surprend à répéter dans presque chaque paragraphe. Pour les trouver, écrire
// deux ou trois pages, puis compter. Le plafond se choisit bas, autour de trois
// ou quatre par page. Les deux tables sont indépendantes : un tic français n'a
// pas toujours d'équivalent anglais.
// Comptés sur les huit pages du guide une fois écrites, puis plafonnés un cran
// au-dessus du pire cas : le plafond attrape la dérive de la page suivante, il
// ne condamne pas ce qui est déjà écrit. "coffre", "case" et "module" n'y sont
// pas, ce sont les sujets du site et non des tics.
const FR_CEILING = { "donc": 5, "plutôt": 4, "c'est": 4 };
const EN_CEILING = { "rather than": 4, "therefore": 4 };
let tics = 0;
for (const [pages, ceiling] of [[PAGES, FR_CEILING], [EN_PAGES, EN_CEILING]]) {
  for (const f of pages) {
    const text = read(f).replace(/<[^>]+>/g, " ").toLowerCase();
    for (const [w, max] of Object.entries(ceiling)) {
      const n = (text.match(new RegExp(w, "g")) || []).length;
      if (n > max) { fail(`${f}: "${w}" ×${n} (plafond ${max})`); tics++; }
    }
  }
}
if (!tics) pass(`${PAGES.length + EN_PAGES.length} pages, aucun mot de registre au-dessus de son plafond`);

// ---- 5. mots creux ----
// Cette liste-ci vaut pour tous les sites et se garde telle quelle. Un mot y
// entre quand il ne désigne rien de mesurable. Attention à ne pas y mettre un
// terme de jeu : "unlock" en est absent parce que sur un site de mod il décrit
// une mécanique réelle et non du remplissage.
const HOLLOW = ["delve", "tapestry", "seamless", "robust", "elevate",
                "unleash", "harness", "leverage", "foster", "crucial", "game-changer", "cutting-edge"];
const HOLLOW_SOURCES = [...EN_PAGES, ...I18N_JS];
let hollow = 0;
for (const f of HOLLOW_SOURCES) {
  const text = read(f).toLowerCase();
  for (const w of HOLLOW) if (new RegExp(`\\b${w}\\b`).test(text)) { fail(`mot creux "${w}" dans ${f}`); hollow++; }
}
if (!hollow) pass(`${HOLLOW_SOURCES.length} fichiers, aucun mot creux dans les pages EN`);

done("tells");
