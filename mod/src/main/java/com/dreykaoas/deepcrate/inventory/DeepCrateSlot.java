package com.dreykaoas.deepcrate.inventory;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A crate slot: it accepts {@link CrateStorage#SLOT_LIMIT}, but never hands out more than a player
 * can hold.
 */
public class DeepCrateSlot extends Slot {
    public DeepCrateSlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        // Slot's own version takes the smaller of the container limit and the item limit, which would
        // pull every insertion back down to 64.
        return this.getMaxStackSize();
    }

    @Override
    public Optional<ItemStack> tryRemove(int i, int j, Player player) {
        return super.tryRemove(Math.min(i, CrateStorage.VANILLA_LIMIT), Math.min(j, CrateStorage.VANILLA_LIMIT), player);
    }
}
