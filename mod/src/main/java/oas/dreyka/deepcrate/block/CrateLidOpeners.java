package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.inventory.container.CratePairContainer;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

/** The opener bookkeeping a {@link CrateLid} hands off, kept out of that class for its own file. */
final class CrateLidOpeners extends ContainerOpenersCounter {
    private final CrateLid lid;

    CrateLidOpeners(CrateLid lid) {
        this.lid = lid;
    }

    @Override
    protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
        this.lid.playSound(level, blockPos, blockState, SoundEvents.CHEST_OPEN);
    }

    @Override
    protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
        this.lid.playSound(level, blockPos, blockState, SoundEvents.CHEST_CLOSE);
    }

    @Override
    protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int j) {
        level.blockEvent(blockPos, blockState.getBlock(), CrateLid.EVENT_SET_OPEN_COUNT, j);
    }

    @Override
    public boolean isOwnContainer(Player player) {
        if (!(player.containerMenu instanceof DeepCrateMenu deepCrateMenu)) {
            return false;
        }

        Container container = deepCrateMenu.wiring().crate();
        return container == this.lid.crate || container instanceof CratePairContainer cratePairContainer && cratePairContainer.contains(this.lid.crate);
    }
}
