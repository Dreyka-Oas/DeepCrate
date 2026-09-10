package oas.dreyka.deepcrate.block.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import oas.dreyka.deepcrate.block.DeepCrateBlock;

/** The water a crate stands in, which the block itself only ever reports rather than holds. */
public final class CrateWaterlog {
    private CrateWaterlog() {}

    /**
     * Puts the water back on the clock whenever a neighbour changes. Without it a crate placed in a
     * source block leaves a dry hole the moment the flow around it is recalculated.
     */
    public static void keepFlowing(BlockState blockState, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos) {
        if (blockState.getValue(DeepCrateBlock.WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelReader));
        }
    }
}
