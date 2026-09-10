package oas.dreyka.deepcrate.block.placement;

import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import oas.dreyka.deepcrate.block.DeepCrateBlock;

/** The box a crate fills: a chest's, and for a pair the half that stops at the seam. */
public final class CrateShape {
    private CrateShape() {}

    private static final VoxelShape SINGLE = Block.column(14.0, 0.0, 14.0);
    private static final Map<Direction, VoxelShape> HALVES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));

    public static VoxelShape of(BlockState blockState) {
        return switch (blockState.getValue(DeepCrateBlock.TYPE)) {
            case SINGLE -> SINGLE;
            case LEFT, RIGHT -> HALVES.get(CratePairing.connectedDirection(blockState));
        };
    }

    public static BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(DeepCrateBlock.FACING, rotation.rotate(blockState.getValue(DeepCrateBlock.FACING)));
    }

    public static BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(DeepCrateBlock.FACING)));
    }
}
