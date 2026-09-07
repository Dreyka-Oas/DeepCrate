package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.CratePairing;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.inventory.CrateOpenData;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** What the crate screen shows and opens onto, one crate or a paired one seen as a single container. */
public final class CrateMenuOpening {
    private CrateMenuOpening() {}

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
