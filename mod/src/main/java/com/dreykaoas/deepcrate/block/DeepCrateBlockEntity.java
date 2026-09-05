package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.api.CrateLayout;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.module.CrateModules;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateOpenData;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DeepCrateBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity, ExtendedScreenHandlerFactory<CrateOpenData> {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.crate");

    private CrateStorage storage = new CrateStorage(CrateTier.DEFAULT_COLUMNS, DeepCrateApi.BASE_CAPACITY);
    private final CrateModules modules = new CrateModules();
    private final CrateLid crateLid = new CrateLid(this);
    private final CrateModuleHolder crateModuleHolder = new CrateModuleHolder(this);

    public DeepCrateBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(RegistryInit.BLOCK_ENTITY, blockPos, blockState);
    }

    /**
     * The tier this crate belongs to, read from its block rather than held in a field, so the block
     * keeps the one-argument constructor {@code simpleCodec} needs.
     */
    public CrateTier tier() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier == null) {
            throw new IllegalStateException("No crate tier registered for " + this.getBlockState().getBlock());
        }

        return crateTier;
    }

    public CrateStorage storage() {
        this.alignStorageWithTier();
        this.crateModuleHolder.alignCapacityWithHolder();
        return this.storage;
    }

    public CrateModules modules() {
        return this.modules;
    }

    /**
     * The field itself, skipping the tier and holder alignment {@link #storage()} runs: the alignment
     * methods themselves read this instead, or they would recurse through it.
     */
    CrateStorage unalignedStorage() {
        return this.storage;
    }

    public ItemStack module() {
        return this.crateModuleHolder.module();
    }

    public void setModule(ItemStack itemStack) {
        this.crateModuleHolder.setModule(itemStack);
    }

    public ItemStack rowModules() {
        return this.crateModuleHolder.rowModules();
    }

    public void setRowModules(ItemStack itemStack) {
        this.crateModuleHolder.setRowModules(itemStack);
    }

    public void setModuleIn(Identifier identifier, ItemStack itemStack) {
        this.crateModuleHolder.setModuleIn(identifier, itemStack);
    }

    public int extraRows() {
        return this.crateModuleHolder.extraRows();
    }

    public List<ItemStack> trimToRows() {
        return this.crateModuleHolder.trimToRows();
    }

    public DeepCrateBlockEntity moduleHolder() {
        return this.crateModuleHolder.holder();
    }

    @Override
    public int getContainerSize() {
        return this.storage().size();
    }

    @Override
    public int getMaxStackSize() {
        return this.moduleHolder().storage().capacity();
    }

    /**
     * The per-item limit, as seen from outside: hoppers and pipes read this one, the menu reads the
     * argument-free version above.
     */
    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.storage().automationCapacityFor(itemStack);
    }

    @Override
    protected Component getDefaultName() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier == null) {
            return DEFAULT_NAME;
        }

        Component name = Component.translatable(crateTier.block().getDescriptionId());
        return this.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE
            ? name
            : Component.translatable("container.deepcrate.double", name);
    }

    /**
     * A lock on either half locks the pair. Checking only the half that was clicked would let a player
     * walk around the crate and open it from the other side.
     */
    @Override
    public boolean canOpen(Player player) {
        for (DeepCrateBlockEntity deepCrateBlockEntity : DeepCrateBlock.cratesFor(this)) {
            if (!deepCrateBlockEntity.canOpenOwnLock(player)) {
                return false;
            }
        }

        return true;
    }

    private boolean canOpenOwnLock(Player player) {
        return super.canOpen(player);
    }

    /**
     * Writes a slot at the crate's own limit, not at the one automation is told about. The inherited
     * version runs the stack through getMaxStackSize(ItemStack), which is held back to 64 while
     * lithium is installed and would cut a player's own stack in half.
     */
    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.storage().set(i, itemStack);
        this.setChanged();
    }

    /** What a crate refuses, it refuses through its slots too, which is where a player meets it. */
    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return this.storage().accepts(itemStack);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.storage().slots();
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.storage().replaceSlots(nonNullList);
    }

    @Override
    public CrateOpenData getScreenOpeningData(ServerPlayer serverPlayer) {
        Container container = DeepCrateBlock.containerFor(DeepCrateBlock.cratesFor(this));
        CrateTier crateTier = this.tier();
        CrateLayout crateLayout = DeepCrateApi.layoutFor(crateTier, container.getContainerSize() / crateTier.columns());
        return new CrateOpenData(
            container.getContainerSize(), crateLayout.rowsPerPage(), crateLayout.pageCount(), container.getMaxStackSize(), crateTier.columns()
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        List<DeepCrateBlockEntity> crates = DeepCrateBlock.cratesFor(this);
        Container container = DeepCrateBlock.containerFor(crates);
        CrateTier crateTier = this.tier();
        CrateLayout crateLayout = DeepCrateApi.layoutFor(crateTier, container.getContainerSize() / crateTier.columns());
        return new DeepCrateMenu(i, inventory, container, crates, crateLayout, crateTier.columns());
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        // storage(), not the field: a crate saved before anyone opened it would otherwise write the
        // nine slots it starts with rather than the size its tier calls for.
        CrateSave.save(valueOutput, this.storage(), this.modules);
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.storage = CrateSave.load(valueInput, this.modules);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        CrateSave.collectImplicitComponents(builder);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);
        CrateSave.removeComponentsFromTag(valueOutput);
    }

    @Override
    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
        if (this.level == null) {
            return;
        }

        List<ItemStack> list = this.storage().splitForVanilla();
        this.storage.clear();
        for (ItemStack itemStack : this.modules) {
            list.add(itemStack);
        }

        this.modules.clear();

        for (ItemStack itemStack : list) {
            CrateDrops.dropWhole(this.level, blockPos, 0.5, itemStack);
        }
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        this.crateLid.startOpen(containerUser);
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        this.crateLid.stopOpen(containerUser);
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.crateLid.getEntitiesWithContainerOpen();
    }

    public void recheckOpen() {
        this.crateLid.recheckOpen();
    }

    public static void lidAnimateTick(Level level, BlockPos blockPos, BlockState blockState, DeepCrateBlockEntity deepCrateBlockEntity) {
        deepCrateBlockEntity.crateLid.tickLid();
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        return this.crateLid.triggerEvent(i, j) || super.triggerEvent(i, j);
    }

    @Override
    public float getOpenNess(float f) {
        return this.crateLid.getOpenNess(f);
    }

    /**
     * Grows a crate to the size its tier and its row modules call for. Not done once and cached: the
     * row count changes while the game runs, and at load time the block state is not known yet
     * because loadAdditional runs before the block entity is bound to a level.
     */
    private void alignStorageWithTier() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier == null) {
            return;
        }

        this.storage.setTier(crateTier);
        int target = crateTier.slotCount() + this.extraRows() * crateTier.columns();
        if (target > this.storage.size()) {
            this.storage.grow(target);
        }
    }

    /**
     * A pair opens under one title: the name given to either half, or the shared default when neither
     * was named on an anvil.
     */
    @Override
    public Component getDisplayName() {
        for (DeepCrateBlockEntity deepCrateBlockEntity : DeepCrateBlock.cratesFor(this)) {
            if (deepCrateBlockEntity.getCustomName() != null) {
                return deepCrateBlockEntity.getCustomName();
            }
        }

        return this.getDefaultName();
    }
}
