package oas.dreyka.deepcrate.client.screen.config;

import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class ConfigValuesTest {
    @Test
    void aNegativeRoomLeavesTheLabelWhole() {
        // room <= 0 must short-circuit before the font is ever touched, or a null font here would
        // throw instead of falling back to the label unclipped.
        Component label = Component.literal("crate");
        assertSame(label, ConfigValues.clip(null, label, -1));
    }
}
