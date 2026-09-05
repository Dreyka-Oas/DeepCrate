package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.client.render.DeepCrateRenderer;
import com.dreykaoas.deepcrate.client.screen.DeepCrateScreen;
import com.dreykaoas.deepcrate.client.sort.CrateSortOrder;
import com.dreykaoas.deepcrate.client.sort.DeepCrateClientApi;
import com.dreykaoas.deepcrate.init.RegistryInit;
import java.util.Comparator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.Item;

public final class DeepCrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerShippedSortOrders();
        MenuScreens.register(RegistryInit.MENU, DeepCrateScreen::new);
        BlockEntityRendererRegistry.register(RegistryInit.BLOCK_ENTITY, DeepCrateRenderer::new);
    }

    /**
     * The mod's own two orders go through the registry rather than around it, which is how we know
     * the registry is enough for anyone else's.
     */
    private static void registerShippedSortOrders() {
        DeepCrateClientApi.registerSortOrder(
            new CrateSortOrder(
                RegistryInit.id("name"),
                0,
                RegistryInit.id("textures/gui/sort/name.png"),
                (totals, collator) -> Comparator.comparing(DeepCrateClientApi::nameOf, collator)
            )
        );
        DeepCrateClientApi.registerSortOrder(
            new CrateSortOrder(
                RegistryInit.id("count"),
                1,
                RegistryInit.id("textures/gui/sort/count.png"),
                (totals, collator) -> Comparator.<Item, Long>comparing(totals::get)
                    .reversed()
                    .thenComparing(Comparator.comparing(DeepCrateClientApi::nameOf, collator))
            )
        );
    }
}
