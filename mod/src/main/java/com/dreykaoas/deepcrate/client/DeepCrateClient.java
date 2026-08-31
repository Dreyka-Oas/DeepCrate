package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.init.RegistryInit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class DeepCrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(RegistryInit.MENU, DeepCrateScreen::new);
        BlockEntityRendererRegistry.register(RegistryInit.BLOCK_ENTITY, DeepCrateRenderer::new);
    }
}
