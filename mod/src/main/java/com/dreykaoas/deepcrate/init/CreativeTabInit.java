package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.api.CrateModule;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.RowModule;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Puts the crates next to the chests and barrels, and the modules with the other tools. */
public final class CreativeTabInit {
    private CreativeTabInit() {}

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            // Read from the registry rather than from the six shipped tiers, so an addon's crate shows
            // up in the tab as well.
            for (CrateTier crateTier : DeepCrateApi.tiers()) {
                entries.accept(crateTier.block());
            }
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            for (CrateModule crateModule : DeepCrateApi.modules()) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(crateModule.items())) {
                    entries.accept(holder.value());
                }
            }

            for (RowModule rowModule : DeepCrateApi.rowModules()) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(rowModule.items())) {
                    entries.accept(holder.value());
                }
            }
        });
    }
}
