package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * Every row of the screen, and which of them the player is looking at.
 *
 * The rows are walked out of the snapshot rather than named one by one, so an option added to the
 * schema turns up here with nothing wired anywhere. A row filed under another category, or left out
 * by a search, or below the last line that fits, is hidden rather than dropped: its widgets stay
 * registered with the screen, and a hidden widget answers neither a click nor a key.
 */
final class ConfigRows {
    private final List<ConfigRow> rows = new ArrayList<>();
    private final Map<String, Component> categories = new LinkedHashMap<>();
    /** The rows the filters let through, in schema order, which is what the wheel walks. */
    private final List<ConfigRow> kept = new ArrayList<>();

    private String category;
    private String query = "";
    private int first;
    private int room = 1;

    ConfigRows(Font font, List<ConfigOption> options, ConfigEdits configEdits) {
        for (ConfigOption configOption : options) {
            this.rows.add(new ConfigRow(font, configOption, configEdits));
            this.categories.putIfAbsent(configOption.category(), Component.translatable(configOption.categoryKey()));
        }

        this.category = this.categories.isEmpty() ? "" : this.categories.keySet().iterator().next();
    }

    Map<String, Component> categories() {
        return this.categories;
    }

    String category() {
        return this.category;
    }

    void select(String category) {
        this.category = category;
        this.first = 0;
    }

    void search(String query) {
        this.query = query.toLowerCase(Locale.ROOT).trim();
        this.first = 0;
    }

    /** True when the wheel had somewhere to go, which is what tells the screen to lay out again. */
    boolean scroll(int by) {
        int wanted = Math.max(0, Math.min(this.first + by, this.kept.size() - this.room));
        if (wanted == this.first) {
            return false;
        }

        this.first = wanted;
        return true;
    }

    void layout(int x, int y, int width, int height) {
        this.kept.clear();
        for (ConfigRow configRow : this.rows) {
            if (this.keeps(configRow)) {
                this.kept.add(configRow);
            }

            configRow.show(false);
        }

        this.room = Math.max(1, height / ConfigRow.HEIGHT);
        this.first = Math.max(0, Math.min(this.first, this.kept.size() - this.room));
        for (int i = this.first; i < Math.min(this.kept.size(), this.first + this.room); i++) {
            ConfigRow configRow = this.kept.get(i);
            configRow.show(true);
            configRow.place(x, y + (i - this.first) * ConfigRow.HEIGHT, width);
        }
    }

    /**
     * While something is searched the categories stand aside. Somebody looking for an option by name
     * does not know which category it was filed under, and a search that only read one of them would
     * answer that the option does not exist.
     */
    private boolean keeps(ConfigRow configRow) {
        return this.query.isEmpty() ? configRow.category().equals(this.category) : configRow.matches(this.query);
    }

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (ConfigRow configRow : this.rows) {
            configRow.render(guiGraphics, mouseX, mouseY);
        }
    }

    void register(Consumer<AbstractWidget> sink) {
        for (ConfigRow configRow : this.rows) {
            configRow.register(sink);
        }
    }

    void accept(List<ConfigOption> options) {
        for (ConfigOption configOption : options) {
            for (ConfigRow configRow : this.rows) {
                if (configRow.name().equals(configOption.name())) {
                    configRow.accept(configOption);
                }
            }
        }
    }
}
