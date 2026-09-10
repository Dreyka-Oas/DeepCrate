package oas.dreyka.deepcrate.registry;

import oas.dreyka.deepcrate.api.Registrations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What {@link ModuleRegistry} and {@link RowModuleRegistry} share: a unique-by-id list, a lookup by
 * matching a stack against each entry in turn, and folding a value drawn from those stacks into one
 * number. The fold itself, maximum for a capacity or a sum for rows, is the caller's to supply.
 */
final class ModuleList<T> {
    private final List<T> modules = new ArrayList<>();
    private final Function<T, Identifier> idOf;
    private final BiPredicate<T, ItemStack> matches;
    private final String label;

    ModuleList(Function<T, Identifier> idOf, BiPredicate<T, ItemStack> matches, String label) {
        this.idOf = idOf;
        this.matches = matches;
        this.label = label;
    }

    T register(T module, @Nullable Comparator<T> ordering) {
        Registrations.addUnique(this.modules, module, this.idOf, this.label);
        if (ordering != null) {
            this.modules.sort(ordering);
        }

        return module;
    }

    List<T> all() {
        return Collections.unmodifiableList(this.modules);
    }

    @Nullable T find(ItemStack itemStack) {
        for (T module : this.modules) {
            if (this.matches.test(module, itemStack)) {
                return module;
            }
        }

        return null;
    }

    int among(Iterable<ItemStack> stacks, int identity, Function<ItemStack, Integer> valueOf, IntBinaryOperator accumulate) {
        int result = identity;
        for (ItemStack itemStack : stacks) {
            result = accumulate.applyAsInt(result, valueOf.apply(itemStack));
        }

        return result;
    }
}
