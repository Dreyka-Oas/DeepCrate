package com.dreykaoas.deepcrate.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

/** Puts the crate next to the chests and barrels of the functional blocks tab. */
public final class CreativeTabInit {
    private CreativeTabInit() {}

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(RegistryInit.ITEM));
    }
}
