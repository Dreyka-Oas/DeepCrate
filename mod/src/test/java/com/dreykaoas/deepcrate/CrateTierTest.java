package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dreykaoas.deepcrate.api.CrateTier;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateTierTest {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("deepcrate", "test_crate");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void theShortFormStaysNineWide() {
        CrateTier crateTier = new CrateTier(ID, 3, Blocks.CHEST);

        assertEquals(CrateTier.DEFAULT_COLUMNS, crateTier.columns());
        assertEquals(27, crateTier.slotCount());
    }

    @Test
    void aWiderTierCountsItsOwnColumns() {
        CrateTier crateTier = new CrateTier(ID, 3, 12, Blocks.CHEST);

        assertEquals(36, crateTier.slotCount());
    }

    @Test
    void aTierWithNoColumnIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CrateTier(ID, 3, 0, Blocks.CHEST));
    }
}
