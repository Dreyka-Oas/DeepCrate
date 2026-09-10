package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * One option on one line: the name a player reads, the range it accepts and the control that changes
 * it.
 *
 * The row is built from the option the server sent and never from the field this machine reads,
 * which is what makes it right in a solo world, on a world opened to the local network and on a
 * dedicated server alike. A row the server speaks about again takes the new value, unless it is
 * still holding an edit the server cannot have heard yet.
 */
final class ConfigRow {
    static final int HEIGHT = 22;
    static final int GAP = 4;
    private static final int LABEL_COLOUR = 0xFFFFFFFF;
    private static final int HINT_COLOUR = 0xFFA0A0A0;
    /** What the game wraps a tooltip at, so a long description reads like every other one. */
    private static final int TOOLTIP_WIDTH = 170;

    private final Font font;
    private final ConfigControl control;
    private final Component label;
    private final Component description;
    private final @Nullable Component rangeHint;
    private final String name;
    private final String category;

    private int labelX;
    private int labelY;
    private int labelRoom;
    private int top;
    private boolean shown;

    ConfigRow(Font font, ConfigOption configOption, ConfigEdits configEdits) {
        this.font = font;
        this.name = configOption.name();
        this.category = configOption.category();
        this.label = Component.translatable(configOption.labelKey());
        this.description = Component.translatable(configOption.descKey());
        this.rangeHint = ConfigValues.rangeHint(configOption);
        this.control = new ConfigControl(font, configOption, configEdits);
    }

    void accept(ConfigOption configOption) {
        this.control.accept(configOption.value());
    }

    String name() {
        return this.name;
    }

    String category() {
        return this.category;
    }

    /** Matched on the name a player reads and on the one the file holds, since either may be the known one. */
    boolean matches(String query) {
        return this.label.getString().toLowerCase(Locale.ROOT).contains(query) || this.name.toLowerCase(Locale.ROOT).contains(query);
    }

    void register(Consumer<AbstractWidget> sink) {
        this.control.register(sink);
    }

    void show(boolean bl) {
        this.shown = bl;
        this.control.show(bl);
    }

    void place(int x, int y, int width) {
        this.top = y;
        this.labelX = x;
        this.labelY = y + (ConfigButton.HEIGHT - 8) / 2;
        this.control.place(x + width, y);
        this.labelRoom = this.control.left() - GAP - x;
    }

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.shown) {
            return;
        }

        int hintRoom = this.rangeHint == null ? 0 : this.font.width(this.rangeHint) + GAP;
        guiGraphics.drawString(
            this.font, ConfigValues.clip(this.font, this.label, this.labelRoom - hintRoom), this.labelX, this.labelY, LABEL_COLOUR, false
        );
        if (this.rangeHint != null) {
            guiGraphics.drawString(this.font, this.rangeHint, this.labelX + this.labelRoom - hintRoom + GAP, this.labelY, HINT_COLOUR, false);
        }

        // The description belongs to the whole row, so the name answers for it as well as the control does.
        if (mouseX >= this.labelX && mouseX < this.labelX + this.labelRoom && mouseY >= this.top && mouseY < this.top + HEIGHT) {
            guiGraphics.setTooltipForNextFrame(this.font, this.font.split(this.description, TOOLTIP_WIDTH), mouseX, mouseY);
        }
    }
}
