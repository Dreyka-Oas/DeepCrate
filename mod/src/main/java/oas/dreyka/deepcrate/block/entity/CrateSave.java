package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModules;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.container.CrateStorage;
import oas.dreyka.deepcrate.inventory.slot.StoredEntry;
import com.mojang.serialization.Codec;
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
public final class CrateSave {
    // Was an unsigned byte while a crate had 27 slots; a double netherite crate has 234, and NbtOps
    // reads any numeric tag, so worlds written by the first version still load.
    private static final Codec<StoredEntry<Integer>> SLOT_CODEC = StoredEntry.codec("Slot", Codec.INT);
    private static final Codec<StoredEntry<Identifier>> MODULE_CODEC = StoredEntry.codec("Id", Identifier.CODEC);

    private CrateSave() {}

    public static void save(ValueOutput valueOutput, CrateStorage storage, CrateModules modules) {
        ValueOutput.TypedOutputList<StoredEntry<Integer>> typedOutputList = valueOutput.list("Slots", SLOT_CODEC);

        for (int i = 0; i < storage.size(); i++) {
            ItemStack itemStack = storage.get(i);
            if (!itemStack.isEmpty()) {
                typedOutputList.add(StoredEntry.of(i, itemStack));
            }
        }

        valueOutput.putInt("Size", storage.size());
        if (!modules.isEmpty()) {
            ValueOutput.TypedOutputList<StoredEntry<Identifier>> savedModules = valueOutput.list("Modules", MODULE_CODEC);
            for (Identifier identifier : modules.ids()) {
                savedModules.add(StoredEntry.of(identifier, modules.get(identifier)));
            }
        }
    }

    public static CrateStorage load(ValueInput valueInput, CrateModules modules) {
        modules.clear();
        for (StoredEntry<Identifier> storedModule : valueInput.listOrEmpty("Modules", MODULE_CODEC)) {
            modules.set(storedModule.key(), storedModule.toStack());
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
        List<StoredEntry<Integer>> storedSlots = new ArrayList<>();
        int highest = 0;
        for (StoredEntry<Integer> storedSlot : valueInput.listOrEmpty("Slots", SLOT_CODEC)) {
            if (storedSlot.key() >= 0) {
                storedSlots.add(storedSlot);
                highest = Math.max(highest, storedSlot.key() + 1);
            }
        }

        int size = Math.max(Math.max(CrateTier.DEFAULT_COLUMNS, highest), valueInput.getIntOr("Size", CrateTier.DEFAULT_COLUMNS));
        CrateStorage storage = new CrateStorage(size, DeepCrateApi.capacityAmong(modules));
        for (StoredEntry<Integer> storedSlot : storedSlots) {
            storage.restore(storedSlot.key(), storedSlot.toStack());
        }

        return storage;
    }

    public static void collectImplicitComponents(DataComponentMap.Builder builder) {
        // Deliberately emptied: ItemContainerContents runs through the vanilla stack codec, which
        // refuses a slot above 99. A broken crate drops its content on the ground instead.
        builder.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    public static void removeComponentsFromTag(ValueOutput valueOutput) {
        valueOutput.discard("Slots");
        valueOutput.discard("Modules");
        // The two keys of the previous format, still discarded: a crate saved by it and picked up by
        // this one must not carry its old modules along in the item.
        valueOutput.discard("Module");
        valueOutput.discard("RowModules");
        valueOutput.discard("Size");
    }
}
