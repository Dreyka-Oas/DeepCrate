package oas.dreyka.deepcrate.client.screen;

import oas.dreyka.deepcrate.client.screen.config.ConfigValues;
import oas.dreyka.deepcrate.client.screen.hook.CrateTooltipCallback;
import oas.dreyka.deepcrate.client.screen.hook.PanelArea;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The crate screen, built out of the chest texture of the base game.
 *
 * Nothing is shipped for the background: the header, the module strip, the slot frame, the grid and
 * the player inventory are blits from generic_54, which is what keeps a crate looking like a chest at
 * any row count.
 */
public class DeepCrateScreen extends AbstractContainerScreen<DeepCrateMenu> {
    private final int rows;
    private final int columns;
    private final CrateScreenLayout screenLayout;

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.rows = deepCrateMenu.layout().rowsPerPage();
        this.columns = deepCrateMenu.columns();
        this.screenLayout = new CrateScreenLayout(deepCrateMenu);
        // Exactly a chest of this many rows: the module hangs off the left edge rather than taking a
        // band inside the panel.
        this.imageWidth = deepCrateMenu.panelWidth();
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        PanelArea area = new PanelArea(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, new PanelArea.WidgetSink() {
            @Override
            public <T extends AbstractWidget> T add(T widget) {
                return DeepCrateScreen.this.addRenderableWidget(widget);
            }
        });
        this.screenLayout.build(this, area, this.font, this.inventoryLabelY, this.playerInventoryTitle);
    }

    /**
     * The page buttons, the sort buttons and the module tab sit past the edges of the panel, which the
     * game otherwise counts as outside the screen: releasing a click there drops whatever the player
     * is carrying on the ground. Anything a mod hangs there goes through the same list.
     */
    @Override
    protected boolean hasClickedOutside(double d, double e, int i, int j) {
        return super.hasClickedOutside(d, e, i, j) && !this.screenLayout.holdsClick(d, e);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.screenLayout.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        // AbstractContainerScreen leaves this to the subclass, as ContainerScreen does; without it no
        // slot ever shows a tooltip.
        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        CrateScreenBackground.render(guiGraphics, this.menu, this.leftPos, this.topPos, this.imageWidth, this.rows, this.columns);
    }

    /**
     * While the search field holds the keyboard, no key reaches the rest of the screen. The base
     * class only asks the focused widget first and then acts on whatever it did not claim, and a
     * plain letter is claimed by nobody: the "e" of "emerald" would close the crate, a digit would swap
     * a slot into the hotbar, and the drop key would throw the item under the pointer. Escape is the
     * one key left through, so the screen can still be closed without reaching for the mouse.
     */
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return this.screenLayout.claimsKey(keyEvent) || super.keyPressed(keyEvent);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot, int i, int j) {
        if (CrateSlotCount.shortens(slot)) {
            CrateSlotCount.render(guiGraphics, this.font, slot, this.imageWidth);
        } else {
            super.renderSlot(guiGraphics, slot, i, j);
        }

        if (this.screenLayout.dims(slot)) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0101010);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        CrateTooltipCallback.EVENT.invoker().addLines(this.menu, this.hoveredSlot, itemStack, lines);
        return lines;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        // The page being read, as a bare number against the right edge of the title line.
        String page = String.valueOf(this.menu.page() + 1);
        int pageX = this.imageWidth - 7 - this.font.width(page);
        guiGraphics.drawString(this.font, page, pageX, this.titleLabelY, 0xFF404040, false);

        // A crate named on an anvil can be longer than the panel; the name stops before the number
        // rather than running under it.
        int room = pageX - this.titleLabelX - 4;
        Component title = ConfigValues.clip(this.font, this.title, room);
        guiGraphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }
}
