package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The settings screen: a search field across the top, the categories down the left, one row per
 * option on the right.
 *
 * It is filled from the snapshot the server sent and from nothing else, which is what makes it read
 * the truth in a solo world, on a world opened to the local network and on a dedicated server alike.
 * There is no save button and no cancel button either: every toggle, every reset and every edit
 * leaves as it happens, so the file on the server is what the screen shows at all times.
 */
public final class ConfigScreen extends Screen {
    private static final int MARGIN = 20;
    private static final int TITLE_Y = 14;
    private static final int SEARCH_Y = 28;
    private static final int SEARCH_HEIGHT = 16;
    private static final int CATEGORY_WIDTH = 90;
    private static final int CATEGORY_GAP = 2;
    private static final int LIST_Y = 54;
    private static final int FOOT_MARGIN = 12;
    private static final int GAP = 8;
    private static final int TITLE_COLOUR = 0xFFFFFFFF;

    private final ConfigEdits edits = new ConfigEdits();
    private final ConfigRows configRows;
    private final Map<String, ConfigButton> categoryButtons = new LinkedHashMap<>();

    private String query = "";

    public ConfigScreen(List<ConfigOption> options) {
        super(Component.translatable("screen.deepcrate.config.title"));
        this.configRows = new ConfigRows(this.font, options, this.edits);
    }

    @Override
    protected void init() {
        EditBox searchBox = new EditBox(
            this.font, MARGIN, SEARCH_Y, this.width - MARGIN * 2, SEARCH_HEIGHT, Component.translatable("screen.deepcrate.config.search")
        );
        searchBox.setHint(Component.translatable("screen.deepcrate.config.search"));
        searchBox.setMaxLength(48);
        // A resized window builds the screen again, and what was typed has to land in the new field.
        // Before the responder, so putting it back is not read as a fresh search.
        searchBox.setValue(this.query);
        searchBox.setResponder(this::onSearch);
        this.addRenderableWidget(searchBox);

        this.categoryButtons.clear();
        int y = LIST_Y;
        for (Map.Entry<String, Component> entry : this.configRows.categories().entrySet()) {
            String category = entry.getKey();
            ConfigButton configButton = new ConfigButton(
                MARGIN, y, CATEGORY_WIDTH, ConfigButton.HEIGHT, entry.getValue(), () -> this.choose(category)
            );
            this.categoryButtons.put(category, this.addRenderableWidget(configButton));
            y += ConfigButton.HEIGHT + CATEGORY_GAP;
        }

        // The same widgets as before the rebuild, so a half-typed value survives a resized window.
        this.configRows.register(this::addRenderableWidget);
        this.refresh();
    }

    /** Whatever the server says now, taken by the rows that are not holding an edit of their own. */
    public void applySnapshot(List<ConfigOption> options) {
        this.configRows.accept(options);
    }

    private void choose(String category) {
        this.configRows.select(category);
        this.refresh();
    }

    private void onSearch(String string) {
        this.query = string;
        this.configRows.search(string);
        this.refresh();
    }

    /** Run whenever what is on screen may have moved: a category chosen, a search typed, a wheel turned. */
    private void refresh() {
        boolean searching = !this.query.isEmpty();
        for (Map.Entry<String, ConfigButton> entry : this.categoryButtons.entrySet()) {
            entry.getValue().active = searching || !entry.getKey().equals(this.configRows.category());
        }

        int left = MARGIN + CATEGORY_WIDTH + GAP;
        this.configRows.layout(left, LIST_Y, this.width - MARGIN - left, this.height - LIST_Y - FOOT_MARGIN);
        // A field the layout has just hidden would keep the keyboard and swallow every key typed after.
        if (this.getFocused() instanceof AbstractWidget abstractWidget && !abstractWidget.visible) {
            this.clearFocus();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, TITLE_Y, TITLE_COLOUR, false);
        // After the widgets, so a description read off a row still lands over them.
        this.configRows.render(guiGraphics, i, j);
    }

    @Override
    public void tick() {
        super.tick();
        this.edits.tick();
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (g != 0.0 && this.configRows.scroll(g > 0.0 ? -1 : 1)) {
            this.refresh();
            return true;
        }

        return super.mouseScrolled(d, e, f, g);
    }

    /**
     * Nothing here is saved by a button, so what the wait still holds has to leave before the screen
     * does. A value typed a moment before Escape would otherwise die with it.
     */
    @Override
    public void onClose() {
        this.edits.flush();
        super.onClose();
    }
}
