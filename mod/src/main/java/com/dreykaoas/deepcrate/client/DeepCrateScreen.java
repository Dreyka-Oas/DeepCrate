package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The crate screen, built out of the chest texture of the base game.
 *
 * Nothing is shipped for the background: the header, the module strip, the slot frame, the grid and
 * the player inventory are five blits from generic_54, which is what keeps a crate looking like a
 * chest at any row count.
 */
public class DeepCrateScreen extends AbstractContainerScreen<DeepCrateMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int HEADER_HEIGHT = 17;
    private static final int STRIP_HEIGHT = 19;
    private static final int STRIP_SOURCE_Y = 126;
    private static final int SLOT_FRAME_U = 7;
    private static final int SLOT_FRAME_V = 17;
    private static final int PLAYER_PANEL_HEIGHT = 96;
    private static final int PAGE_BUTTON_SIZE = 16;

    private final int rows;

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.rows = deepCrateMenu.layout().rowsPerPage();
        this.imageHeight = HEADER_HEIGHT + STRIP_HEIGHT + this.rows * 18 + PLAYER_PANEL_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        if (this.menu.layout().pageCount() < 2) {
            return;
        }

        // Stacked down the right edge, outside the panel: the crate grid already fills the width.
        for (int page = 0; page < this.menu.layout().pageCount(); page++) {
            int target = page;
            this.addRenderableWidget(
                Button.builder(Component.literal(String.valueOf(page + 1)), button -> this.menu.setPage(target))
                    .bounds(this.leftPos + this.imageWidth + 3, this.topPos + HEADER_HEIGHT + page * (PAGE_BUTTON_SIZE + 2), PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)
                    .build()
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0.0F, 0.0F, this.imageWidth, HEADER_HEIGHT, 256, 256);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y + HEADER_HEIGHT, 0.0F, STRIP_SOURCE_Y, this.imageWidth, STRIP_HEIGHT, 256, 256
        );
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            x + DeepCrateMenu.MODULE_X - 1,
            y + DeepCrateMenu.MODULE_Y - 1,
            SLOT_FRAME_U,
            SLOT_FRAME_V,
            18,
            18,
            256,
            256
        );
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            x,
            y + DeepCrateMenu.GRID_TOP - 1,
            0.0F,
            SLOT_FRAME_V,
            this.imageWidth,
            this.rows * 18,
            256,
            256
        );
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            x,
            y + DeepCrateMenu.GRID_TOP - 1 + this.rows * 18,
            0.0F,
            STRIP_SOURCE_Y,
            this.imageWidth,
            PLAYER_PANEL_HEIGHT,
            256,
            256
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);

        if (this.menu.layout().pageCount() > 1) {
            Component component = Component.translatable(
                "screen.deepcrate.page", this.menu.page() + 1, this.menu.layout().pageCount()
            );
            guiGraphics.drawString(this.font, component, this.imageWidth - 8 - this.font.width(component), 6, 0x404040, false);
        }
    }
}
