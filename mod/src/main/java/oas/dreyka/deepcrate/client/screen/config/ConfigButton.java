package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.client.widget.NarratingButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * The plain button of the settings screen, used for a category down the left and for the reset glyph
 * at the end of a row.
 *
 * Those two carry a different label and a different width and nothing else, so they share one widget
 * rather than two classes differing by a constant. The category currently read is shown by turning
 * its button inactive, which greys the label and stops it answering a second click, the way a tab
 * already open reads.
 */
final class ConfigButton extends NarratingButton {
    static final int HEIGHT = 20;

    private final Runnable onChosen;

    ConfigButton(int x, int y, int width, int height, Component component, Runnable onChosen) {
        super(x, y, width, height, component);
        this.onChosen = onChosen;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.onChosen.run();
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);
        this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }
}
