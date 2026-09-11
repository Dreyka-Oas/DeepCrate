package oas.dreyka.deepcrate.inventory.menu.reaction;

import oas.dreyka.deepcrate.block.entity.CrateDrops;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a crate screen closing does once {@code super.removed(player)} has run: stop the crate's own
 * open tracking and, server side, spill whatever a shrunk crate can no longer hold. Built around the
 * menu's own reference, the same pattern {@code CrateModuleHolder} uses for a block entity, reached
 * only through the accessors DeepCrateMenu exposes for it because this class sits in the
 * inventory.menu subpackage rather than in inventory.
 */
public final class CrateMenuCleanup {
    private final DeepCrateMenu menu;

    public CrateMenuCleanup(DeepCrateMenu menu) {
        this.menu = menu;
    }

    public void afterRemoved(Player player) {
        this.menu.wiring().crate().stopOpen(player);

        // A module pulled out leaves slots above the new capacity; the excess goes to the ground, as
        // asked. Server side only: the client copy would drop a second set of ghosts.
        if (player.level().isClientSide()) {
            return;
        }

        for (DeepCrateBlockEntity deepCrateBlockEntity : this.menu.wiring().crates()) {
            if (anotherScreenIsOpen(deepCrateBlockEntity, player)) {
                continue;
            }

            List<ItemStack> spilled = new ArrayList<>(deepCrateBlockEntity.storage().overflow());
            // Row modules taken out shrink the crate the same way: what sat in the rows that are gone
            // goes to the ground rather than staying in a slot nobody can reach.
            spilled.addAll(deepCrateBlockEntity.trimToRows());
            if (spilled.isEmpty()) {
                continue;
            }

            for (ItemStack itemStack : spilled) {
                CrateDrops.dropWhole(player.level(), deepCrateBlockEntity.getBlockPos(), 1.0, itemStack);
            }

            deepCrateBlockEntity.setChanged();
        }
    }

    /**
     * Whether someone other than the player leaving still has this crate open. Cutting a crate back
     * under an open screen would leave that menu holding slots the crate no longer has, and its next
     * click would ask for an index past the end. The player closing is not counted: the game clears
     * their own menu only after this call returns.
     */
    private static boolean anotherScreenIsOpen(DeepCrateBlockEntity deepCrateBlockEntity, Player closing) {
        for (ContainerUser containerUser : deepCrateBlockEntity.getEntitiesWithContainerOpen()) {
            if (containerUser.getLivingEntity() != closing) {
                return true;
            }
        }

        return false;
    }
}
