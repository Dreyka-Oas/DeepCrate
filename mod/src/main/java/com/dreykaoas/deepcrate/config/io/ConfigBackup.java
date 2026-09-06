package com.dreykaoas.deepcrate.config.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Setting a file aside instead of destroying it, and keeping the pile from growing forever.
 *
 * A settings file can be an evening of tuning, so nothing here deletes one outright: it is renamed to
 * {@code deepcrate.json.old-yyyyMMdd-HHmmss-SSS} and a fresh one written beside it. That stamp sorts
 * alphabetically in the order it happened, which is what makes {@link #prune} a plain sort with no
 * bookkeeping file to keep in step.
 */
public final class ConfigBackup {
    /** Past this the oldest go: the recent ones are the ones worth recovering. */
    public static final int KEEP = 3;

    private static final String SUFFIX = ".old-";

    private ConfigBackup() {}

    /**
     * Moves the file aside and says where it went.
     *
     * A move and never a copy then a delete: the content has to survive even if the process dies in
     * the middle, and a rename is the only way to get that for nothing.
     */
    public static Path archive(Path file, String stamp) throws IOException {
        Path aside = file.resolveSibling(file.getFileName() + SUFFIX + stamp);
        Files.move(file, aside);
        return aside;
    }

    /**
     * Deletes all but the {@code keep} newest archives, and says what went.
     *
     * Only {@code <baseName>.old-*} is matched. The live file and every other mod's are left alone:
     * the settings folder is shared, and deleting something written by someone else would reach well
     * outside this mod.
     */
    public static List<Path> prune(Path directory, String baseName, int keep) throws IOException {
        List<Path> archives = new ArrayList<>();
        try (DirectoryStream<Path> listing = Files.newDirectoryStream(directory, baseName + SUFFIX + "*")) {
            for (Path archive : listing) {
                archives.add(archive);
            }
        }

        if (archives.size() <= keep) {
            return List.of();
        }

        // Newest first. The stamp makes the name order the same as the order it happened, so this
        // needs no file attribute and cannot be fooled by a touched date.
        archives.sort(Comparator.comparing((Path archive) -> archive.getFileName().toString()).reversed());

        List<Path> deleted = new ArrayList<>();
        for (Path stale : archives.subList(keep, archives.size())) {
            if (Files.deleteIfExists(stale)) {
                deleted.add(stale);
            }
        }

        return List.copyOf(deleted);
    }
}
