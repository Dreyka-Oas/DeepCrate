package com.dreykaoas.deepcrate.mixin;

import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.inventory.CratePairContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two things the hopper cannot do on its own for a crate.
 *
 * It stops at the item's own stack size, because its merge test compares the count already in the
 * slot against that limit, so a slot holding 65 is never topped up again. And it only knows how to
 * see a double chest, by a hard-coded ChestBlockEntity test, so a hopper on one half of a double
 * crate would only ever reach that half.
 *
 * Both injections bail out immediately for anything that is not a crate, which is what keeps this
 * compatible with the optimisation mods that rewrite the same class.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void deepcrate$fillToCrateCapacity(
        Container container, Container container2, ItemStack itemStack, int i, Direction direction, CallbackInfoReturnable<ItemStack> callbackInfoReturnable
    ) {
        if (!(container2 instanceof DeepCrateBlockEntity) && !(container2 instanceof CratePairContainer)) {
            return;
        }

        ItemStack itemStack2 = container2.getItem(i);
        int capacity = container2.getMaxStackSize(itemStack);

        if (itemStack2.isEmpty()) {
            container2.setItem(i, itemStack.split(Math.min(itemStack.getCount(), capacity)));
            container2.setChanged();
        } else if (ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
            int room = Math.min(capacity - itemStack2.getCount(), itemStack.getCount());
            if (room > 0) {
                itemStack2.grow(room);
                itemStack.shrink(room);
                container2.setChanged();
            }
        }

        callbackInfoReturnable.setReturnValue(itemStack);
    }

    @Inject(method = "getBlockContainer", at = @At("HEAD"), cancellable = true)
    private static void deepcrate$seeBothHalves(
        Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<Container> callbackInfoReturnable
    ) {
        if (blockState.getBlock() instanceof DeepCrateBlock && level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            callbackInfoReturnable.setReturnValue(DeepCrateBlock.containerFor(DeepCrateBlock.cratesFor(deepCrateBlockEntity)));
        }
    }
}
