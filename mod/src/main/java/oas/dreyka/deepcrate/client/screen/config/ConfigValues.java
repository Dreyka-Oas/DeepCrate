package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import oas.dreyka.deepcrate.config.bounds.ConfigRange;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Turning an option into the text a row draws, and telling apart what a text field holds.
 *
 * A row would carry all of this on its own, and five rows would carry five copies of it. It sits
 * here instead because none of it needs a row: it reads an option and gives back a piece of text or
 * an answer about one.
 */
public final class ConfigValues {
    private ConfigValues() {}

    private static final String ELLIPSIS = "...";

    /**
     * Whether the text in a field is a number of the kind the option holds.
     *
     * A half-typed "-" or an empty field is not, and sending it would have the server refuse a value
     * for every keystroke it took to reach a good one. The field keeps what was typed either way;
     * only the sending waits.
     */
    static boolean parses(ConfigPrimitive configPrimitive, String raw) {
        try {
            switch (configPrimitive) {
                case INT -> Integer.parseInt(raw);
                case LONG -> Long.parseLong(raw);
                case FLOAT -> Float.parseFloat(raw);
                case DOUBLE -> Double.parseDouble(raw);
                case BOOL -> {
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    /** Null for an option nothing bounds, which is a row drawing no hint at all. */
    static @Nullable Component rangeHint(ConfigOption configOption) {
        ConfigRange configRange = configOption.range();
        if (configRange == null) {
            return null;
        }

        return Component.translatable(
            "screen.deepcrate.config.range", bound(configOption.kind(), configRange.min()), bound(configOption.kind(), configRange.max())
        );
    }

    /**
     * The bounds of a whole-number option are held as doubles like every other, and "1.0 to 32767.0"
     * reads as a range that accepts halves.
     */
    private static String bound(ConfigPrimitive configPrimitive, double value) {
        boolean whole = configPrimitive == ConfigPrimitive.INT || configPrimitive == ConfigPrimitive.LONG;
        return whole ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** A label longer than the room left by the controls stops with a mark rather than running under them. */
    public static Component clip(Font font, Component component, int room) {
        if (room <= 0 || font.width(component) <= room) {
            return component;
        }

        return Component.literal(font.plainSubstrByWidth(component.getString(), Math.max(0, room - font.width(ELLIPSIS))) + ELLIPSIS);
    }
}
