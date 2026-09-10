package oas.dreyka.deepcrate.client.widget;

import java.util.function.BooleanSupplier;
import oas.dreyka.deepcrate.client.widget.NarratingButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * One of the page buttons stacked down the right edge of a crate, carrying its page number.
 *
 * A crate shows one page at a time, so a page holding a single stack looks exactly like an empty one
 * from the outside. The dot in the corner is what separates them without opening all twelve in turn.
 */
public class PageButton extends NarratingButton {
    public static final int SIZE = 16;
    /** Small enough that the number stays readable, and in the corner the label never reaches. */
    private static final int DOT_SIZE = 4;
    private static final int DOT_MARGIN = 1;
    private static final int DOT_CORE_INSET = 1;
    private static final int DOT_CORE = 0xFF63C452;
    /** The plate is light when hovered and dark when not; the core needs an edge to read on both. */
    private static final int DOT_EDGE = 0xFF201F1C;

    private final BooleanSupplier holdsItems;
    private final Runnable onOpen;

    public PageButton(int x, int y, Component component, BooleanSupplier holdsItems, Runnable onOpen) {
        super(x, y, SIZE, SIZE, component);
        this.holdsItems = holdsItems;
        this.onOpen = onOpen;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.onOpen.run();
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);
        this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        if (!this.holdsItems.getAsBoolean()) {
            return;
        }

        int left = this.getX() + SIZE - DOT_MARGIN - DOT_SIZE;
        int top = this.getY() + DOT_MARGIN;
        guiGraphics.fill(left, top, left + DOT_SIZE, top + DOT_SIZE, DOT_EDGE);
        guiGraphics.fill(
            left + DOT_CORE_INSET,
            top + DOT_CORE_INSET,
            left + DOT_SIZE - DOT_CORE_INSET,
            top + DOT_SIZE - DOT_CORE_INSET,
            DOT_CORE
        );
    }
}
