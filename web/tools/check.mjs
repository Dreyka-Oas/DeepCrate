// Run from web/:  node tools/check.mjs
//
// Runs every check in this folder and exits non-zero if any of them failed, so
// one command gates a commit. Each check also runs on its own when you want
// just its output.
import { spawnSync } from "node:child_process";

const CHECKS = ["check-assets.mjs", "check-lang.mjs", "check-i18n.mjs",
                "check-source.mjs", "check-tells.mjs", "check-design.mjs"];

let failed = 0;
for (const check of CHECKS) {
  console.log(`\n=== ${check} ${"=".repeat(Math.max(0, 54 - check.length))}`);
  const run = spawnSync(process.execPath, [`tools/${check}`], { stdio: "inherit" });
  if (run.status !== 0) failed++;
}

console.log(failed === 0
  ? `\nall ${CHECKS.length} checks passed`
  : `\n${failed} of ${CHECKS.length} checks failed`);
process.exit(failed === 0 ? 0 : 1);
