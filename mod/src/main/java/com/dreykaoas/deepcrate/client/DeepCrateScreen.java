package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.DeepCrateSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int HEADER_HEIGHT = 17;
    /** The only band of the texture that is bare panel, measured on generic_54: rows 125 to 138. */
    private static final int BARE_PANEL_V = 125;
    private static final int BARE_PANEL_HEIGHT = 14;
    private static final int SLOT_FRAME_U = 7;
    private static final int SLOT_FRAME_V = 17;
    private static final int PLAYER_PANEL_V = 126;
    private static final int PLAYER_PANEL_HEIGHT = 96;
    private static final int PAGE_BUTTON_SIZE = 16;
    private static final int PAGE_BUTTONS_PER_COLUMN = 4;
    /** The gap between the last crate row and the first inventory row is fourteen pixels; this fits it. */
    private static final int SEARCH_HEIGHT = 11;
    /** The tab the module slot sits on, left of the panel: its own small panel with the same border. */
    private static final Identifier MODULE_TAB = Identifier.fromNamespaceAndPath("deepcrate", "textures/gui/module_tab.png");
    private static final int MODULE_TAB_WIDTH = 28;
    /** Two cells: the capacity module, then the row modules under it. */
    private static final int MODULE_TAB_HEIGHT = 46;
    /** The tab is drawn this far up and left of the slot, so its frame lands exactly around it. */
    private static final int MODULE_TAB_MARGIN = 6;
    private static final int MODULE_TAB_TEXTURE = 64;
    /** Past four digits a count runs out of its cell, so it is shortened and the tooltip carries the truth. */
    private static final int ABBREVIATE_ABOVE = 999;

    private final int rows;
    private final List<Button> pageButtons = new ArrayList<>();

    private EditBox searchBox;
    private String query = "";

    public DeepCrateScreen(DeepCrateMenu deepCrateMenu, Inventory inventory, Component component) {
        super(deepCrateMenu, inventory, component);
        this.rows = deepCrateMenu.layout().rowsPerPage();
        // Exactly a chest of this many rows: the module hangs off the left edge rather than taking a
        // band inside the panel.
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // On the inventory line, to the right of its label and running to the edge of the panel.
        int labelEnd = DeepCrateMenu.GRID_LEFT + this.font.width(this.playerInventoryTitle) + 6;
        this.searchBox = new EditBox(
            this.font,
            this.leftPos + labelEnd,
            this.topPos + this.inventoryLabelY - 2,
            this.imageWidth - labelEnd - DeepCrateMenu.GRID_LEFT,
            SEARCH_HEIGHT,
            Component.translatable("screen.deepcrate.search")
        );
        this.searchBox.setHint(Component.translatable("screen.deepcrate.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setResponder(text -> this.query = text.toLowerCase(Locale.ROOT).trim());
        this.addRenderableWidget(this.searchBox);

        if (this.menu.layout().pageCount() < 2) {
            return;
        }

        // Stacked down the right edge, outside the panel: the crate grid already fills the width.
        // Four to a column, then a second column further right, so a crate with many pages does not
        // grow a strip taller than the screen.
        this.pageButtons.clear();
        for (int page = 0; page < this.menu.layout().pageCount(); page++) {
            int target = page;
            this.pageButtons.add(
                this.addRenderableWidget(
                    Button.builder(Component.literal(String.valueOf(page + 1)), button -> this.menu.setPage(target))
                        .bounds(
                            this.leftPos + this.imageWidth + 3 + page / PAGE_BUTTONS_PER_COLUMN * (PAGE_BUTTON_SIZE + 2),
                            this.topPos + HEADER_HEIGHT + page % PAGE_BUTTONS_PER_COLUMN * (PAGE_BUTTON_SIZE + 2),
                            PAGE_BUTTON_SIZE,
                            PAGE_BUTTON_SIZE
                        )
                        .build()
                )
            );
        }
    }

    /**
     * The page buttons sit past the right edge of the panel, which the game otherwise counts as
     * outside the screen: releasing a click there drops whatever the player is carrying on the ground.
     */
    @Override
    protected boolean hasClickedOutside(double d, double e, int i, int j) {
        return super.hasClickedOutside(d, e, i, j) && !this.isOverPageButtons(d, e) && !this.isOverModuleTab(d, e, i, j);
    }

    private boolean isOverModuleTab(double d, double e, int i, int j) {
        int tabX = i + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN;
        int tabY = j + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN;
        return d >= tabX && d < tabX + MODULE_TAB_WIDTH && e >= tabY && e < tabY + MODULE_TAB_HEIGHT;
    }

    private boolean isOverPageButtons(double d, double e) {
        for (Button button : this.pageButtons) {
            if (button.isMouseOver(d, e)) {
                return true;
            }
        }

        return false;
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
        int x = this.leftPos;
        int y = this.topPos;

        // Only the rows this page holds get slot cells; the last page of a crate whose rows do not
        // divide evenly would otherwise show a row of cells no slot lives in.
        int rowsOnPage = Math.min(
            this.rows,
            Math.max(0, this.menu.getContainer().getContainerSize() / CrateTier.COLUMNS - this.menu.page() * this.rows)
        );

        blit(guiGraphics, x, y, 0, 0, this.imageWidth, HEADER_HEIGHT + rowsOnPage * 18);
        if (rowsOnPage < this.rows) {
            this.fillBarePanel(guiGraphics, x, y + HEADER_HEIGHT + rowsOnPage * 18, (this.rows - rowsOnPage) * 18);
        }

        blit(guiGraphics, x, y + HEADER_HEIGHT + this.rows * 18, 0, PLAYER_PANEL_V, this.imageWidth, PLAYER_PANEL_HEIGHT);
        this.renderModuleTab(guiGraphics, x, y);
    }

    /**
     * The bit of panel the module slot sits on: a small window of its own, bordered on all four sides
     * and standing a few pixels clear of the crate panel.
     * A slot itself has no texture in Minecraft: it is the background that carries the 18 by 18 cell.
     */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            MODULE_TAB,
            x + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN,
            y + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN,
            0.0F,
            0.0F,
            MODULE_TAB_WIDTH,
            MODULE_TAB_HEIGHT,
            MODULE_TAB_TEXTURE,
            MODULE_TAB_TEXTURE
        );
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot, int i, int j) {
        ItemStack itemStack = slot.getItem();
        if (this.dims(slot)) {
            super.renderSlot(guiGraphics, slot, i, j);
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0101010);
            return;
        }

        if (slot instanceof DeepCrateSlot && itemStack.getCount() > ABBREVIATE_ABOVE) {
            guiGraphics.renderItem(itemStack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
            guiGraphics.renderItemDecorations(this.font, itemStack, slot.x, slot.y, abbreviate(itemStack.getCount()));
            return;
        }

        super.renderSlot(guiGraphics, slot, i, j);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int i, int j) {
        super.renderTooltip(guiGraphics, i, j);

        // The abbreviated count hides the real one, so the tooltip says it.
        if (this.hoveredSlot instanceof DeepCrateSlot && this.hoveredSlot.getItem().getCount() > ABBREVIATE_ABOVE && this.menu.getCarried().isEmpty()) {
            guiGraphics.setTooltipForNextFrame(
                this.font,
                Component.translatable("screen.deepcrate.count", this.hoveredSlot.getItem().getCount()),
                i,
                j + 12
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);

        // The page being read, as a bare number against the right edge of the title line.
        String page = String.valueOf(this.menu.page() + 1);
        guiGraphics.drawString(this.font, page, this.imageWidth - 7 - this.font.width(page), this.titleLabelY, 0xFF404040, false);
    }

    /**
     * Whether a slot is greyed out by the search. Only the crate's own slots answer: dimming the
     * player's inventory as well would leave nothing readable on screen.
     */
    private boolean dims(Slot slot) {
        if (this.query.isEmpty() || !(slot instanceof DeepCrateSlot) || slot.getItem().isEmpty()) {
            return false;
        }

        return !slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(this.query);
    }

    /** Tiles the one bare band of the texture over a height it does not natively cover. */
    private void fillBarePanel(GuiGraphics guiGraphics, int x, int y, int height) {
        int drawn = 0;
        while (drawn < height) {
            int slice = Math.min(BARE_PANEL_HEIGHT, height - drawn);
            blit(guiGraphics, x, y + drawn, 0, BARE_PANEL_V, this.imageWidth, slice);
            drawn += slice;
        }
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, u, v, width, height, 256, 256);
    }

    private static String abbreviate(int count) {
        if (count >= 1_000_000) {
            return count / 1_000_000 + "M";
        }

        return count / 1000 + "k";
    }
}
