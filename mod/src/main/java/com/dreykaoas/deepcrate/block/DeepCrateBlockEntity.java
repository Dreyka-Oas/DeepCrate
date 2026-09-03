package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.api.CrateLayout;
import com.dreykaoas.deepcrate.api.CrateModules;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateOpenData;
import com.dreykaoas.deepcrate.inventory.CratePairContainer;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.StoredModule;
import com.dreykaoas.deepcrate.inventory.StoredSlot;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.jspecify.annotations.Nullable;

public class DeepCrateBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity, ExtendedScreenHandlerFactory<CrateOpenData> {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.crate");
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private CrateStorage storage = new CrateStorage(CrateTier.COLUMNS, DeepCrateApi.BASE_CAPACITY);
    private final CrateModules modules = new CrateModules();

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
        this.alignCapacityWithHolder();
        return this.storage;
    }

    public CrateModules modules() {
        return this.modules;
    }

    public ItemStack module() {
        return this.modules.get(RegistryInit.CAPACITY_SLOT);
    }

    public void setModule(ItemStack itemStack) {
        this.setModuleIn(RegistryInit.CAPACITY_SLOT, itemStack);
    }

    public ItemStack rowModules() {
        return this.modules.get(RegistryInit.ROWS_SLOT);
    }

    public void setRowModules(ItemStack itemStack) {
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
    public void setModuleIn(Identifier identifier, ItemStack itemStack) {
        this.modules.set(identifier, itemStack);
        this.storage().setCapacity(DeepCrateApi.capacityAmong(this.modules));
        for (DeepCrateBlockEntity deepCrateBlockEntity : DeepCrateBlock.cratesFor(this)) {
            deepCrateBlockEntity.storage();
            deepCrateBlockEntity.setChanged();
        }

        this.setChanged();
    }

    /** Rows this crate has beyond its tier's own, read from wherever the pair keeps its modules. */
    public int extraRows() {
        return DeepCrateApi.rowsAmong(this.moduleHolder().modules);
    }

    /**
     * Cuts the crate back to the size its modules now call for and hands back what was above it.
     * Called when the screen closes, the same rule the capacity module follows: pulling modules out
     * spills rather than silently swallowing.
     */
    public List<ItemStack> trimToRows() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier == null) {
            return List.of();
        }

        return this.storage().trimTo(crateTier.slotCount() + this.extraRows() * CrateTier.COLUMNS);
    }

    /**
     * Where the module of a pair lives.
     *
     * A crate that already holds one keeps it, whichever side of the pair it ended up on; otherwise
     * it is the half the game calls first. Deciding rather than moving anything means both halves
     * always name the same holder, and a crate that marries another does not have to be rewritten.
     */
    public DeepCrateBlockEntity moduleHolder() {
        if (this.level == null || this.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE) {
            return this;
        }

        BlockPos blockPos = DeepCrateBlock.connectedPos(this.getBlockState(), this.getBlockPos());
        if (!(this.level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity other)) {
            return this;
        }

        if (this.hasAnyModule()) {
            return !other.hasAnyModule() || this.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.RIGHT ? this : other;
        }

        if (other.hasAnyModule()) {
            return other;
        }

        return this.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.RIGHT ? this : other;
    }

    private boolean hasAnyModule() {
        return !this.modules.isEmpty();
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
        if (!this.modules.isEmpty()) {
            ValueOutput.TypedOutputList<StoredModule> savedModules = valueOutput.list("Modules", StoredModule.CODEC);
            for (Identifier identifier : this.modules.ids()) {
                savedModules.add(StoredModule.of(identifier, this.modules.get(identifier)));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.modules.clear();
        for (StoredModule storedModule : valueInput.listOrEmpty("Modules", StoredModule.CODEC)) {
            this.modules.set(storedModule.id(), storedModule.toStack());
        }

        // A crate saved before the cells were a registry kept its two stacks under their own keys.
        // Read once and never written again, so a world upgrades on the first load of each crate.
        if (this.modules.isEmpty()) {
            this.modules.set(RegistryInit.CAPACITY_SLOT, valueInput.read("Module", ItemStack.CODEC).orElse(ItemStack.EMPTY));
            this.modules.set(RegistryInit.ROWS_SLOT, valueInput.read("RowModules", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }

        // Read the slots first: the crate has to be at least large enough to hold every one of them,
        // whatever Size says and whatever the tier says. A missing or shrunken Size must never be a
        // reason to drop stored items on the floor of the save file.
        List<StoredSlot> storedSlots = new ArrayList<>();
        int highest = 0;
        for (StoredSlot storedSlot : valueInput.listOrEmpty("Slots", StoredSlot.CODEC)) {
            if (storedSlot.slot() >= 0) {
                storedSlots.add(storedSlot);
                highest = Math.max(highest, storedSlot.slot() + 1);
            }
        }

        int size = Math.max(Math.max(CrateTier.COLUMNS, highest), valueInput.getIntOr("Size", CrateTier.COLUMNS));
        this.storage = new CrateStorage(size, DeepCrateApi.capacityAmong(this.modules));

        for (StoredSlot storedSlot : storedSlots) {
            this.storage.restore(storedSlot.slot(), storedSlot.toStack());
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
        valueOutput.discard("Modules");
        // The two keys of the previous format, still discarded: a crate saved by it and picked up by
        // this one must not carry its old modules along in the item.
        valueOutput.discard("Module");
        valueOutput.discard("RowModules");
        valueOutput.discard("Size");
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

        // One entity per stack of 64: the vanilla helper cuts each stack into ten-to-thirty pieces, and
        // a full echo crate would put several thousand entities on one block in a single tick.
        for (ItemStack itemStack : list) {
            ItemEntity itemEntity = new ItemEntity(this.level, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, itemStack);
            itemEntity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itemEntity);
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
     * Grows a crate to the size its tier and its row modules call for. Not done once and cached: the
     * row count changes while the game runs, and at load time the block state is not known yet
     * because loadAdditional runs before the block entity is bound to a level.
     */
    private void alignStorageWithTier() {
        CrateTier crateTier = DeepCrateApi.tierOf(this.getBlockState().getBlock());
        if (crateTier == null) {
            return;
        }

        int target = crateTier.slotCount() + this.extraRows() * CrateTier.COLUMNS;
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

    /**
     * A crate paired with another follows its partner's module. Without this the half that does not
     * hold the module keeps the 64 it was loaded with, and every insertion into it is clamped.
     */
    private void alignCapacityWithHolder() {
        DeepCrateBlockEntity holder = this.moduleHolder();
        if (holder != this) {
            this.storage.setCapacity(DeepCrateApi.capacityAmong(holder.modules));
        }
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
            Direction direction = DeepCrateBlock.connectedDirection(blockState);
            x += direction.getStepX() * 0.5;
            z += direction.getStepZ() * 0.5;
        }

        level.playSound(null, x, blockPos.getY() + 0.5, z, soundEvent, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
