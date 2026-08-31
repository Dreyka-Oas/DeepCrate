package com.dreykaoas.deepcrate;

import com.dreykaoas.deepcrate.init.CreativeTabInit;
import com.dreykaoas.deepcrate.init.RegistryInit;
import net.fabricmc.api.ModInitializer;

/**
 * Entry point. Registration is split into {@code init.*} helpers, as in the sibling mods, so this
 * class stays a composition root with no logic of its own.
 */
public final class DeepCrateMod implements ModInitializer {
    @Override
    public void onInitialize() {
        RegistryInit.register();
        CreativeTabInit.register();
    }
}
