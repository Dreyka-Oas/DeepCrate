package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import oas.dreyka.deepcrate.config.access.ConfigRuntime;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * The right end of a row: the thing that changes the value, and the glyph that puts it back.
 *
 * A boolean gets a toggle and a number gets a text field, which is the whole difference between the
 * two kinds of row, so it is held here rather than in a row that would otherwise ask what it is
 * twice on every frame. The reset is a glyph rather than a picture because no sprite sheet exists
 * for this screen and adding one would be a change to the resources nobody asked for.
 */
final class ConfigControl {
    private static final int WIDGET_WIDTH = 68;
    private static final int RESET_SIZE = 20;
    /** Written as an escape rather than as itself, so the file stays plain ASCII whatever reads it. */
    private static final Component RESET_GLYPH = Component.literal("\u21BA");

    private final ConfigEdits edits;
    private final String name;
    private final ConfigPrimitive kind;
    private final AbstractWidget widget;
    private final ConfigButton reset;

    /** Raised while the server's own value is written in, whose responder would else send it straight back. */
    private boolean applying;

    ConfigControl(Font font, ConfigOption configOption, ConfigEdits configEdits) {
        this.edits = configEdits;
        this.name = configOption.name();
        this.kind = configOption.kind();
        this.widget = this.kind == ConfigPrimitive.BOOL ? this.buildToggle(configOption) : this.buildField(font, configOption);
        this.widget.setTooltip(Tooltip.create(Component.translatable(configOption.descKey())));
        this.reset = new ConfigButton(0, 0, RESET_SIZE, ConfigButton.HEIGHT, RESET_GLYPH, this::backToDefault);
        String defaultValue = ConfigRuntime.defaultOf(this.name);
        if (defaultValue == null) {
            this.reset.active = false;
        } else {
            this.reset.setTooltip(Tooltip.create(Component.translatable("screen.deepcrate.config.reset", defaultValue)));
        }
    }

    private ConfigToggle buildToggle(ConfigOption configOption) {
        return new ConfigToggle(WIDGET_WIDTH, Boolean.parseBoolean(configOption.value()), on -> this.edits.sendNow(this.name, String.valueOf(on)));
    }

    private EditBox buildField(Font font, ConfigOption configOption) {
        EditBox editBox = new EditBox(font, 0, 0, WIDGET_WIDTH, ConfigButton.HEIGHT, Component.translatable(configOption.labelKey()));
        editBox.setMaxLength(20);
        // Before the responder, so filling the box with what the server sent is not read as an edit.
        editBox.setValue(configOption.value());
        editBox.setResponder(this::onTyped);
        return editBox;
    }

    private void onTyped(String string) {
        if (this.applying || !ConfigValues.parses(this.kind, string)) {
            return;
        }

        this.edits.sendSoon(this.name, string);
    }

    private void backToDefault() {
        String defaultValue = ConfigRuntime.defaultOf(this.name);
        if (defaultValue != null) {
            this.showValue(defaultValue);
            this.edits.sendNow(this.name, defaultValue);
        }
    }

    /** The server has spoken, unless this control still holds an edit the server cannot have heard yet. */
    void accept(String value) {
        if (!this.edits.isWaiting(this.name)) {
            this.showValue(value);
        }
    }

    private void showValue(String value) {
        this.applying = true;
        if (this.widget instanceof ConfigToggle configToggle) {
            configToggle.showValue(Boolean.parseBoolean(value));
        } else if (this.widget instanceof EditBox editBox) {
            editBox.setValue(value);
        }

        this.applying = false;
    }

    void register(Consumer<AbstractWidget> sink) {
        sink.accept(this.widget);
        sink.accept(this.reset);
    }

    void show(boolean bl) {
        this.widget.visible = bl;
        this.reset.visible = bl;
    }

    /** Laid out from the right edge of the row inwards, so the controls line up whatever the label says. */
    void place(int right, int y) {
        this.reset.setPosition(right - RESET_SIZE, y);
        this.widget.setPosition(this.reset.getX() - ConfigRow.GAP - WIDGET_WIDTH, y);
    }

    int left() {
        return this.widget.getX();
    }
}
