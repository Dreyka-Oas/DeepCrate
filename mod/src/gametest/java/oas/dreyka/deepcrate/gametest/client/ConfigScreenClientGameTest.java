package oas.dreyka.deepcrate.gametest.client;

import oas.dreyka.deepcrate.client.screen.config.ConfigScreen;
import oas.dreyka.deepcrate.config.access.ConfigOption;
import oas.dreyka.deepcrate.config.access.ConfigRuntime;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Photographs the settings screen, because a line in the log saying it opened says nothing about what is
 * drawn on it. The screen is never built here: the command runs on the server, the server sends the
 * snapshot, and the client answers that packet by putting the screen up, which is the road a player takes.
 * Only what a picture cannot carry is then asserted, which is the class the client is really holding, a row
 * for every option the schema has, and a press here reaching the server's own table.
 */
public class ConfigScreenClientGameTest implements FabricClientGameTest {
    /** The glyph a reset button carries, one per row, a row being nameable from outside that package by nothing else. */
    private static final String RESET_GLYPH = "\u21BA";
    /** Matches an option of each category, by the label for two of them and by the field name for the third. */
    private static final String SEARCH = "at";

    @Override
    public void runTest(ClientGameTestContext context) {
        // Cheats on: the harness builds its world without them, and the packet carrying an edit asks the player for rights of his own.
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
            .adjustSettings(worldCreationUiState -> worldCreationUiState.setAllowCommands(true)).create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getClientWorld().waitForChunksRender();
            // A crate in the frame, so the shot taken after Escape cannot be read as a screen closing over nothing.
            server.runCommand("execute at @a run setblock ~ ~ ~-4 deepcrate:copper_crate[facing=south,type=single]");
            server.runCommand("execute at @a run tp @a ~ ~ ~ 180 5");
            context.getInput().resizeWindow(1920, 1080);
            context.runOnClient(minecraft -> minecraft.options.hideGui = true);
            singleplayer.getClientWorld().waitForChunksRender();
            context.waitTicks(40);
            List<ConfigOption> options = server.computeOnServer(minecraftServer -> ConfigRuntime.snapshot());
            // The harness runs a command as the console, which has no player and prints the table as text. Under an execute clause it is the player asking.
            server.runCommand("execute as @a run deepcrateconfig");
            context.waitTicks(40);
            if (!context.computeOnClient(minecraft -> minecraft.screen instanceof ConfigScreen)) {
                throw fail(context, "the settings command opened no screen of the mod");
            }

            context.takeScreenshot("config-screen-as-it-opens");
            int rows = rowCount(context);
            if (rows < options.size()) {
                throw fail(context, "the screen carries " + rows + " rows for the " + options.size() + " options the schema has");
            }

            // The toggle before the search, so nothing has to reach into the field to empty it again.
            ConfigOption flag = firstBoolean(context, options);
            press(context, CommonComponents.optionStatus(Boolean.parseBoolean(flag.value())));
            // An accepted change is answered with the whole table again, so the shot waits for that answer.
            context.waitTicks(20);
            context.takeScreenshot("config-screen-boolean-toggled");
            if (server.computeOnServer(minecraftServer -> ConfigRuntime.snapshot()).equals(options)) {
                throw fail(context, "the toggle of " + flag.name() + " was pressed and the server's table never moved");
            }
            // Options arrive grouped by holder, so the last is filed under the last category, never the one opened on.
            ConfigOption last = options.get(options.size() - 1);
            if (!last.category().equals(options.get(0).category())) {
                press(context, Component.translatable(last.categoryKey()));
                context.takeScreenshot("config-screen-one-category-chosen");
            }
            search(context);
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitTicks(20);
            if (context.computeOnClient(minecraft -> minecraft.screen != null)) {
                throw fail(context, "escape gave no world back");
            }
            context.takeScreenshot("config-screen-closed-by-escape");
        }
    }

    /** One glyph per row, hidden rows counted too: a row filed under another category stays registered with the screen. */
    private static int rowCount(ClientGameTestContext context) {
        return context.computeOnClient(minecraft -> {
            int found = 0;
            for (GuiEventListener guiEventListener : minecraft.screen.children()) {
                if (guiEventListener instanceof AbstractButton button && button.getMessage().getString().equals(RESET_GLYPH)) {
                    found++;
                }
            }
            return found;
        });
    }

    /** The focus is handed over by name, where a click would be aimed at pixels the width of the window decides. */
    private static void search(ClientGameTestContext context) {
        String hint = Component.translatable("screen.deepcrate.config.search").getString();
        context.runOnClient(minecraft -> {
            for (GuiEventListener guiEventListener : minecraft.screen.children()) {
                if (guiEventListener instanceof EditBox editBox && editBox.getMessage().getString().equals(hint)) {
                    minecraft.screen.setFocused(editBox);
                }
            }
        });
        context.getInput().typeChars(SEARCH);
        context.waitTicks(20);
        context.takeScreenshot("config-screen-search-across-categories");
    }

    /** Clicks with the real cursor, hit test included, and steps over the hidden button of a row filed elsewhere. */
    private static void press(ClientGameTestContext context, Component wanted) {
        double[] at = {-1.0, -1.0};
        context.runOnClient(minecraft -> {
            String message = wanted.getString();
            double scale = minecraft.getWindow().getGuiScale();
            for (GuiEventListener guiEventListener : minecraft.screen.children()) {
                if (guiEventListener instanceof AbstractButton button && button.visible && button.getMessage().getString().equals(message)) {
                    at[0] = (button.getX() + button.getWidth() / 2.0) * scale;
                    at[1] = (button.getY() + button.getHeight() / 2.0) * scale;
                }
            }
        });
        if (at[0] < 0.0) {
            throw fail(context, "no button on the settings screen carries " + wanted.getString());
        }
        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTicks(2);
        context.getInput().pressMouse(0);
        context.waitTicks(20);
    }

    /** Found by its type rather than named, so the boolean the mod ships can be renamed without touching this. */
    private static ConfigOption firstBoolean(ClientGameTestContext context, List<ConfigOption> options) {
        for (ConfigOption configOption : options) {
            if (configOption.kind() == ConfigPrimitive.BOOL) {
                return configOption;
            }
        }
        throw fail(context, "the schema ships no boolean option, so no toggle could be pressed");
    }

    /** No GameTestHelper reaches a client gametest, so what it reports is what it throws, screen and all. */
    private static AssertionError fail(ClientGameTestContext context, String message) {
        String held = context.computeOnClient(minecraft -> minecraft.screen == null ? "no screen" : minecraft.screen.getClass().getName());
        return new AssertionError("[DeepCrate] " + message + ", the client holding " + held);
    }
}
