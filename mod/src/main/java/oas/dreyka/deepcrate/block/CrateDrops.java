package oas.dreyka.deepcrate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Puts a stack on the ground whole.
 *
 * Containers.dropItemStack cuts each stack into ten to thirty pieces. A full double echo crate losing
 * its module spills two thousand stacks, which through that helper is nearer seven thousand entities
 * in one tick.
 */
public final class CrateDrops {
    private CrateDrops() {}

    public static void dropWhole(Level level, BlockPos blockPos, double yOffset, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + yOffset, blockPos.getZ() + 0.5, itemStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
