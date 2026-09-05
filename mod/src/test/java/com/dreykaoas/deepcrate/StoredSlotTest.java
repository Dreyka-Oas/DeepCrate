package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.inventory.slot.StoredSlot;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The point of the custom save format: a count above the 99 the vanilla stack codec allows has to
 * survive a full write and read.
 */
class StoredSlotTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    void aSlotOf128SurvivesSaveAndReload() {
        ValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ValueOutput.TypedOutputList<StoredSlot> typedOutputList = valueOutput.list("Slots", StoredSlot.CODEC);
        typedOutputList.add(StoredSlot.of(3, new ItemStack(Items.COBBLESTONE, 128)));

        CompoundTag compoundTag = ((TagValueOutput) valueOutput).buildResult();
        ValueInput valueInput = TagValueInput.create(ProblemReporter.DISCARDING, registries, compoundTag);

        StoredSlot storedSlot = valueInput.listOrEmpty("Slots", StoredSlot.CODEC).iterator().next();

        assertEquals(3, storedSlot.slot());
        assertEquals(128, storedSlot.count());
        assertEquals(Items.COBBLESTONE, storedSlot.toStack().getItem());
        assertEquals(128, storedSlot.toStack().getCount());
    }

    @Test
    void anEmptyCrateWritesAnEmptyList() {
        ValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        valueOutput.list("Slots", StoredSlot.CODEC);

        CompoundTag compoundTag = ((TagValueOutput) valueOutput).buildResult();
        ValueInput valueInput = TagValueInput.create(ProblemReporter.DISCARDING, registries, compoundTag);

        assertTrue(!valueInput.listOrEmpty("Slots", StoredSlot.CODEC).iterator().hasNext());
    }
}
