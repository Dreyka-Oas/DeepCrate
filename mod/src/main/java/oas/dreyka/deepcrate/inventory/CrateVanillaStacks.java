package oas.dreyka.deepcrate.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Cutting a count held in one crate slot into stacks the rest of the game can hold: a hand, an item
 * entity and ItemStack.CODEC all stop at {@link CrateStorage#VANILLA_LIMIT}, whatever a module lets a
 * slot carry.
 */
final class CrateVanillaStacks {
    private CrateVanillaStacks() {}

    /** Pieces of {@code count} taken from {@code itemStack}, which is itself left untouched. */
    static List<ItemStack> split(ItemStack itemStack, int count) {
        List<ItemStack> list = new ArrayList<>();
        int piece = Math.min(CrateStorage.VANILLA_LIMIT, Math.max(1, itemStack.getMaxStackSize()));
        int left = count;
        while (left > 0) {
            int take = Math.min(left, piece);
            list.add(itemStack.copyWithCount(take));
            left -= take;
        }

        return list;
    }

    static List<ItemStack> splitAll(Iterable<ItemStack> slots) {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack itemStack : slots) {
            list.addAll(split(itemStack, itemStack.getCount()));
        }

        return list;
    }
}
