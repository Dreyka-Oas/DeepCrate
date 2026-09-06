package com.dreykaoas.deepcrate.config.io.diag;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What a parsed file looks like next to the schema, and the kinds of drift worth naming on their own.
 *
 * Held apart from the walk that fills it: this is the vocabulary the loader, the log and the operator
 * notice all read, while the walk is one method's business.
 */
public final class ConfigDrift {
    private ConfigDrift() {}

    /**
     * A name matching no option and not repairable, with the closest real one when something is near
     * enough to be worth naming. Null when nothing is close, a guess in the air being worse than none.
     */
    public record Unknown(String name, @Nullable String suggestion) {}

    /** A misspelling the loader settles alone: the value of {@code from} is applied to {@code to}. */
    public record Rename(String from, String to) {}

    /**
     * @param keysInFile every key in an option position, at the root or inside a category
     * @param recognised how many of those name a real option
     * @param unknown names matching nothing and not repairable, so the value written there is lost
     * @param renamed misspellings settled alone, the value kept and the key rewritten
     * @param duplicated options written under two categories, where the flattening is last wins
     * @param bogusCategory root objects whose name is not a category
     * @param misplaced right name, wrong category, which the rewrite corrects on its own
     */
    public record Report(
        int keysInFile,
        int recognised,
        List<Unknown> unknown,
        List<Rename> renamed,
        List<String> duplicated,
        List<String> bogusCategory,
        List<String> misplaced
    ) {
        /**
         * The file had content and not one key of it was recognisable, which is the only signal strong
         * enough to justify starting over: anything weaker and the rewrite would throw away the
         * settings that are still readable. A file of nothing but typos is not unusable, every one of
         * them is about to be repaired.
         */
        public boolean unusable() {
            return this.keysInFile > 0 && this.recognised == 0 && this.renamed.isEmpty();
        }

        /**
         * Nothing here needs the administrator.
         *
         * Drift the loader repairs alone does not count: a typo, an option under the wrong category
         * and a stale category name are all corrected by the write that follows the read. What stays
         * actionable is a value genuinely lost or written twice.
         */
        public boolean clean() {
            return this.unknown.isEmpty() && this.duplicated.isEmpty();
        }

        public int problemCount() {
            return this.unknown.size() + this.duplicated.size();
        }
    }
}
