package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.api.module.CrateCapacityCallback;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateStorageTest {
    /** The rule the tests swap in and out. A Fabric event never lets go of a listener once given one. */
    private static final AtomicReference<CrateCapacityCallback> RULE = new AtomicReference<>();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CrateCapacityCallback.EVENT.register((tier, itemStack, proposed) -> {
            CrateCapacityCallback rule = RULE.get();
            return rule == null ? proposed : rule.capacity(tier, itemStack, proposed);
        });
    }

    @AfterEach
    void forgetTheRule() {
        RULE.set(null);
    }

    @Test
    void oneSlotSwallowsWhatTheModuleAllows() {
        CrateStorage crateStorage = new CrateStorage(27, 128);

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.COBBLESTONE, 200));

        assertEquals(128, crateStorage.get(0).getCount());
        assertEquals(72, crateStorage.get(1).getCount());
        assertTrue(leftover.isEmpty());
    }

    @Test
    void withNoModuleACrateBehavesLikeAChest() {
        CrateStorage crateStorage = new CrateStorage(27, 64);

        crateStorage.insert(new ItemStack(Items.COBBLESTONE, 100));

        assertEquals(64, crateStorage.get(0).getCount());
        assertEquals(36, crateStorage.get(1).getCount());
    }

    @Test
    void insertToppingUpAPartialSlotComesBackEmpty() {
        CrateStorage crateStorage = new CrateStorage(27, 128);
        crateStorage.set(0, new ItemStack(Items.COBBLESTONE, 100));

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.COBBLESTONE, 28));

        assertEquals(128, crateStorage.get(0).getCount());
        assertTrue(leftover.isEmpty());
    }

    @Test
    void aDifferentItemNeverMergesIntoAnOccupiedSlot() {
        CrateStorage crateStorage = new CrateStorage(27, 128);
        crateStorage.set(0, new ItemStack(Items.DIRT, 10));

        crateStorage.insert(new ItemStack(Items.STONE, 10));

        assertEquals(Items.DIRT, crateStorage.get(0).getItem());
        assertEquals(Items.STONE, crateStorage.get(1).getItem());
    }

    @Test
    void aFullCrateHandsBackWhatDoesNotFit() {
        CrateStorage crateStorage = new CrateStorage(2, 128);
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));
        crateStorage.set(1, new ItemStack(Items.DIRT, 128));

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.DIRT, 40));

        assertEquals(40, leftover.getCount());
    }

    @Test
    void extractNeverHandsOutMoreThanAHandHolds() {
        CrateStorage crateStorage = new CrateStorage(27, 1024);
        crateStorage.set(0, new ItemStack(Items.DIRT, 1024));

        ItemStack itemStack = crateStorage.extract(0, 1024);

        assertEquals(64, itemStack.getCount());
        assertEquals(960, crateStorage.get(0).getCount());
    }

    @Test
    void extractOfNothingIsRefusedRatherThanSilentlyClamped() {
        CrateStorage crateStorage = new CrateStorage(27, 64);

        assertThrows(IllegalArgumentException.class, () -> crateStorage.extract(0, 0));
    }

    @Test
    void pullingTheModuleOutSpillsExactlyWhatNoLongerFits() {
        CrateStorage crateStorage = new CrateStorage(27, 1024);
        crateStorage.set(0, new ItemStack(Items.DIRT, 1000));
        crateStorage.set(1, new ItemStack(Items.STONE, 50));

        crateStorage.setCapacity(64);
        List<ItemStack> spilled = crateStorage.overflow();

        assertEquals(64, crateStorage.get(0).getCount());
        assertEquals(50, crateStorage.get(1).getCount());
        assertEquals(936, spilled.stream().mapToInt(ItemStack::getCount).sum());
        assertTrue(spilled.stream().allMatch(itemStack -> itemStack.getCount() <= 64));
    }

    @Test
    void nothingSpillsWhileTheCapacityHolds() {
        CrateStorage crateStorage = new CrateStorage(27, 128);
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));

        assertTrue(crateStorage.overflow().isEmpty());
        assertEquals(128, crateStorage.get(0).getCount());
    }

    @Test
    void breakingACrateCutsEverySlotIntoDroppableStacks() {
        CrateStorage crateStorage = new CrateStorage(27, 1024);
        crateStorage.set(0, new ItemStack(Items.DIRT, 128));
        crateStorage.set(1, new ItemStack(Items.STONE, 65));

        List<ItemStack> list = crateStorage.splitForVanilla();

        assertEquals(List.of(64, 64, 64, 1), list.stream().map(ItemStack::getCount).toList());
    }

    @Test
    void aTierGainingRowsKeepsWhatWasStored() {
        CrateStorage crateStorage = new CrateStorage(27, 64);
        crateStorage.set(26, new ItemStack(Items.DIRT, 5));

        crateStorage.grow(72);

        assertEquals(72, crateStorage.size());
        assertEquals(5, crateStorage.get(26).getCount());
        assertTrue(crateStorage.get(71).isEmpty());
    }

    @Test
    void aTierLosingRowsIsRefusedRatherThanSwallowingSlots() {
        CrateStorage crateStorage = new CrateStorage(72, 64);

        assertThrows(IllegalArgumentException.class, () -> crateStorage.grow(27));
    }

    @Test
    void aSlotSetAboveTheCapacityIsCutBackToIt() {
        CrateStorage crateStorage = new CrateStorage(27, 128);

        crateStorage.set(0, new ItemStack(Items.DIRT, 300));

        assertEquals(128, crateStorage.get(0).getCount());
    }

    @Test
    void anEventCanRaiseWhatOneSlotHolds() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        RULE.set((tier, itemStack, proposed) -> itemStack.is(Items.DIAMOND) ? proposed * 4 : proposed);

        assertEquals(256, crateStorage.capacityFor(new ItemStack(Items.DIAMOND)));
        assertEquals(64, crateStorage.capacityFor(new ItemStack(Items.COBBLESTONE)));
    }

    @Test
    void aRefusedItemIsNeverStoredAndNeverDestroyed() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        RULE.set((tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed);

        ItemStack leftover = crateStorage.insert(new ItemStack(Items.GUNPOWDER, 30));

        assertEquals(30, leftover.getCount());
        assertTrue(crateStorage.isEmpty());
        assertFalse(crateStorage.accepts(new ItemStack(Items.GUNPOWDER)));
        // Never zero: a slot told it holds nothing would write a stack of nothing, which is how an
        // item gets destroyed rather than refused.
        assertEquals(1, crateStorage.capacityFor(new ItemStack(Items.GUNPOWDER)));
    }

    @Test
    void automationIsTurnedAwayFromARefusedItemToo() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        RULE.set((tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed);

        assertEquals(0, crateStorage.automationCapacityFor(new ItemStack(Items.GUNPOWDER)));
    }

    @Test
    void whatIsAlreadyStoredSurvivesTheCrateRefusingIt() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        crateStorage.set(0, new ItemStack(Items.GUNPOWDER, 30));
        RULE.set((tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed);

        assertTrue(crateStorage.overflow().isEmpty());
        assertEquals(30, crateStorage.get(0).getCount());
    }
}
