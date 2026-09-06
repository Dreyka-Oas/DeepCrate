// Run from web/:  node tools/check-design.mjs
//
// KEEP, except the four thresholds at the top of the file, which are a SLOT: a
// site with three bands rather than seven wants a lower page-skeleton ceiling.
// Lowering them all to zero turns the check off and is exactly what it exists
// to prevent, so move one at a time and say why.
//
// L'uniformité parfaite est le tell, pas la valeur choisie. Ce check mesure la
// variété : combien de courbes d'easing distinctes, combien de distances de
// reveal, combien de textures de fond empilées, et combien de squelettes de
// page différents dans le wiki.
import { walk, read, byExt, fail, pass, done } from "./lib/tree.mjs";

const CSS = byExt(walk("assets/css"), ".css");
const allCss = CSS.map(read).join("\n");

// ---- 1. la courbe qu'on retrouve sur un site sur deux ----
if (/cubic-bezier\(\s*0?\.16\s*,\s*1\s*,\s*0?\.3\s*,\s*1\s*\)/.test(allCss) || allCss.includes("--ease-out-expo"))
  fail("cubic-bezier(0.16, 1, 0.3, 1) / --ease-out-expo encore présent");
else pass("la courbe signature n'est plus utilisée");

// ---- 2. variété des courbes ----
const curves = new Set([...allCss.matchAll(/cubic-bezier\([^)]+\)/g)].map((m) => m[0].replace(/\s/g, "")));
if (curves.size < 3) fail(`${curves.size} courbe(s) d'easing distincte(s), minimum 3`);
else pass(`${curves.size} courbes d'easing distinctes`);

// ---- 3. variété des reveals ----
const dists = new Set([...allCss.matchAll(/translateY\((-?[\d.]+)px\)/g)].map((m) => m[1]));
if (dists.size < 3) fail(`${dists.size} distance(s) de reveal distincte(s), minimum 3`);
else pass(`${dists.size} distances de reveal distinctes`);

// ---- 4. une seule texture de fond ----
const TEXTURES = [
  ["grain plein écran", /feTurbulence|var\(--grain\)/],
  ["glow radial sur body", /radial-gradient\([^)]*--page-radial/],
  ["grille du hero", /home-hero__grid/],
];
const on = TEXTURES.filter(([, re]) => re.test(allCss)).map(([n]) => n);
if (on.length > 1) fail(`${on.length} textures de fond empilées : ${on.join(", ")}`);
else pass(`${on.length} texture de fond`);

// ---- 5. diversité de squelette dans le wiki ----
// Signature = suite ordonnée des blocs structurants. Onze pages avec la même
// signature, c'est un gabarit appliqué mécaniquement, pas une mise en page.
// Le périmètre reste lang/fr/wiki. Les ouvertures EN sont hors périmètre par
// choix, pas par oubli : les deux trees sont écrits à la main désormais, et
// rien ne garantit qu'une page EN partage la structure de son homologue FR.
const BLOCKS = /breadcrumb|<h1|<site-ledger|<h2|param-table|callout--\w+|stage-flow/g;
const WIKI = byExt(walk("lang/fr/wiki"), ".html");
const sigs = new Map();
for (const f of WIKI) {
  const sig = (read(f).match(BLOCKS) || []).slice(0, 5).join(">");
  sigs.set(sig, (sigs.get(sig) || 0) + 1);
}
const worst = Math.max(...sigs.values());
if (worst > 4) fail(`${worst} pages wiki partagent la même ouverture (maximum 4)`);
else pass(`${sigs.size} ouvertures distinctes sur ${WIKI.length} pages wiki`);

done("design");
