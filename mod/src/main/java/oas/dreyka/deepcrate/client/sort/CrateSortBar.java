package oas.dreyka.deepcrate.client.sort;

import oas.dreyka.deepcrate.client.screen.hook.PanelArea;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import oas.dreyka.deepcrate.net.CrateSortPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

/**
 * The sort buttons above the crate, one per registered order, and what pressing one sends to the
 * server.
 */
public final class CrateSortBar {
    /** The two sort buttons stand above the panel, flush with its left edge and clear of it. */
    private static final int SORT_GAP = 4;
    private static final int SORT_BUTTON_GAP = 2;

    private final DeepCrateMenu menu;
    private final List<SortButton> sortButtons = new ArrayList<>();
    /** Kept by name and not by position, so a mod loaded since does not shift every direction by one. */
    private final Map<Identifier, Boolean> sortDirections = new HashMap<>();

    public CrateSortBar(DeepCrateMenu menu) {
        this.menu = menu;
    }

    /** Rebuilds the buttons for the current layout: called from init(), again on every window resize. */
    public void init(PanelArea panelArea, int leftPos, int topPos, int imageWidth) {
        // Read before the widgets are replaced: a window resized mid-session rebuilds the screen, and
        // a button that forgot its direction would sort the same way twice in a row.
        for (SortButton sortButton : this.sortButtons) {
            this.sortDirections.put(sortButton.order().id(), sortButton.isReversed());
        }

        this.sortButtons.clear();
        List<CrateSortOrder> orders = DeepCrateClientApi.sortOrders();
        // Enough orders to outgrow the panel wrap onto a second line above the first, rather than
        // running off the side of the screen.
        int perRow = Math.max(1, imageWidth / (SortButton.SIZE + SORT_BUTTON_GAP));
        int sortRows = (orders.size() + perRow - 1) / perRow;
        for (int i = 0; i < orders.size(); i++) {
            CrateSortOrder crateSortOrder = orders.get(i);
            int row = sortRows - 1 - i / perRow;
            this.sortButtons.add(
                panelArea.addClickableWidget(
                    new SortButton(
                        leftPos + i % perRow * (SortButton.SIZE + SORT_BUTTON_GAP),
                        topPos - SORT_GAP - SortButton.SIZE - row * (SortButton.SIZE + SORT_BUTTON_GAP),
                        crateSortOrder,
                        this.sortDirections.getOrDefault(crateSortOrder.id(), false),
                        this::sort
                    )
                )
            );
        }
    }

    /**
     * The order is worked out here and sent whole, because it comes from the item names in the
     * language this player reads and the crate has no idea what that is.
     */
    private void sort(CrateSortOrder crateSortOrder, boolean reversed) {
        ClientPlayNetworking.send(
            new CrateSortPayload(this.menu.containerId, DeepCrateClientApi.order(crateSortOrder, this.menu.getContainer(), reversed))
        );
    }
}
