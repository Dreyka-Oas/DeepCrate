package oas.dreyka.deepcrate.client.sort;

import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * One of the buttons above the crate, one per registered order. The plate under the drawing is the
 * game's own button sprite, which is what keeps it from looking home-made and gives the pressed and
 * hovered states.
 *
 * The icon shows what the next press will do, not what the last one did: another button may have been
 * pressed since, so there is no "current order" to show.
 */
public class SortButton extends AbstractButton {
    /** Each order brings its own drawing: sixteen wide, thirty-two tall, plain over reversed. */
    private static final int SHEET_HEIGHT = 32;
    public static final int SIZE = 16;

    private final CrateSortOrder order;
    private final BiConsumer<CrateSortOrder, Boolean> onSort;

    private boolean reversed;

    public SortButton(int x, int y, CrateSortOrder crateSortOrder, boolean reversed, BiConsumer<CrateSortOrder, Boolean> onSort) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.order = crateSortOrder;
        this.reversed = reversed;
        this.onSort = onSort;
        this.tellWhatIsNext();
    }

    public CrateSortOrder order() {
        return this.order;
    }

    /** Read back when the screen is rebuilt, so a resized window does not reset the direction. */
    public boolean isReversed() {
        return this.reversed;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.onSort.accept(this.order, this.reversed);
        this.reversed = !this.reversed;
        this.tellWhatIsNext();
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            this.order.icon(),
            this.getX(),
            this.getY(),
            0.0F,
            this.reversed ? SIZE : 0.0F,
            SIZE,
            SIZE,
            SIZE,
            SHEET_HEIGHT
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    private void tellWhatIsNext() {
        Component component = this.order.label(this.reversed);
        this.setMessage(component);
        this.setTooltip(Tooltip.create(component));
    }
}
