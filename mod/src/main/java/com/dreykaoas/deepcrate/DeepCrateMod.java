package com.dreykaoas.deepcrate;

import com.dreykaoas.deepcrate.config.DeepCrateConfig;
import com.dreykaoas.deepcrate.init.AddonInit;
import com.dreykaoas.deepcrate.init.ConfigNotice;
import com.dreykaoas.deepcrate.init.CreativeTabInit;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.net.CrateSortPayload;
import net.fabricmc.api.ModInitializer;

/**
 * Entry point. Registration is split into {@code init.*} helpers, as in the sibling mods, so this
 * class stays a composition root with no logic of its own.
 */
public final class DeepCrateMod implements ModInitializer {
    /**
     * The order of the first three lines is decided rather than tidy.
     *
     * An addon's settings holders are registered before the file is read, because the read drops a
     * name it does not know and the rewrite that follows deletes the line. The file is read before
     * {@code RegistryInit}, because touching that class runs its static fields and those read
     * {@code rowModuleStackLimit} twice, for good.
     */
    @Override
    public void onInitialize() {
        AddonInit.registerConfig();
        DeepCrateConfig.load();
        RegistryInit.register();
        AddonInit.register();
        CreativeTabInit.register();
        CrateSortPayload.register();
        ConfigNotice.register();
    }
}
