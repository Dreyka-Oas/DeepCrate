package oas.dreyka.deepcrate.inventory.menu;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.CrateContainer;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * What a module cell changing does to the menu it sits in: the capacity it reports, and, for a row
 * module, whether the screen has to reopen. Built around the menu's own reference, the same pattern
 * {@code CrateModuleHolder} uses for a block entity, reached only through the accessors DeepCrateMenu
 * exposes for it because this class sits in the inventory.menu subpackage rather than in inventory.
 */
public final class CrateModuleReaction {
    private final DeepCrateMenu menu;

    public CrateModuleReaction(DeepCrateMenu menu) {
        this.menu = menu;
    }

    public void onModuleChanged() {
        this.menu.setCapacity(DeepCrateApi.capacityAmong(this.moduleStacks()));
        this.reopenIfRowCountChanged();

        if (this.menu.crates().isEmpty()) {
            if (this.menu.getContainer() instanceof CrateContainer crateContainer) {
                crateContainer.setCapacity(this.menu.capacity());
            }

            return;
        }

        // Both halves follow the one module, so a hopper reaching the far half sees the same limit.
        for (DeepCrateBlockEntity deepCrateBlockEntity : this.menu.crates()) {
            deepCrateBlockEntity.storage().setCapacity(this.menu.capacity());
        }
    }

    /** The cells as a plain list, which is what the two api helpers walk. */
    public List<ItemStack> moduleStacks() {
        Container moduleContainer = this.menu.moduleContainer();
        List<ItemStack> stacks = new ArrayList<>(moduleContainer.getContainerSize());
        for (int i = 0; i < moduleContainer.getContainerSize(); i++) {
            stacks.add(moduleContainer.getItem(i));
        }

        return stacks;
    }

    /**
     * A row module changes how many slots the screen has, and a menu's slot list is fixed once it is
     * built. So the crate is opened again, at the start of the next tick rather than inside the click
     * that caused it: closing a menu mid-click would put whatever the player is carrying on the
     * ground.
     */
    private void reopenIfRowCountChanged() {
        int rows = DeepCrateApi.rowsAmong(this.moduleStacks());
        if (rows == this.menu.rowModuleCount()) {
            return;
        }

        this.menu.setRowModuleCount(rows);
        if (this.menu.crates().isEmpty()
            || !(this.menu.player() instanceof ServerPlayer serverPlayer)
            || !(this.menu.player().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        DeepCrateBlockEntity deepCrateBlockEntity = this.menu.crates().get(0);
        serverLevel.getServer().execute(() -> {
            if (serverPlayer.containerMenu == this.menu) {
                serverPlayer.openMenu(deepCrateBlockEntity);
            }
        });
    }
}
