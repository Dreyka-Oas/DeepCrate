package oas.dreyka.deepcrate.config.io.diag;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.io.ConfigBackup;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Setting an unusable file aside so its content is never lost. */
public final class ConfigQuarantine {
    private ConfigQuarantine() {}

    /**
     * True once the original is safely out of the way, which is what makes it sound to write a fresh
     * file in its place.
     *
     * Called for a file that does not parse at all, and for one that parses carrying no recognisable
     * option.
     */
    public static boolean moveAside(Path path, String cause) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        try {
            Path aside = ConfigBackup.archive(path, stamp);
            DeepCrate.LOGGER.error(
                "[DeepCrate] settings unusable ({}): moved to {} and rewritten with the defaults. Fix that file and rename it back to keep them.",
                cause, aside
            );
            try {
                for (Path pruned : ConfigBackup.prune(path.getParent(), path.getFileName().toString(), ConfigBackup.KEEP)) {
                    DeepCrate.LOGGER.info("[DeepCrate] removed old settings backup {}", pruned);
                }
            } catch (IOException pruneFailed) {
                // Housekeeping. Failing it must not undo the archive, which is what protects the file.
                DeepCrate.LOGGER.warn("[DeepCrate] could not trim old settings backups: {}", pruneFailed.toString());
            }

            return true;
        } catch (IOException moveFailed) {
            DeepCrate.LOGGER.error(
                "[DeepCrate] settings unusable ({}) and could not be moved aside ({}), running on the defaults and leaving {} alone",
                cause, moveFailed.toString(), path
            );
            return false;
        }
    }
}
