// Shared file walking and text scrubbing for the checks in tools/.
// Dependency-free like everything else here: the site has no package.json and
// the checks have to keep running with nothing installed.
import { readdirSync, statSync, readFileSync } from "node:fs";
import { join } from "node:path";

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
// payloads, quoted strings and hex colours. Without it, "#5fc8d2" contributes
// an id named after itself and every dead-code check drowns in phantoms.
export function selectorsOnly(css) {
  return css
    .replace(/\/\*[\s\S]*?\*\//g, " ")
    .replace(/url\((?:[^()]|\([^()]*\))*\)/g, "url()")
    .replace(/"[^"]*"|'[^']*'/g, '""')
    .replace(/#[0-9a-fA-F]{3,8}\b/g, "#");
}

export const CLASS_RE = /\.(-?[A-Za-z_][\w-]*)/g;

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
