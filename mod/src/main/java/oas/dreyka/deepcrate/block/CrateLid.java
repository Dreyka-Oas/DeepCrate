package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.inventory.CratePairContainer;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** How many players have a crate open, and the lid animation and sound that follow from it. */
final class CrateLid {
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private final DeepCrateBlockEntity crate;
    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
            CrateLid.playSound(level, blockPos, blockState, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
            CrateLid.playSound(level, blockPos, blockState, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int j) {
            level.blockEvent(blockPos, blockState.getBlock(), EVENT_SET_OPEN_COUNT, j);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (!(player.containerMenu instanceof DeepCrateMenu deepCrateMenu)) {
                return false;
            }

            Container container = deepCrateMenu.getContainer();
            return container == CrateLid.this.crate
                || container instanceof CratePairContainer cratePairContainer && cratePairContainer.contains(CrateLid.this.crate);
        }
    };

    CrateLid(DeepCrateBlockEntity crate) {
        this.crate = crate;
    }

    void startOpen(ContainerUser containerUser) {
        if (!this.crate.isRemoved() && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.incrementOpeners(
                containerUser.getLivingEntity(), this.crate.getLevel(), this.crate.getBlockPos(), this.crate.getBlockState(),
                containerUser.getContainerInteractionRange()
            );
        }
    }

    void stopOpen(ContainerUser containerUser) {
        if (!this.crate.isRemoved() && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(containerUser.getLivingEntity(), this.crate.getLevel(), this.crate.getBlockPos(), this.crate.getBlockState());
        }
    }

    List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.crate.getLevel(), this.crate.getBlockPos());
    }

    void recheckOpen() {
        if (!this.crate.isRemoved()) {
            this.openersCounter.recheckOpeners(this.crate.getLevel(), this.crate.getBlockPos(), this.crate.getBlockState());
        }
    }

    void tickLid() {
        this.lidController.tickLid();
    }

    /** Whether this handled the event; {@code false} leaves the caller to try its own super chain. */
    boolean triggerEvent(int i, int j) {
        if (i == EVENT_SET_OPEN_COUNT) {
            this.lidController.shouldBeOpen(j > 0);
            return true;
        }

        return false;
    }

    float getOpenNess(float f) {
        return this.lidController.getOpenness(f);
    }

    private static void playSound(Level level, BlockPos blockPos, BlockState blockState, SoundEvent soundEvent) {
        ChestType chestType = blockState.getValue(DeepCrateBlock.TYPE);
        // Only one half speaks, otherwise a pair opens twice as loud as a single crate, and it speaks
        // from the middle of the pair rather than from its own block.
        if (chestType == ChestType.LEFT) {
            return;
        }

        double x = blockPos.getX() + 0.5;
        double z = blockPos.getZ() + 0.5;
        if (chestType == ChestType.RIGHT) {
            Direction direction = CratePairing.connectedDirection(blockState);
            x += direction.getStepX() * 0.5;
            z += direction.getStepZ() * 0.5;
        }

        level.playSound(null, x, blockPos.getY() + 0.5, z, soundEvent, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
