package oas.dreyka.deepcrate.api.module;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * What a crate keeps in its module slots, one stack per registered kind.
 *
 * A stack whose kind nobody registered stays in the table without being drawn: a world opened without
 * the mod that added that kind gives its module back when the mod returns, rather than losing it on
 * the first save.
 */
public final class CrateModules implements Iterable<ItemStack> {
    private final Map<Identifier, ItemStack> stacks = new LinkedHashMap<>();

    public ItemStack get(Identifier identifier) {
        return this.stacks.getOrDefault(identifier, ItemStack.EMPTY);
    }

    public void set(Identifier identifier, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            this.stacks.remove(identifier);
        } else {
            this.stacks.put(identifier, itemStack);
        }
    }

    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    public void clear() {
        this.stacks.clear();
    }

    public Set<Identifier> ids() {
        return Set.copyOf(this.stacks.keySet());
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return Collections.unmodifiableCollection(this.stacks.values()).iterator();
    }
}
