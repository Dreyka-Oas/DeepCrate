package oas.dreyka.deepcrate.inventory.menu.reaction;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.container.CrateContainer;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
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

        if (this.menu.wiring().crates().isEmpty()) {
            if (this.menu.wiring().crate() instanceof CrateContainer crateContainer) {
                crateContainer.setCapacity(this.menu.capacity());
            }

            return;
        }

        // Both halves follow the one module, so a hopper reaching the far half sees the same limit.
        for (DeepCrateBlockEntity deepCrateBlockEntity : this.menu.wiring().crates()) {
            deepCrateBlockEntity.storage().setCapacity(this.menu.capacity());
        }
    }

    /** The cells as a plain list, which is what the two api helpers walk. */
    public List<ItemStack> moduleStacks() {
        Container moduleContainer = this.menu.wiring().moduleContainer();
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
        if (this.menu.wiring().crates().isEmpty()
            || !(this.menu.wiring().player() instanceof ServerPlayer serverPlayer)
            || !(this.menu.wiring().player().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<DeepCrateBlockEntity> crates = List.copyOf(this.menu.wiring().crates());
        DeepCrateBlockEntity deepCrateBlockEntity = crates.get(0);
        serverLevel.getServer().execute(() -> {
            if (serverPlayer.containerMenu == this.menu) {
                silently(crates, 0, () -> serverPlayer.openMenu(deepCrateBlockEntity));
            }
        });
    }

    /**
     * Runs the reopen with every crate's lid held quiet. {@code openMenu} closes the old screen before
     * it opens the new one, so without this the crate slams shut and swings open again in the player's
     * ear, for a rebuild they never asked for. Both halves of a pair count their own openers, hence
     * the walk down the list.
     */
    private static void silently(List<DeepCrateBlockEntity> crates, int index, Runnable action) {
        if (index == crates.size()) {
            action.run();
            return;
        }

        crates.get(index).lidSilently(() -> silently(crates, index + 1, action));
    }
}
