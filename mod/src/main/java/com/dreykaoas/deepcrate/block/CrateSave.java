package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.module.CrateModules;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import com.dreykaoas.deepcrate.inventory.module.StoredModule;
import com.dreykaoas.deepcrate.inventory.slot.StoredSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What a crate writes to and reads from its save tag, kept apart from the block entity it saves. */
final class CrateSave {
    private CrateSave() {}

    static void save(ValueOutput valueOutput, CrateStorage storage, CrateModules modules) {
        ValueOutput.TypedOutputList<StoredSlot> typedOutputList = valueOutput.list("Slots", StoredSlot.CODEC);

        for (int i = 0; i < storage.size(); i++) {
            ItemStack itemStack = storage.get(i);
            if (!itemStack.isEmpty()) {
                typedOutputList.add(StoredSlot.of(i, itemStack));
            }
        }

        valueOutput.putInt("Size", storage.size());
        if (!modules.isEmpty()) {
            ValueOutput.TypedOutputList<StoredModule> savedModules = valueOutput.list("Modules", StoredModule.CODEC);
            for (Identifier identifier : modules.ids()) {
                savedModules.add(StoredModule.of(identifier, modules.get(identifier)));
            }
        }
    }

    static CrateStorage load(ValueInput valueInput, CrateModules modules) {
        modules.clear();
        for (StoredModule storedModule : valueInput.listOrEmpty("Modules", StoredModule.CODEC)) {
            modules.set(storedModule.id(), storedModule.toStack());
        }

        // A crate saved before the cells were a registry kept its two stacks under their own keys.
        // Read once and never written again, so a world upgrades on the first load of each crate.
        if (modules.isEmpty()) {
            modules.set(RegistryInit.CAPACITY_SLOT, valueInput.read("Module", ItemStack.CODEC).orElse(ItemStack.EMPTY));
            modules.set(RegistryInit.ROWS_SLOT, valueInput.read("RowModules", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }

        // Read the slots first: the crate has to be at least large enough to hold every one of them,
        // whatever Size says and whatever the tier says. A missing or shrunken Size must never be a
        // reason to drop stored items on the floor of the save file.
        List<StoredSlot> storedSlots = new ArrayList<>();
        int highest = 0;
        for (StoredSlot storedSlot : valueInput.listOrEmpty("Slots", StoredSlot.CODEC)) {
            if (storedSlot.slot() >= 0) {
                storedSlots.add(storedSlot);
                highest = Math.max(highest, storedSlot.slot() + 1);
            }
        }

        int size = Math.max(Math.max(CrateTier.DEFAULT_COLUMNS, highest), valueInput.getIntOr("Size", CrateTier.DEFAULT_COLUMNS));
        CrateStorage storage = new CrateStorage(size, DeepCrateApi.capacityAmong(modules));
        for (StoredSlot storedSlot : storedSlots) {
            storage.restore(storedSlot.slot(), storedSlot.toStack());
        }

        return storage;
    }

    static void collectImplicitComponents(DataComponentMap.Builder builder) {
        // Deliberately emptied: ItemContainerContents runs through the vanilla stack codec, which
        // refuses a slot above 99. A broken crate drops its content on the ground instead.
        builder.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    static void removeComponentsFromTag(ValueOutput valueOutput) {
        valueOutput.discard("Slots");
        valueOutput.discard("Modules");
        // The two keys of the previous format, still discarded: a crate saved by it and picked up by
        // this one must not carry its old modules along in the item.
        valueOutput.discard("Module");
        valueOutput.discard("RowModules");
        valueOutput.discard("Size");
    }
}
