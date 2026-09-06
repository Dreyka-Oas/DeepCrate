package com.dreykaoas.deepcrate.config;

import com.dreykaoas.deepcrate.config.schema.ConfigSchema;
import com.dreykaoas.deepcrate.config.schema.ConfigType;
import java.lang.reflect.Field;

/** Putting one written value on the field it names, through the parse and the clamp. */
public final class ConfigAccess {
    private ConfigAccess() {}

    /**
     * False when the line names no option, says something its type cannot hold, or is refused by the
     * clamp. The field then keeps whatever valid value it already had.
     */
    public static boolean apply(String name, String raw) {
        Field option = ConfigSchema.find(name);
        if (option == null) {
            return false;
        }

        try {
            option.set(null, ConfigBounds.clamp(option.getName(), ConfigType.parse(option.getType(), raw)));
        } catch (RuntimeException | IllegalAccessException refused) {
            return false;
        }

        return true;
    }
}
