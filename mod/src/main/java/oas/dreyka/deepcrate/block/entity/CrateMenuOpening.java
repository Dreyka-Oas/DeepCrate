package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.CratePairing;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.CrateOpenData;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/** What the crate screen shows and opens onto, one crate or a paired one seen as a single container. */
public final class CrateMenuOpening {
    private CrateMenuOpening() {}

    /**
     * Opens the crate under the pointer, server side only: the client opens its own copy when the
     * opening payload reaches it. The click counts as handled either way, or the held item would be
     * placed against the crate the player just opened.
     */
    public static InteractionResult open(Level level, BlockPos blockPos, Player player) {
        if (level instanceof ServerLevel && level.getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
            player.openMenu(deepCrateBlockEntity);
        }

        return InteractionResult.SUCCESS;
    }

    public static CrateOpenData screenOpeningData(DeepCrateBlockEntity crate) {
        Container container = CratePairing.containerFor(CratePairing.cratesFor(crate));
        CrateTier crateTier = crate.tier();
        CrateLayout crateLayout = DeepCrateApi.layoutFor(crateTier, container.getContainerSize() / crateTier.columns());
        return new CrateOpenData(
            container.getContainerSize(), crateLayout.rowsPerPage(), crateLayout.pageCount(), container.getMaxStackSize(), crateTier.columns()
        );
    }

    public static AbstractContainerMenu menu(DeepCrateBlockEntity crate, int i, Inventory inventory) {
        List<DeepCrateBlockEntity> crates = CratePairing.cratesFor(crate);
        Container container = CratePairing.containerFor(crates);
        CrateTier crateTier = crate.tier();
        CrateLayout crateLayout = DeepCrateApi.layoutFor(crateTier, container.getContainerSize() / crateTier.columns());
        return new DeepCrateMenu(i, inventory, container, crates, crateLayout, crateTier.columns());
    }
}
