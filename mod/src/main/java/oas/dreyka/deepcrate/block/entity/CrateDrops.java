package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.inventory.container.CrateStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Puts a stack on the ground whole.
 *
 * Containers.dropItemStack cuts each stack into ten to thirty pieces. A double netherite crate at the
 * twenty row modules the settings allow holds 594 slots, so losing its 512 module spills 4158 stacks
 * of 64, and through that helper that is forty to a hundred and twenty thousand entities in one tick.
 */
public final class CrateDrops {
    private CrateDrops() {}

    public static void dropWhole(Level level, BlockPos blockPos, double yOffset, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + yOffset, blockPos.getZ() + 0.5, itemStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    public static void preRemoveSideEffects(DeepCrateBlockEntity crate, BlockPos blockPos) {
        if (crate.getLevel() == null) {
            return;
        }

        CrateStorage storage = crate.storage();
        List<ItemStack> list = storage.splitForVanilla();
        storage.clear();
        for (ItemStack itemStack : crate.modules()) {
            list.add(itemStack);
        }

        crate.modules().clear();

        for (ItemStack itemStack : list) {
            dropWhole(crate.getLevel(), blockPos, 0.5, itemStack);
        }
    }
}
