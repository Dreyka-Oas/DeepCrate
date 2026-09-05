package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.api.TagMatch;
import com.dreykaoas.deepcrate.api.module.CrateModules;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateModulesTest {
    private static final Identifier CAPACITY = Identifier.fromNamespaceAndPath("deepcrate", "capacity");
    private static final Identifier ROWS = Identifier.fromNamespaceAndPath("deepcrate", "rows");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void anUntouchedTableAnswersEmptyForEveryName() {
        CrateModules crateModules = new CrateModules();

        assertTrue(crateModules.get(CAPACITY).isEmpty());
        assertTrue(crateModules.isEmpty());
        assertTrue(crateModules.ids().isEmpty());
    }

    @Test
    void aStackPutBackUnderItsNameComesBackWhole() {
        CrateModules crateModules = new CrateModules();

        crateModules.set(ROWS, new ItemStack(Items.CHEST, 7));

        assertEquals(7, crateModules.get(ROWS).getCount());
        assertFalse(crateModules.isEmpty());
        assertEquals(1, crateModules.ids().size());
    }

    @Test
    void anEmptyStackClearsItsNameRatherThanKeepingAHole() {
        CrateModules crateModules = new CrateModules();
        crateModules.set(CAPACITY, new ItemStack(Items.CHEST));

        crateModules.set(CAPACITY, ItemStack.EMPTY);

        assertTrue(crateModules.isEmpty());
        assertTrue(crateModules.ids().isEmpty());
    }

    @Test
    void walkingTheTableHandsBackEveryStackItHolds() {
        CrateModules crateModules = new CrateModules();
        crateModules.set(CAPACITY, new ItemStack(Items.CHEST));
        crateModules.set(ROWS, new ItemStack(Items.BARREL, 3));

        int total = 0;
        for (ItemStack itemStack : crateModules) {
            total += itemStack.getCount();
        }

        assertEquals(4, total);
    }

    @Test
    void tagMatchSaysNoForAnEmptyStack() {
        assertFalse(TagMatch.matches(ItemStack.EMPTY, TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("deepcrate", "module_512"))));
    }
}
