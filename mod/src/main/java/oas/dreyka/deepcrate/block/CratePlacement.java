package oas.dreyka.deepcrate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Which neighbour a crate marries: the decision taken as it is put down, and the one taken again
 * whenever the block beside it changes. The rules are the chest's, reimplemented against the same two
 * properties rather than inherited, because AbstractChestBlock hard-codes ChestBlockEntity.
 */
final class CratePlacement {
    private CratePlacement() {}

    static BlockState forPlacement(DeepCrateBlock deepCrateBlock, BlockPlaceContext blockPlaceContext) {
        Direction facing = blockPlaceContext.getHorizontalDirection().getOpposite();
        Direction clicked = blockPlaceContext.getClickedFace();
        LevelAccessor levelAccessor = blockPlaceContext.getLevel();
        BlockPos blockPos = blockPlaceContext.getClickedPos();
        ChestType chestType = ChestType.SINGLE;

        // Same split as the chest: crouching against a crate's side pairs with THAT crate and nothing
        // else. Chaining the two cases with else-if let a crouched placement fall through and pair
        // with a neighbour the player never pointed at.
        if (clicked.getAxis().isHorizontal() && blockPlaceContext.isSecondaryUseActive()) {
            Direction partner = partnerFacing(deepCrateBlock, levelAccessor, blockPos, clicked.getOpposite());
            if (partner != null && partner.getAxis() != clicked.getAxis()) {
                facing = partner;
                chestType = partner.getCounterClockWise() == clicked.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        } else {
            if (facing == partnerFacing(deepCrateBlock, levelAccessor, blockPos, facing.getClockWise())) {
                chestType = ChestType.LEFT;
            } else if (facing == partnerFacing(deepCrateBlock, levelAccessor, blockPos, facing.getCounterClockWise())) {
                chestType = ChestType.RIGHT;
            }
        }

        return deepCrateBlock.defaultBlockState()
            .setValue(DeepCrateBlock.FACING, facing)
            .setValue(DeepCrateBlock.TYPE, chestType)
            .setValue(DeepCrateBlock.WATERLOGGED, levelAccessor.getFluidState(blockPos).getType() == Fluids.WATER);
    }

    /**
     * The state a crate takes when the block beside it changes, or {@code null} when this neighbour is
     * none of its business and the inherited answer stands. It marries a crate to the one just placed
     * beside it, and it sets the survivor back to single when its partner is broken.
     */
    static @Nullable BlockState repaired(DeepCrateBlock deepCrateBlock, BlockState blockState, BlockState neighbour, Direction direction) {
        if (neighbour.is(deepCrateBlock) && direction.getAxis().isHorizontal()) {
            ChestType chestType = neighbour.getValue(DeepCrateBlock.TYPE);
            if (blockState.getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE
                && chestType != ChestType.SINGLE
                && blockState.getValue(DeepCrateBlock.FACING) == neighbour.getValue(DeepCrateBlock.FACING)
                && CratePairing.connectedDirection(neighbour) == direction.getOpposite()) {
                return blockState.setValue(DeepCrateBlock.TYPE, chestType.getOpposite());
            }
        } else if (blockState.getValue(DeepCrateBlock.TYPE) != ChestType.SINGLE
            && CratePairing.connectedDirection(blockState) == direction) {
            return blockState.setValue(DeepCrateBlock.TYPE, ChestType.SINGLE);
        }

        return null;
    }

    /** The facing of a neighbouring crate of the same tier that is still on its own. */
    private static @Nullable Direction partnerFacing(
        DeepCrateBlock deepCrateBlock, LevelAccessor levelAccessor, BlockPos blockPos, Direction direction
    ) {
        BlockState blockState = levelAccessor.getBlockState(blockPos.relative(direction));
        return blockState.is(deepCrateBlock) && blockState.getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE
            ? blockState.getValue(DeepCrateBlock.FACING)
            : null;
    }
}
