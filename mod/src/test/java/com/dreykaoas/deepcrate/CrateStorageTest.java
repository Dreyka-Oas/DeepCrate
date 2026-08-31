package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.inventory.CrateStorage;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateStorageTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void oneSlotSwallowsTwiceAVanillaStack() {
        CrateStorage crateStorage = new CrateStorage();

        ItemStack itemStack = new ItemStack(Items.COBBLESTONE, 200);
        ItemStack leftover = crateStorage.insert(itemStack);

        assertEquals(128, crateStorage.get(0).getCount());
        assertEquals(72, crateStorage.get(1).getCount());
        assertTrue(leftover.isEmpty());
    }

    @Test
    void insertToppingUpAPartialSlotComesBackEmpty() {
        CrateStorage crateStorage = new CrateStorage();
        crateStorage.set(0, new ItemStack(Items.COBBLESTONE, 100));

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.COBBLESTONE, 28));

        assertEquals(128, crateStorage.get(0).getCount());
        assertTrue(leftover.isEmpty());
    }

    @Test
    void insertOverflowsToTheNextEmptySlotOnly() {
        CrateStorage crateStorage = new CrateStorage();
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.DIRT, 10));

        assertEquals(128, crateStorage.get(0).getCount());
        assertEquals(10, crateStorage.get(1).getCount());
        assertTrue(leftover.isEmpty());
    }

    @Test
    void aDifferentItemNeverMergesIntoAnOccupiedSlot() {
        CrateStorage crateStorage = new CrateStorage();
        crateStorage.set(0, new ItemStack(Items.DIRT, 10));

        crateStorage.insert(new ItemStack(Items.STONE, 10));

        assertEquals(Items.DIRT, crateStorage.get(0).getItem());
        assertEquals(Items.STONE, crateStorage.get(1).getItem());
    }

    @Test
    void aFullCrateHandsBackWhatDoesNotFit() {
        CrateStorage crateStorage = new CrateStorage();
        for (int i = 0; i < CrateStorage.SLOT_COUNT; i++) {
            crateStorage.set(i, new ItemStack(Items.DIRT, 128));
        }

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.DIRT, 40));

        assertEquals(40, leftover.getCount());
    }

    @Test
    void extractNeverHandsOutMoreThanAHandHolds() {
        CrateStorage crateStorage = new CrateStorage();
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));

        ItemStack itemStack = crateStorage.extract(0, 128);

        assertEquals(64, itemStack.getCount());
        assertEquals(64, crateStorage.get(0).getCount());
    }

    @Test
    void extractOfNothingIsRefusedRatherThanSilentlyClamped() {
        CrateStorage crateStorage = new CrateStorage();

        assertThrows(IllegalArgumentException.class, () -> crateStorage.extract(0, 0));
    }

    @Test
    void breakingACrateCutsEverySlotIntoDroppableStacks() {
        CrateStorage crateStorage = new CrateStorage();
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));
        crateStorage.set(1, new ItemStack(Items.STONE, 65));

        List<ItemStack> list = crateStorage.splitForVanilla();

        assertEquals(List.of(64, 64, 64, 1), list.stream().map(ItemStack::getCount).toList());
    }

    @Test
    void aSlotSetAboveTheLimitIsCutBackToIt() {
        CrateStorage crateStorage = new CrateStorage();

        crateStorage.set(0, new ItemStack(Items.DIRT, 300));

        assertEquals(128, crateStorage.get(0).getCount());
    }
}
