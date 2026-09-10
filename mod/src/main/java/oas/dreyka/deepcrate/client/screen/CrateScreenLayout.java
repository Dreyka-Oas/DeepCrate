package oas.dreyka.deepcrate.client.screen;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.client.screen.hook.CrateScreenCallback;
import oas.dreyka.deepcrate.client.screen.hook.PanelArea;
import oas.dreyka.deepcrate.client.sort.CrateSortBar;
import oas.dreyka.deepcrate.inventory.CratePanelGeometry;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * What a crate screen hangs around its panel and where each piece lands: the module tab, the search
 * field, the sort bar and the page bar, laid out again on every resize because a window resized
 * mid-session moves all of them.
 */
final class CrateScreenLayout {
    /** Stands in until the first layout, so a click never has to test a field that is not built yet. */
    private static final PanelArea EMPTY_AREA = new PanelArea(0, 0, 0, 0, new PanelArea.WidgetSink() {
        @Override
        public <T extends AbstractWidget> T add(T widget) {
            return widget;
        }
    });

    private final CratePageBar cratePageBar;
    private final CrateSortBar crateSortBar;
    private final CrateSearch crateSearch;
    private final int moduleTabHeight;

    private PanelArea panelArea = EMPTY_AREA;
    private @Nullable SearchBox searchBox;

    CrateScreenLayout(DeepCrateMenu deepCrateMenu) {
        this.cratePageBar = new CratePageBar(deepCrateMenu);
        this.crateSortBar = new CrateSortBar(deepCrateMenu);
        this.crateSearch = new CrateSearch(deepCrateMenu);
        this.moduleTabHeight = CratePanel.MODULE_TAB_CAP * 2 + DeepCrateApi.moduleSlots().size() * CratePanel.MODULE_TAB_CELL;
    }

    void build(DeepCrateScreen screen, PanelArea area, Font font, int inventoryLabelY, Component playerInventoryTitle) {
        this.panelArea = area;
        this.panelArea.keepClickable(
            area.left() + CratePanelGeometry.MODULE_X - CratePanel.MODULE_TAB_MARGIN,
            area.top() + CratePanelGeometry.MODULE_Y - CratePanel.MODULE_TAB_MARGIN,
            CratePanel.MODULE_TAB_WIDTH,
            this.moduleTabHeight
        );

        // On the inventory line, to the right of its label and running to the edge of the panel.
        int labelEnd = CratePanelGeometry.GRID_LEFT + font.width(playerInventoryTitle) + 6;
        this.searchBox = this.crateSearch.buildBox(
            font, area.left() + labelEnd, area.top() + inventoryLabelY - 2, area.width() - labelEnd - CratePanelGeometry.GRID_LEFT
        );
        this.panelArea.addWidget(this.searchBox);

        this.crateSortBar.init(this.panelArea, area.left(), area.top(), area.width());
        this.cratePageBar.init(this.panelArea, area.left(), area.top(), area.width());
        // Last, so a listener sees the screen as a player will and can measure against what is there.
        CrateScreenCallback.EVENT.invoker().onScreenInit(screen, this.panelArea);
    }

    boolean holdsClick(double pointerX, double pointerY) {
        return this.panelArea.holdsClick(pointerX, pointerY);
    }

    void tick() {
        this.cratePageBar.tick();
    }

    boolean dims(Slot slot) {
        return this.crateSearch.dims(slot);
    }

    /** Whether the search field takes the key rather than letting it reach the rest of the screen. */
    boolean claimsKey(KeyEvent keyEvent) {
        if (this.searchBox == null || !this.searchBox.isFocused() || keyEvent.key() == InputConstants.KEY_ESCAPE) {
            return false;
        }

        this.searchBox.keyPressed(keyEvent);
        return true;
    }
}
