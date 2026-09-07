package oas.dreyka.deepcrate.init;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.api.module.RowModule;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The mod's own tab in the creative inventory: every tier in chain order, then every module. */
public final class CreativeTabInit {
    private static final ResourceKey<CreativeModeTab> CRATES = ResourceKey.create(Registries.CREATIVE_MODE_TAB, RegistryInit.id("crates"));

    private CreativeTabInit() {}

    public static void register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            CRATES,
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.deepcrate.crates"))
                .icon(() -> new ItemStack(RegistryInit.TIERS.getLast().block()))
                .displayItems((parameters, output) -> {
                    // Read from the registry rather than from the shipped tiers, so an addon's crate
                    // and modules show up in the tab as well.
                    for (CrateTier crateTier : DeepCrateApi.tiers()) {
                        output.accept(crateTier.block());
                    }

                    for (CrateModule crateModule : DeepCrateApi.modules()) {
                        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(crateModule.items())) {
                            output.accept(holder.value());
                        }
                    }

                    for (RowModule rowModule : DeepCrateApi.rowModules()) {
                        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(rowModule.items())) {
                            output.accept(holder.value());
                        }
                    }
                })
                .build()
        );
    }
}
