package oas.dreyka.deepcrate.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import oas.dreyka.deepcrate.block.placement.CratePairing;

/** How many players have a crate open, and the lid animation and sound that follow from it. */
final class CrateLid {
    static final int EVENT_SET_OPEN_COUNT = 1;

    final DeepCrateBlockEntity crate;
    private final ChestLidController lidController = new ChestLidController();
    private boolean silent;
    private int soundsPlayed;
    private final ContainerOpenersCounter openersCounter = new CrateLidOpeners(this);

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

    /** The scheduled tick vanilla uses to notice a player left without closing the screen. */
    static void recheckAt(Level level, BlockPos blockPos) {
        if (level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            deepCrateBlockEntity.recheckOpen();
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

    /**
     * Runs an action without the lid speaking. A row module changing rebuilds the screen through
     * {@code ServerPlayer.openMenu}, which closes the old menu before it opens the new one, so the
     * opener count falls to zero and back inside one call. The player asked for neither, and hearing
     * a crate slam shut in their face is the part they notice.
     */
    void silently(Runnable action) {
        boolean wasSilent = this.silent;
        this.silent = true;
        try {
            action.run();
        } finally {
            this.silent = wasSilent;
        }
    }

    /** How many lid sounds this crate has spoken, which is what a test can watch. */
    int soundsPlayed() {
        return this.soundsPlayed;
    }

    void playSound(Level level, BlockPos blockPos, BlockState blockState, SoundEvent soundEvent) {
        if (this.silent) {
            return;
        }

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

        this.soundsPlayed++;
        level.playSound(null, x, blockPos.getY() + 0.5, z, soundEvent, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
