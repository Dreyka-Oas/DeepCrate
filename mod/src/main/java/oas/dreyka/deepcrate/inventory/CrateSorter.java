package oas.dreyka.deepcrate.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Puts a crate back in order: identical stacks merge up to what the module allows, and the piles
 * follow the order the player asked for.
 *
 * The order arrives as a list of items rather than being worked out here, because the player sorts
 * by the names they read and the server has no idea what language that is. Nothing is created or
 * lost either way: the same piles come out, only their place changes.
 */
public final class CrateSorter {
    private CrateSorter() {}

    /** The halves of a pair are sorted as one run, so the letters do not start over halfway through. */
    public static void arrange(List<CrateStorage> crateStorages, List<Item> order) {
        List<Pile> piles = new ArrayList<>();
        int slotCount = 0;
        for (CrateStorage crateStorage : crateStorages) {
            slotCount += crateStorage.size();
            for (int i = 0; i < crateStorage.size(); i++) {
                gather(piles, crateStorage.get(i));
            }
        }

        // Stable, so two piles of the same item with different components keep the order they had.
        piles.sort(Comparator.comparingInt(pile -> rank(order, pile.sample.getItem())));

        List<ItemStack> arranged = new ArrayList<>(slotCount);
        for (Pile pile : piles) {
            // Both halves share one module, so either one answers for the pair. A slot found above
            // that limit sets its own: between a module being pulled out and the screen closing, a
            // slot legitimately holds more, and cutting it into capacity-sized pieces here could ask
            // for more slots than the crate has.
            int limit = Math.max(crateStorages.get(0).capacityFor(pile.sample), pile.biggest);
            for (int left = pile.total; left > 0; left -= limit) {
                arranged.add(pile.sample.copyWithCount(Math.min(left, limit)));
            }
        }

        if (arranged.size() > slotCount) {
            throw new IllegalStateException("Sorting a crate of " + slotCount + " slots asked for " + arranged.size());
        }

        int at = 0;
        for (CrateStorage crateStorage : crateStorages) {
            for (int i = 0; i < crateStorage.size(); i++) {
                crateStorage.restore(i, at < arranged.size() ? arranged.get(at++) : ItemStack.EMPTY);
            }
        }
    }

    private static void gather(List<Pile> piles, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }

        for (Pile pile : piles) {
            if (ItemStack.isSameItemSameComponents(pile.sample, itemStack)) {
                pile.total += itemStack.getCount();
                pile.biggest = Math.max(pile.biggest, itemStack.getCount());
                return;
            }
        }

        piles.add(new Pile(itemStack.copyWithCount(1), itemStack.getCount()));
    }

    /** Anything the order does not name goes after everything it does, keeping the order it had. */
    private static int rank(List<Item> order, Item item) {
        int at = order.indexOf(item);
        return at < 0 ? order.size() : at;
    }

    private static final class Pile {
        private final ItemStack sample;
        private int total;
        private int biggest;

        private Pile(ItemStack sample, int count) {
            this.sample = sample;
            this.total = count;
            this.biggest = count;
        }
    }
}
