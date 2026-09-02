package com.dreykaoas.deepcrate.client;

import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One of the two buttons on the tab above the crate.
 *
 * The icon shows what the next press will do, not what the last one did: the other button may have
 * been pressed since, so there is no "current order" to show.
 */
public class SortButton extends AbstractButton {
    /** Four icons, letters on the top row and counts under them, each way round. */
    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("deepcrate", "textures/gui/sort_icons.png");
    private static final int SHEET = 32;
    /** The cells of the sheet are spaced on sixteen, so a cell of fourteen leaves its neighbour alone. */
    private static final int STRIDE = 16;
    /** Smaller than a slot: these sit above the panel, where a full cell reads as a lump. */
    public static final int SIZE = 14;

    private final CrateSort crateSort;
    private final BiConsumer<CrateSort, Boolean> onSort;

    private boolean reversed;

    public SortButton(int x, int y, CrateSort crateSort, boolean reversed, BiConsumer<CrateSort, Boolean> onSort) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.crateSort = crateSort;
        this.reversed = reversed;
        this.onSort = onSort;
        this.tellWhatIsNext();
    }

    /** Read back when the screen is rebuilt, so a resized window does not reset the direction. */
    public boolean isReversed() {
        return this.reversed;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.onSort.accept(this.crateSort, this.reversed);
        this.reversed = !this.reversed;
        this.tellWhatIsNext();
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ICONS,
            this.getX(),
            this.getY(),
            this.reversed ? STRIDE : 0,
            this.crateSort == CrateSort.NAME ? 0 : STRIDE,
            SIZE,
            SIZE,
            SHEET,
            SHEET
        );

        if (this.isHovered()) {
            guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + SIZE - 1, this.getY() + SIZE - 1, 0x60FFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    private void tellWhatIsNext() {
        Component component = Component.translatable(
            "screen.deepcrate.sort." + this.crateSort.name().toLowerCase(Locale.ROOT) + (this.reversed ? "_reversed" : "")
        );
        this.setMessage(component);
        this.setTooltip(Tooltip.create(component));
    }
}
