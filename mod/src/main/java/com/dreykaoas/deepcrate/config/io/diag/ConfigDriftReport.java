package com.dreykaoas.deepcrate.config.io.diag;

import com.dreykaoas.deepcrate.DeepCrate;
import java.nio.file.Path;

/**
 * Saying in the log what is wrong with the shape of the file.
 *
 * The apply loop walks the schema and not the file, so it never looks at a key the schema does not
 * have. Without this a misspelled option is invisible: the edit does nothing, the load still reports
 * success, and the next write deletes the line.
 */
public final class ConfigDriftReport {
    private ConfigDriftReport() {}

    /** One line per problem, naming the key. What the loader repairs alone is stated, not warned about. */
    public static void emit(ConfigDrift.Report report, Path path) {
        for (ConfigDrift.Rename rename : report.renamed()) {
            DeepCrate.LOGGER.info(
                "[DeepCrate] misspelled option '{}' in {}, read as '{}': the value is kept and the file is corrected on the next write",
                rename.from(), path.getFileName(), rename.to()
            );
        }

        for (ConfigDrift.Unknown unknown : report.unknown()) {
            if (unknown.suggestion() == null) {
                DeepCrate.LOGGER.warn(
                    "[DeepCrate] unknown option '{}' in {}: it does nothing and goes away when the file is rewritten",
                    unknown.name(), path.getFileName()
                );
                continue;
            }

            // Reaching here with a suggestion means the repair was not safe: either that option is set
            // elsewhere in the file already, or another name is just as close.
            DeepCrate.LOGGER.warn(
                "[DeepCrate] unknown option '{}' in {}, did you mean '{}'? Too ambiguous to correct alone, so it does nothing",
                unknown.name(), path.getFileName(), unknown.suggestion()
            );
        }

        for (String name : report.duplicated()) {
            DeepCrate.LOGGER.warn(
                "[DeepCrate] option '{}' is written under two categories, only one copy is read and which one is not worth relying on", name
            );
        }

        for (String category : report.bogusCategory()) {
            // Corrected by the write that follows this read, so there is nothing to act on.
            DeepCrate.LOGGER.info(
                "[DeepCrate] '{}' is not a category: the options under it are still read by name and move to their own on the next write",
                category
            );
        }

        if (!report.misplaced().isEmpty()) {
            DeepCrate.LOGGER.info(
                "[DeepCrate] {} option(s) filed under the wrong category, moved on the next write", report.misplaced().size()
            );
        }
    }
}
