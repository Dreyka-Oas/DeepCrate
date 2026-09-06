// KEEP.
// Shared file-walking and text-scrubbing helpers for the checks in tools/.
// Dependency-free, like everything else here: the site has no package.json and
// the checks must keep running with nothing installed.
import { readdirSync, statSync, readFileSync } from "node:fs";
import { join } from "node:path";

// Un dossier en point porte de l'outillage local, jamais du contenu de site.
// La règle par nom plutôt qu'une liste : ce qu'on installe demain sera ignoré
// sans qu'il faille y penser.
const IGNORED = new Set(["node_modules"]);

export function walk(dir = ".", out = []) {
  for (const name of readdirSync(dir)) {
    if (IGNORED.has(name)) continue;
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      if (!name.startsWith(".")) walk(path, out);
    } else out.push(path.replace(/^\.\//, ""));
  }
  return out;
}

export const read = (file) => readFileSync(file, "utf8");
export const byExt = (files, ...exts) => files.filter((f) => exts.some((e) => f.endsWith(e)));

// CSS with everything that is not a selector neutralised: comments, url()
// payloads, quoted strings and hex colours. Without this, the data: URI of the
// grain filter contributes a ".w3" class and "#f16436" an "#f16436" id, and
// every dead-code check drowns in phantoms.
export function selectorsOnly(css) {
  return css
    .replace(/\/\*[\s\S]*?\*\//g, " ")
    .replace(/url\((?:[^()]|\([^()]*\))*\)/g, "url()")
    .replace(/"[^"]*"|'[^']*'/g, '""')
    .replace(/#[0-9a-fA-F]{3,8}\b/g, "#");
}

export const CLASS_RE = /\.(-?[A-Za-z_][\w-]*)/g;
export const ID_RE = /#(-?[A-Za-z_][\w-]*)/g;

let failures = 0;
export function fail(message) {
  failures++;
  console.error("FAIL  " + message);
}
export function pass(message) {
  console.log("ok    " + message);
}
export function done(label) {
  console.log(failures === 0 ? `\n${label}: ok` : `\n${label}: ${failures} problem(s)`);
  process.exit(failures === 0 ? 0 : 1);
}
