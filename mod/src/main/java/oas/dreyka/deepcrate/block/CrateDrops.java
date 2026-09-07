package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.inventory.CrateStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Puts a stack on the ground whole.
 *
 * Containers.dropItemStack cuts each stack into ten to thirty pieces. A full double netherite crate
 * losing its module spills 594 stacks, which through that helper is ten to eighteen thousand
 * entities in one tick.
 */
public final class CrateDrops {
    private CrateDrops() {}

    public static void dropWhole(Level level, BlockPos blockPos, double yOffset, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + yOffset, blockPos.getZ() + 0.5, itemStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    static void preRemoveSideEffects(DeepCrateBlockEntity crate, BlockPos blockPos) {
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
