package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.api.CrateTier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Puts the crates next to the chests and barrels, and the modules with the other tools. */
public final class CreativeTabInit {
    private CreativeTabInit() {}

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            for (CrateTier crateTier : RegistryInit.TIERS) {
                entries.accept(crateTier.block());
            }
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            for (Item item : RegistryInit.MODULE_ITEMS) {
                entries.accept(item);
            }
        });
    }
}
