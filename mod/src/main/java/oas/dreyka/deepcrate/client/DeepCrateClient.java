package oas.dreyka.deepcrate.client;

import oas.dreyka.deepcrate.client.render.DeepCrateRenderer;
import oas.dreyka.deepcrate.client.screen.DeepCrateScreen;
import oas.dreyka.deepcrate.client.screen.hook.CrateTooltipCallback;
import oas.dreyka.deepcrate.client.sort.CrateSortOrder;
import oas.dreyka.deepcrate.client.sort.DeepCrateClientApi;
import oas.dreyka.deepcrate.config.domain.ScreenConfig;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.Comparator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public final class DeepCrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerShippedSortOrders();
        registerShippedTooltipLine();
        MenuScreens.register(RegistryInit.MENU, DeepCrateScreen::new);
        BlockEntityRendererRegistry.register(RegistryInit.BLOCK_ENTITY, DeepCrateRenderer::new);
    }

    /**
     * An abbreviated count hides the real one, so the item's own tooltip carries it. The line goes
     * through the event rather than around it, which is how we know the event is enough for anyone
     * else's.
     */
    private static void registerShippedTooltipLine() {
        CrateTooltipCallback.EVENT.register((menu, slot, itemStack, lines) -> {
            if (slot instanceof DeepCrateSlot && itemStack.getCount() > ScreenConfig.abbreviateAbove) {
                lines.add(1, Component.translatable("screen.deepcrate.count", itemStack.getCount()));
            }
        });
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
