package com.dreykaoas.deepcrate.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dreykaoas.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ConfigBoundsTest {
    @Test
    void aCapacityIsPulledBackInsideWhatThePacketCanCarry() {
        assertEquals(1, ConfigBounds.clamp("baseCapacity", 0));
        assertEquals(32767, ConfigBounds.clamp("baseCapacity", 99999));
        assertEquals(512, ConfigBounds.clamp("baseCapacity", 512), "a value already inside comes back untouched");
    }

    @Test
    void theNameIsMatchedWhateverTheCase() {
        assertEquals(6, ConfigBounds.clamp("MAXROWSPERPAGE", 40));
    }

    @Test
    void aBooleanCrossesUntouched() {
        assertEquals(Boolean.TRUE, ConfigBounds.clamp("limitAutomationWithLithium", Boolean.TRUE));
        assertEquals(Boolean.FALSE, ConfigBounds.clamp("limitAutomationWithLithium", Boolean.FALSE));
    }

    @Test
    void shorteningNeverStartsBelowAThousand() {
        // Under 999 a count of ten is drawn as "0k", the shortening dividing by a thousand and keeping
        // no decimal below a million.
        assertEquals(999, ConfigBounds.clamp("abbreviateAbove", 10));
    }

    @Test
    void everyNumericOptionHasARange() {
        for (Field option : ConfigSchema.all()) {
            if (option.getType() == boolean.class) {
                continue;
            }

            assertNotNull(ConfigBoundsTable.get(option.getName()), option.getName() + " has no clamp range, so any number reaches the code");
        }
    }
}
