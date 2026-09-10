package oas.dreyka.deepcrate.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.api.module.CrateModules;
import oas.dreyka.deepcrate.inventory.container.CrateStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
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
import oas.dreyka.deepcrate.block.entity.CrateSave;

/**
 * Pins the exact NBT shape a crate writes today. StoredSlot and StoredModule read and write worlds
 * already on someone's disk, so the four keys below (Slot, Id, Item, Count) must never move, whatever
 * their two Java records look like internally.
 */
class CrateSaveFormatTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    void aFilledCrateWritesTheKnownKeys() {
        CrateStorage storage = new CrateStorage(1, 512);
        storage.set(0, new ItemStack(Items.COBBLESTONE, 128));

        CrateModules modules = new CrateModules();
        Identifier moduleId = Identifier.fromNamespaceAndPath("deepcrate", "test_module");
        modules.set(moduleId, new ItemStack(Items.IRON_INGOT, 5));

        ValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        CrateSave.save(valueOutput, storage, modules);
        CompoundTag compoundTag = ((TagValueOutput) valueOutput).buildResult();

        ListTag slots = compoundTag.getListOrEmpty("Slots");
        CompoundTag slot = slots.getCompoundOrEmpty(0);
        assertEquals(0, slot.getIntOr("Slot", -1));
        assertTrue(slot.contains("Item"));
        assertEquals(128, slot.getIntOr("Count", -1));

        ListTag storedModules = compoundTag.getListOrEmpty("Modules");
        CompoundTag storedModule = storedModules.getCompoundOrEmpty(0);
        assertEquals(moduleId.toString(), storedModule.getStringOr("Id", ""));
        assertTrue(storedModule.contains("Item"));
        assertEquals(5, storedModule.getIntOr("Count", -1));
    }

    @Test
    void aFilledCrateSurvivesSaveAndReload() {
        CrateStorage storage = new CrateStorage(1, 512);
        storage.set(0, new ItemStack(Items.COBBLESTONE, 128));

        CrateModules modules = new CrateModules();
        Identifier moduleId = Identifier.fromNamespaceAndPath("deepcrate", "test_module");
        modules.set(moduleId, new ItemStack(Items.IRON_INGOT, 5));

        ValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        CrateSave.save(valueOutput, storage, modules);
        CompoundTag compoundTag = ((TagValueOutput) valueOutput).buildResult();

        ValueInput valueInput = TagValueInput.create(ProblemReporter.DISCARDING, registries, compoundTag);
        CrateModules reloadedModules = new CrateModules();
        CrateStorage reloaded = CrateSave.load(valueInput, reloadedModules);

        assertEquals(Items.COBBLESTONE, reloaded.get(0).getItem());
        assertEquals(128, reloaded.get(0).getCount());
        assertEquals(Items.IRON_INGOT, reloadedModules.get(moduleId).getItem());
        assertEquals(5, reloadedModules.get(moduleId).getCount());
    }
}
