package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.inventory.CratePairContainer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class CratePairing {
    private CratePairing() {
    }

    /** Which way the other half of a pair sits. */
    public static Direction connectedDirection(BlockState blockState) {
        Direction direction = blockState.getValue(DeepCrateBlock.FACING);
        return blockState.getValue(DeepCrateBlock.TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
    }

    public static BlockPos connectedPos(BlockState blockState, BlockPos blockPos) {
        return blockPos.relative(connectedDirection(blockState));
    }

    /**
     * The crate, or both halves of a pair, the module holder first. Callers rely on that order: it is
     * what makes CratePairContainer answer the shared capacity.
     */
    public static List<DeepCrateBlockEntity> cratesFor(DeepCrateBlockEntity deepCrateBlockEntity) {
        BlockState blockState = deepCrateBlockEntity.getBlockState();
        Level level = deepCrateBlockEntity.getLevel();
        if (level == null || blockState.getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE) {
            return List.of(deepCrateBlockEntity);
        }

        BlockPos blockPos = connectedPos(blockState, deepCrateBlockEntity.getBlockPos());
        if (!(level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity other) || !other.getBlockState().is(blockState.getBlock())) {
            return List.of(deepCrateBlockEntity);
        }

        DeepCrateBlockEntity holder = deepCrateBlockEntity.moduleHolder();
        DeepCrateBlockEntity follower = holder == deepCrateBlockEntity ? other : deepCrateBlockEntity;
        return List.of(holder, follower);
    }

    /** The single crate, or both halves seen as one. */
    public static Container containerFor(List<DeepCrateBlockEntity> list) {
        return list.size() == 1 ? list.get(0) : new CratePairContainer(list.get(0), list.get(1));
    }
}
