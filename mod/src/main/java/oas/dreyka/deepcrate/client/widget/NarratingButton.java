package oas.dreyka.deepcrate.client.widget;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * An {@link AbstractButton} that reads its narration the way the base game already does.
 *
 * The mod's buttons draw four different things but none of them changes what a screen reader says
 * about a press, so the narration was the same line copied four times before this base carried it.
 */
public abstract class NarratingButton extends AbstractButton {
    protected NarratingButton(int x, int y, int width, int height, Component component) {
        super(x, y, width, height, component);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
