package oas.dreyka.deepcrate.client.screen.config;

import java.util.function.Consumer;
import oas.dreyka.deepcrate.client.widget.NarratingButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;

/**
 * The control of a boolean option, reading on or off in the words the base game already ships.
 *
 * The button holds the value it last showed rather than reading the option again on every frame,
 * because the option a player sees comes from the server and travels one way. A press flips what is
 * drawn at once and says so; {@link #showValue} is the other door, taken when the server answers,
 * and it deliberately says nothing back.
 */
final class ConfigToggle extends NarratingButton {
    private final Consumer<Boolean> onToggle;

    private boolean on;

    ConfigToggle(int width, boolean on, Consumer<Boolean> onToggle) {
        super(0, 0, width, ConfigButton.HEIGHT, CommonComponents.optionStatus(on));
        this.on = on;
        this.onToggle = onToggle;
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.showValue(!this.on);
        this.onToggle.accept(this.on);
    }

    void showValue(boolean bl) {
        this.on = bl;
        this.setMessage(CommonComponents.optionStatus(bl));
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);
        this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }
}
