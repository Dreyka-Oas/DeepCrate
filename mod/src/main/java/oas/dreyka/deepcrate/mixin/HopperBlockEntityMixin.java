package oas.dreyka.deepcrate.mixin;

import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.container.CratePairContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The two things the hopper cannot do on its own for a crate: it stops at the item's own stack size,
 * because its merge test compares the count already in the slot against that limit, and it calls a
 * crate full as soon as every slot holds 64.
 *
 * Both injections bail out immediately for anything that is not a crate, which is what keeps them
 * compatible with the optimisation mods that rewrite the same class.
 *
 * What is deliberately NOT patched here is getBlockContainer, where vanilla joins the two halves of a
 * double chest. Lithium replaces that path and casts its result back to a BlockEntity, so returning a
 * pair container there crashes the server on the first hopper tick. A hopper against a double crate
 * therefore reaches the half it touches, as it does with a barrel.
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

    /**
     * The "is it full" test compares each slot against the item's own stack size, so a crate whose
     * slots all hold 64 is declared full and the hopper stops, module or no module.
     */
    @Inject(method = "isFullContainer", at = @At("HEAD"), cancellable = true)
    private static void deepcrate$fullMeansFullForACrate(Container container, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (!(container instanceof DeepCrateBlockEntity) && !(container instanceof CratePairContainer)) {
            return;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (itemStack.getCount() < container.getMaxStackSize(itemStack)) {
                callbackInfoReturnable.setReturnValue(false);
                return;
            }
        }

        callbackInfoReturnable.setReturnValue(true);
    }
}
