package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.init.RegistryInit;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/**
 * The module cells of a crate, and, for a pair, which of its two block entities actually holds them.
 *
 * A crate that already holds a module keeps it, whichever half of a pair it ended up on. Deciding
 * rather than moving anything means both halves always name the same holder, and a crate that marries
 * another later does not have to be rewritten.
 */
final class CrateModuleHolder {
    private final DeepCrateBlockEntity crate;
    private @Nullable DeepCrateBlockEntity cachedHolder;
    private @Nullable BlockState cachedFor;

    CrateModuleHolder(DeepCrateBlockEntity crate) {
        this.crate = crate;
    }

    ItemStack module() {
        return this.crate.modules().get(RegistryInit.CAPACITY_SLOT);
    }

    void setModule(ItemStack itemStack) {
        this.setModuleIn(RegistryInit.CAPACITY_SLOT, itemStack);
    }

    ItemStack rowModules() {
        return this.crate.modules().get(RegistryInit.ROWS_SLOT);
    }

    void setRowModules(ItemStack itemStack) {
        this.setModuleIn(RegistryInit.ROWS_SLOT, itemStack);
    }

    /**
     * Puts a stack in one cell and lets the whole table decide again. Capacity and rows are read
     * across every cell rather than from the one that changed: which cell an item sits in no longer
     * says what it does.
     *
     * Both halves of a pair follow, because a double crate is two containers shown as one screen and
     * one module has to give one row to each of them.
     */
    void setModuleIn(Identifier identifier, ItemStack itemStack) {
        this.crate.modules().set(identifier, itemStack);
        // Which half holds for the pair turns on which half carries a module, so a stack landing in a
        // cell can move the answer with the block state standing still.
        this.forget();
        this.crate.storage().setCapacity(DeepCrateApi.capacityAmong(this.crate.modules()));
        for (DeepCrateBlockEntity deepCrateBlockEntity : DeepCrateBlock.cratesFor(this.crate)) {
            deepCrateBlockEntity.crateModuleHolder.forget();
            deepCrateBlockEntity.storage();
            deepCrateBlockEntity.setChanged();
        }

        this.crate.setChanged();
    }

    /** Rows this crate has beyond its tier's own, read from wherever the pair keeps its modules. */
    int extraRows() {
        return DeepCrateApi.rowsAmong(this.holder().modules());
    }

    /**
     * Cuts the crate back to the size its modules now call for and hands back what was above it.
     * Called when the screen closes, the same rule the capacity module follows: pulling modules out
     * spills rather than silently swallowing.
     */
    List<ItemStack> trimToRows() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.crate.getBlockState().getBlock());
        if (crateTier == null) {
            return List.of();
        }

        return this.crate.storage().trimTo(crateTier.slotCount() + this.extraRows() * crateTier.columns());
    }

    /**
     * Where the module of a pair lives, kept between calls: a hopper asking whether a crate is full
     * comes through here four times a slot, and that bought a block entity lookup each time on half of
     * a pair. The kept answer goes when the state behind it changes, TYPE carrying the pairing, and
     * when the half it names reports itself removed, all a crate dropped with its chunk leaves behind.
     */
    DeepCrateBlockEntity holder() {
        DeepCrateBlockEntity remembered = this.cachedHolder;
        if (remembered != null && this.cachedFor == this.crate.getBlockState() && !remembered.isRemoved()) {
            return remembered;
        }

        DeepCrateBlockEntity holder = this.resolveHolder();
        if (this.crate.getLevel() != null) {
            this.cachedHolder = holder;
            this.cachedFor = this.crate.getBlockState();
        }

        return holder;
    }

    void forget() {
        this.cachedHolder = null;
        this.cachedFor = null;
    }

    private DeepCrateBlockEntity resolveHolder() {
        if (this.crate.getLevel() == null || this.crate.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE) {
            return this.crate;
        }

        BlockPos blockPos = DeepCrateBlock.connectedPos(this.crate.getBlockState(), this.crate.getBlockPos());
        if (!(this.crate.getLevel().getBlockEntity(blockPos) instanceof DeepCrateBlockEntity other)) {
            return this.crate;
        }

        if (this.hasAnyModule(this.crate)) {
            return !this.hasAnyModule(other) || this.crate.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.RIGHT ? this.crate : other;
        }

        if (this.hasAnyModule(other)) {
            return other;
        }

        return this.crate.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.RIGHT ? this.crate : other;
    }

    private boolean hasAnyModule(DeepCrateBlockEntity crate) {
        return !crate.modules().isEmpty();
    }

    /**
     * A crate paired with another follows its partner's module. Without this the half that does not
     * hold the module keeps the 64 it was loaded with, and every insertion into it is clamped.
     */
    void alignCapacityWithHolder() {
        DeepCrateBlockEntity holder = this.holder();
        if (holder != this.crate) {
            this.crate.unalignedStorage().setCapacity(DeepCrateApi.capacityAmong(holder.modules()));
        }
    }
}
