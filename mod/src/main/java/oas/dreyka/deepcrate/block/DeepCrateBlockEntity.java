package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModules;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.CrateOpenData;
import oas.dreyka.deepcrate.inventory.CrateStorage;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DeepCrateBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity, ExtendedScreenHandlerFactory<CrateOpenData> {
    private CrateStorage storage = new CrateStorage(CrateTier.DEFAULT_COLUMNS, CrateConfig.baseCapacity);
    private final CrateModules modules = new CrateModules();
    private final CrateLid crateLid = new CrateLid(this);
    final CrateModuleHolder crateModuleHolder = new CrateModuleHolder(this);

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
        CrateTierBinding.align(this);
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
        return CrateNaming.defaultName(this);
    }

    /**
     * A lock on either half locks the pair. Checking only the half that was clicked would let a player
     * walk around the crate and open it from the other side.
     */
    @Override
    public boolean canOpen(Player player) {
        for (DeepCrateBlockEntity deepCrateBlockEntity : CratePairing.cratesFor(this)) {
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
        return CrateMenuOpening.screenOpeningData(this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return CrateMenuOpening.menu(this, i, inventory);
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
        CrateDrops.preRemoveSideEffects(this, blockPos);
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

    @Override
    public Component getDisplayName() {
        return CrateNaming.displayName(this);
    }
}
