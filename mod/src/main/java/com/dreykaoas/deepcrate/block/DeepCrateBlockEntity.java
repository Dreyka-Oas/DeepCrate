package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.api.CrateLayout;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateOpenData;
import com.dreykaoas.deepcrate.inventory.CratePairContainer;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.StoredSlot;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DeepCrateBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity, ExtendedScreenHandlerFactory<CrateOpenData> {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.crate");
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private CrateStorage storage = new CrateStorage(CrateTier.COLUMNS, DeepCrateApi.BASE_CAPACITY);
    private ItemStack module = ItemStack.EMPTY;
    private boolean storageMatchesTier;

    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
            DeepCrateBlockEntity.playSound(level, blockPos, blockState, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
            DeepCrateBlockEntity.playSound(level, blockPos, blockState, SoundEvents.CHEST_CLOSE);
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
            return container == DeepCrateBlockEntity.this
                || container instanceof CratePairContainer cratePairContainer && cratePairContainer.contains(DeepCrateBlockEntity.this);
        }
    };

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
        return this.storage;
    }

    public ItemStack module() {
        return this.module;
    }

    public void setModule(ItemStack itemStack) {
        this.module = itemStack;
        this.storage().setCapacity(DeepCrateApi.capacityOf(itemStack));
        this.setChanged();
    }

    /**
     * Where the module of a pair lives: on the half the game calls first, so a pair holds exactly one
     * module and separating the two can neither duplicate nor lose it.
     */
    public DeepCrateBlockEntity moduleHolder() {
        if (this.level != null && this.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.LEFT) {
            BlockPos blockPos = DeepCrateBlock.connectedPos(this.getBlockState(), this.getBlockPos());
            if (this.level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity other) {
                return other;
            }
        }

        return this;
    }

    @Override
    public int getContainerSize() {
        return this.storage().size();
    }

    @Override
    public int getMaxStackSize() {
        return this.moduleHolder().storage().capacity();
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        // Container's default caps at the item's own limit, which is the ceiling this crate lifts.
        return this.getMaxStackSize();
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
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
        CrateLayout crateLayout = DeepCrateApi.layoutFor(this.tier(), container.getContainerSize() / CrateTier.COLUMNS);
        return new CrateOpenData(container.getContainerSize(), crateLayout.rowsPerPage(), crateLayout.pageCount(), container.getMaxStackSize());
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        List<DeepCrateBlockEntity> crates = DeepCrateBlock.cratesFor(this);
        Container container = DeepCrateBlock.containerFor(crates);
        CrateLayout crateLayout = DeepCrateApi.layoutFor(this.tier(), container.getContainerSize() / CrateTier.COLUMNS);
        return new DeepCrateMenu(i, inventory, container, crates, crateLayout);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        // storage(), not the field: a crate saved before anyone opened it would otherwise write the
        // nine slots it starts with rather than the size its tier calls for.
        CrateStorage crateStorage = this.storage();
        ValueOutput.TypedOutputList<StoredSlot> typedOutputList = valueOutput.list("Slots", StoredSlot.CODEC);

        for (int i = 0; i < crateStorage.size(); i++) {
            ItemStack itemStack = crateStorage.get(i);
            if (!itemStack.isEmpty()) {
                typedOutputList.add(StoredSlot.of(i, itemStack));
            }
        }

        valueOutput.putInt("Size", crateStorage.size());
        if (!this.module.isEmpty()) {
            valueOutput.store("Module", ItemStack.CODEC, this.module);
        }
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.module = valueInput.read("Module", ItemStack.CODEC).orElse(ItemStack.EMPTY);

        // The saved size wins over the tier: a tier that lost rows in an update must not leave the
        // slots beyond its new end unreachable.
        int size = Math.max(CrateTier.COLUMNS, valueInput.getIntOr("Size", CrateTier.COLUMNS));
        this.storage = new CrateStorage(size, DeepCrateApi.capacityOf(this.module));
        this.storageMatchesTier = false;

        for (StoredSlot storedSlot : valueInput.listOrEmpty("Slots", StoredSlot.CODEC)) {
            if (storedSlot.slot() >= 0 && storedSlot.slot() < size) {
                this.storage.set(storedSlot.slot(), storedSlot.toStack());
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        // Deliberately emptied: ItemContainerContents runs through the vanilla stack codec, which
        // refuses a slot above 99. A broken crate drops its content on the ground instead.
        builder.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        super.removeComponentsFromTag(valueOutput);
        valueOutput.discard("Slots");
        valueOutput.discard("Module");
        valueOutput.discard("Size");
    }

    @Override
    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
        if (this.level == null) {
            return;
        }

        List<ItemStack> list = this.storage().splitForVanilla();
        this.storage.clear();
        if (!this.module.isEmpty()) {
            list.add(this.module);
            this.module = ItemStack.EMPTY;
        }

        for (ItemStack itemStack : list) {
            Containers.dropItemStack(this.level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack);
        }
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter
                .incrementOpeners(containerUser.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), containerUser.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(containerUser.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public static void lidAnimateTick(Level level, BlockPos blockPos, BlockState blockState, DeepCrateBlockEntity deepCrateBlockEntity) {
        deepCrateBlockEntity.lidController.tickLid();
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == EVENT_SET_OPEN_COUNT) {
            this.lidController.shouldBeOpen(j > 0);
            return true;
        }

        return super.triggerEvent(i, j);
    }

    @Override
    public float getOpenNess(float f) {
        return this.lidController.getOpenness(f);
    }

    /**
     * Grows a freshly loaded crate to the size its tier calls for. Deferred until the block state is
     * known, because loadAdditional runs before the block entity is bound to a level.
     */
    private void alignStorageWithTier() {
        if (this.storageMatchesTier) {
            return;
        }

        this.storageMatchesTier = true;
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier != null && crateTier.slotCount() > this.storage.size()) {
            this.storage.grow(crateTier.slotCount());
        }
    }

    private static void playSound(Level level, BlockPos blockPos, BlockState blockState, SoundEvent soundEvent) {
        // Only one half speaks, otherwise a pair opens twice as loud as a single crate.
        if (blockState.getValue(DeepCrateBlock.TYPE) == ChestType.LEFT) {
            return;
        }

        level.playSound(
            null,
            blockPos.getX() + 0.5,
            blockPos.getY() + 0.5,
            blockPos.getZ() + 0.5,
            soundEvent,
            SoundSource.BLOCKS,
            0.5F,
            level.random.nextFloat() * 0.1F + 0.9F
        );
    }
}
