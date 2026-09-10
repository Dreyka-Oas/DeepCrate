package oas.dreyka.deepcrate.config.access;

import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import org.jspecify.annotations.Nullable;
import oas.dreyka.deepcrate.config.bounds.ConfigRange;

/**
 * One option as it leaves the server: what it is called, where it is filed, what type it holds, what
 * it is worth right now, and the span it is clamped to when something clamps it.
 *
 * The value is text rather than the typed number because a boolean, an int and a double all reach the
 * same widget and go back through the same parse the file uses, so a second typed path would be a
 * second thing to keep in step. The translation keys are computed here and nowhere else: the command,
 * the packet and the screen all read them, and three hand-written prefixes would drift apart.
 */
public record ConfigOption(String name, String category, ConfigPrimitive kind, String value, @Nullable ConfigRange range) {
    public String labelKey() {
        return "deepcrate.option." + snakeCase(this.name);
    }

    public String descKey() {
        return this.labelKey() + ".desc";
    }

    public String categoryKey() {
        return "deepcrate.category." + this.category;
    }

    /** The field is camelCase and the lang files are snake_case, so baseCapacity keys as base_capacity. */
    private static String snakeCase(String name) {
        StringBuilder key = new StringBuilder(name.length() + 4);
        for (int index = 0; index < name.length(); index++) {
            char letter = name.charAt(index);
            if (index > 0 && Character.isUpperCase(letter)) {
                key.append('_');
            }

            // Character.toLowerCase and not String.toLowerCase: a key must not depend on the locale
            // the game happens to run under, where a Turkish machine lower-cases I to a dotless one
            // and every key holding it stops matching the lang file.
            key.append(Character.toLowerCase(letter));
        }

        return key.toString();
    }
}
