package oas.dreyka.deepcrate.config.io;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.io.diag.ConfigDrift;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * The one class that knows where the file lives.
 *
 * Everything under it takes the path as a parameter, which is what keeps the read and the write
 * exercisable against a temporary directory with no game running.
 */
public final class ConfigIo {
    /**
     * Written at boot on the thread that starts the mod, read from the server thread when an operator
     * joins.
     */
    private static volatile ConfigDrift.@Nullable Report lastReport;

    private ConfigIo() {}

    public static void load() {
        lastReport = ConfigLoader.load(file());
    }

    /**
     * The live options back onto disk, for an edit made in game rather than in a text editor.
     *
     * The writer is synchronised and writes through a temporary file, so a save landing while another
     * one is in flight waits its turn instead of producing a half file. A path that cannot even be
     * resolved is the caller's business no more than a failed write is: the value is already applied
     * in memory, and throwing here would take down the command or the packet handler that asked.
     */
    public static void save() {
        try {
            ConfigWriter.save(file());
        } catch (RuntimeException noPath) {
            DeepCrate.LOGGER.warn("[DeepCrate] settings could not be saved: {}", noPath.toString());
        }
    }

    /**
     * What the last read made of the file, or nothing when it has not happened yet.
     *
     * Read by the operator notice, because a log line on its own is close to worthless: a solo player
     * never opens {@code latest.log}.
     */
    public static ConfigDrift.@Nullable Report lastReport() {
        return lastReport;
    }

    /** The {@code oas} folder is the author's, so every mod of his lands together. */
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("oas").resolve(DeepCrate.MOD_ID + ".json");
    }
}
