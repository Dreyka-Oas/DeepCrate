package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CratePairContainer;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Every crate tier is an instance of this class. Which tier a given block is stays out of the class:
 * it is read back from the registry, so the one-argument constructor {@code simpleCodec} needs stays
 * available and no field can drift from the registration.
 *
 * The pairing rules are the chest's, reimplemented against the same two properties rather than
 * inherited: AbstractChestBlock hard-codes ChestBlockEntity in its combine method.
 */
public class DeepCrateBlock extends BaseEntityBlock {
    public static final MapCodec<DeepCrateBlock> CODEC = simpleCodec(DeepCrateBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);
    private static final Map<Direction, VoxelShape> HALF_SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));

    public DeepCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false)
        );
    }

    @Override
    public MapCodec<DeepCrateBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DeepCrateBlockEntity(blockPos, blockState);
    }

    /** Which way the other half of a pair sits. */
    public static Direction connectedDirection(BlockState blockState) {
        Direction direction = blockState.getValue(FACING);
        return blockState.getValue(TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
    }

    public static BlockPos connectedPos(BlockState blockState, BlockPos blockPos) {
        return blockPos.relative(connectedDirection(blockState));
    }

    /**
     * The crate, or both halves of a pair, the module holder first. Callers rely on that order: it is
     * what makes CompoundContainer answer the shared capacity.
     */
    public static List<DeepCrateBlockEntity> cratesFor(DeepCrateBlockEntity deepCrateBlockEntity) {
        BlockState blockState = deepCrateBlockEntity.getBlockState();
        Level level = deepCrateBlockEntity.getLevel();
        if (level == null || blockState.getValue(TYPE) == ChestType.SINGLE) {
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

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        // Client only: the lid angle is pure animation, the server already knows the crate is open.
        return level.isClientSide() ? createTickerHelper(blockEntityType, RegistryInit.BLOCK_ENTITY, DeepCrateBlockEntity::lidAnimateTick) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return switch (blockState.getValue(TYPE)) {
            case SINGLE -> SHAPE;
            case LEFT, RIGHT -> HALF_SHAPES.get(connectedDirection(blockState));
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            player.openMenu(deepCrateBlockEntity);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction facing = blockPlaceContext.getHorizontalDirection().getOpposite();
        Direction clicked = blockPlaceContext.getClickedFace();
        ChestType chestType = ChestType.SINGLE;

        // Same split as the chest: crouching against a crate's side pairs with THAT crate and nothing
        // else. Chaining the two cases with else-if let a crouched placement fall through and pair
        // with a neighbour the player never pointed at.
        if (clicked.getAxis().isHorizontal() && blockPlaceContext.isSecondaryUseActive()) {
            Direction partner = this.partnerFacing(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), clicked.getOpposite());
            if (partner != null && partner.getAxis() != clicked.getAxis()) {
                facing = partner;
                chestType = partner.getCounterClockWise() == clicked.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        } else {
            if (facing == this.partnerFacing(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), facing.getClockWise())) {
                chestType = ChestType.LEFT;
            } else if (facing == this.partnerFacing(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), facing.getCounterClockWise())) {
                chestType = ChestType.RIGHT;
            }
        }

        FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(TYPE, chestType)
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    /**
     * The only hook that keeps a pair consistent: it marries a crate to the one just placed beside it,
     * and it sets the survivor back to single when its partner is broken.
     */
    @Override
    protected BlockState updateShape(
        BlockState blockState,
        LevelReader levelReader,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos blockPos,
        Direction direction,
        BlockPos blockPos2,
        BlockState blockState2,
        RandomSource randomSource
    ) {
        if (blockState.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelReader));
        }

        if (blockState2.is(this) && direction.getAxis().isHorizontal()) {
            ChestType chestType = blockState2.getValue(TYPE);
            if (blockState.getValue(TYPE) == ChestType.SINGLE
                && chestType != ChestType.SINGLE
                && blockState.getValue(FACING) == blockState2.getValue(FACING)
                && connectedDirection(blockState2) == direction.getOpposite()) {
                return blockState.setValue(TYPE, chestType.getOpposite());
            }
        } else if (blockState.getValue(TYPE) != ChestType.SINGLE && connectedDirection(blockState) == direction) {
            return blockState.setValue(TYPE, ChestType.SINGLE);
        }

        return super.updateShape(blockState, levelReader, scheduledTickAccess, blockPos, direction, blockPos2, blockState2, randomSource);
    }

    @Override
    protected FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    /** A crate blocks a path, as a chest does; without this mobs walk straight through it. */
    @Override
    protected boolean isPathfindable(BlockState blockState, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, boolean bl) {
        Containers.updateNeighboursAfterDestroy(blockState, serverLevel, blockPos);
    }

    @Override
    protected void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        if (serverLevel.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            deepCrateBlockEntity.recheckOpen();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos, Direction direction) {
        // Both halves, as a double chest does: a comparator on one half of a pair reads the pair.
        if (level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            // Bounded on purpose: a slot left above the capacity, right after a module is pulled out,
            // makes the vanilla ratio climb past one and the signal past fifteen.
            return Math.min(15, AbstractContainerMenu.getRedstoneSignalFromContainer(containerFor(cratesFor(deepCrateBlockEntity))));
        }

        return 0;
    }

    @Override
    protected BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, WATERLOGGED);
    }

    /** The facing of a neighbouring crate of the same tier that is still on its own. */
    private @Nullable Direction partnerFacing(LevelAccessor levelAccessor, BlockPos blockPos, Direction direction) {
        BlockState blockState = levelAccessor.getBlockState(blockPos.relative(direction));
        return blockState.is(this) && blockState.getValue(TYPE) == ChestType.SINGLE ? blockState.getValue(FACING) : null;
    }
}
