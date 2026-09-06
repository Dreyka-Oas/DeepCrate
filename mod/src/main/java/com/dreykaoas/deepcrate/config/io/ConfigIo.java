package com.dreykaoas.deepcrate.config.io;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.config.io.diag.ConfigDrift;
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
