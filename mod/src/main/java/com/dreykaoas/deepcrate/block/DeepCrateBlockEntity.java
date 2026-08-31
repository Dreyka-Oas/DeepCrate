package com.dreykaoas.deepcrate.block;

import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateStorage;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.StoredSlot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DeepCrateBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.deep_crate");
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private final CrateStorage storage = new CrateStorage();
    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
            DeepCrateBlockEntity.this.playSound(SoundEvents.BARREL_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
            DeepCrateBlockEntity.this.playSound(SoundEvents.BARREL_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int j) {
            level.blockEvent(blockPos, blockState.getBlock(), EVENT_SET_OPEN_COUNT, j);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof DeepCrateMenu deepCrateMenu && deepCrateMenu.getContainer() == DeepCrateBlockEntity.this;
        }
    };

    public DeepCrateBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(RegistryInit.BLOCK_ENTITY, blockPos, blockState);
    }

    @Override
    public int getContainerSize() {
        return CrateStorage.SLOT_COUNT;
    }

    @Override
    public int getMaxStackSize() {
        return CrateStorage.SLOT_LIMIT;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        // Container's default caps at the item's own limit, which is exactly the ceiling this crate exists
        // to lift.
        return CrateStorage.SLOT_LIMIT;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.storage.slots();
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.storage.replaceSlots(nonNullList);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new DeepCrateMenu(i, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ValueOutput.TypedOutputList<StoredSlot> typedOutputList = valueOutput.list("Slots", StoredSlot.CODEC);

        for (int i = 0; i < CrateStorage.SLOT_COUNT; i++) {
            ItemStack itemStack = this.storage.get(i);
            if (!itemStack.isEmpty()) {
                typedOutputList.add(StoredSlot.of(i, itemStack));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.storage.clear();

        for (StoredSlot storedSlot : valueInput.listOrEmpty("Slots", StoredSlot.CODEC)) {
            if (storedSlot.slot() >= 0 && storedSlot.slot() < CrateStorage.SLOT_COUNT) {
                this.storage.set(storedSlot.slot(), storedSlot.toStack());
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        // Deliberately not calling super's CONTAINER branch: ItemContainerContents runs through the
        // vanilla stack codec and would refuse a slot above 99. A broken crate drops its content on the
        // ground instead, see preRemoveSideEffects.
        super.collectImplicitComponents(builder);
        builder.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
        if (this.level != null) {
            List<ItemStack> list = this.storage.splitForVanilla();
            this.storage.clear();

            for (ItemStack itemStack : list) {
                Containers.dropItemStack(this.level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack);
            }
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

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public CrateStorage storage() {
        return this.storage;
    }

    private void playSound(SoundEvent soundEvent) {
        if (this.level != null) {
            this.level
                .playSound(
                    null,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    soundEvent,
                    SoundSource.BLOCKS,
                    0.5F,
                    this.level.random.nextFloat() * 0.1F + 0.9F
                );
        }
    }
}
