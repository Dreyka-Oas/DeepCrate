package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The chest screen of the base game, reused as is: three rows and a player inventory below. */
public class DeepCrateScreen extends AbstractContainerScreen<DeepCrateMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROWS = 3;

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, k, l, 0.0F, 0.0F, this.imageWidth, ROWS * 18 + 17, 256, 256);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, BACKGROUND, k, l + ROWS * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256
        );
    }
}
