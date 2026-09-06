package com.dreykaoas.deepcrate.gametest.client;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.client.screen.DeepCrateScreen;
import com.dreykaoas.deepcrate.client.screen.SearchBox;
import com.dreykaoas.deepcrate.client.screen.hook.CrateScreenArea;
import com.dreykaoas.deepcrate.client.screen.hook.CrateScreenCallback;
import com.dreykaoas.deepcrate.config.domain.CrateConfig;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.CrateOpenData;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import com.dreykaoas.deepcrate.inventory.slot.DeepCrateSlot;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One picture per case that changes what is drawn.
 *
 * A case already standing inside a frame taken for something else gets no frame of its own: the empty
 * module tab rides on the three-row crate, the tab holding row modules and nothing else rides on the
 * crate of six pages, and a page button wearing its dot stands beside one without in that same
 * picture. A count past four digits is what no shipped crate reaches, so it comes from a menu built
 * straight from an opening packet.
 *
 * The tab is never seen with both cells at once, because the paged crate of CrateLookClientGameTest
 * carries that already, down to the same six pages and the same dots. A grid of three or twelve
 * columns is built there too, from the same five numbers, so neither of those is taken again here.
 *
 * One class rather than two: the scene is built once and read straight down, where a second entry
 * point would create a second world and send the platform commands again for the same contact sheet.
 */
public class CrateShowcaseClientGameTest implements FabricClientGameTest {
    private static final BlockPos COPPER = new BlockPos(-2, 200, 24);
    private static final BlockPos ECHO = new BlockPos(2, 200, 24);
    /** Wider than the room the title has, which is what makes it stop before the page number. */
    private static final String ANVIL_NAME = "Everything the north quarry sent back";
    /** Any number: a menu built here never reaches the server, so nothing ever answers on it. */
    private static final int LOOSE_MENU_ID = 91;
    /**
     * Where the panel of the last crate screen stands, kept by the same event an addon would use.
     * A slot's x and y are read from that corner, and the screen keeps the corner to itself.
     */
    private static final AtomicReference<CrateScreenArea> LAST_AREA = new AtomicReference<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getClientWorld().waitForChunksRender();

            context.runOnClient(minecraft -> CrateScreenCallback.EVENT.register((screen, area) -> LAST_AREA.set(area)));
            buildTheScene(server);
            context.getInput().resizeWindow(1920, 1080);
            context.runOnClient(minecraft -> minecraft.options.hideGui = true);
            singleplayer.getClientWorld().waitForChunksRender();
            context.waitTicks(40);

            placeSingles(server);
            look(context, server, 1.0, 7.0, 9);
            context.takeScreenshot("01-one-crate-of-each-tier");

            placePairs(server);
            look(context, server, 0.5, 10.0, 6);
            context.takeScreenshot("02-one-pair-of-each-tier");

            // Down at the floor beside the two crates that carry the screens: the far row would
            // otherwise sit above the panel in every picture that follows.
            look(context, server, 0.5, 30.0, 45);
            fillTheCopper(server);
            open(context, server, COPPER);
            context.takeScreenshot("03-three-rows-on-one-page");

            press(context, "screen.deepcrate.sort.name");
            press(context, "screen.deepcrate.sort.count");
            // Off the buttons, or the one pressed last keeps its lit plate and its tooltip in the frame.
            context.getInput().setCursorPos(40.0, 40.0);
            context.waitTicks(10);
            context.takeScreenshot("04-sort-buttons-the-other-way");

            // A fresh screen before the search: a sort button keeps its direction on the screen that
            // owns it, so searching on this one would put the two reversed icons in a second frame.
            open(context, server, COPPER);
            search(context, "diamond");
            context.takeScreenshot("05-search-dims-what-it-does-not-match");

            showCapacity(context, server, 0, "06-a-slot-holding-128");
            showCapacity(context, server, 1, "07-a-slot-holding-256");
            showCapacity(context, server, 2, "08-a-slot-holding-512");

            onCrate(server, COPPER, copper -> {
                // The road a name typed on an anvil takes: it rides on the item, and the block entity
                // reads it off that stack as the crate is placed.
                ItemStack itemStack = new ItemStack(copper.getBlockState().getBlock());
                itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(ANVIL_NAME));
                copper.applyComponentsFromItemStack(itemStack);
            });
            open(context, server, COPPER);
            context.takeScreenshot("09-name-cut-before-the-page-number");

            fillTheEcho(server);
            open(context, server, ECHO);
            context.takeScreenshot("10-eight-rows-over-two-pages");

            onCrate(server, ECHO, echo -> {
                // Rows and nothing beside them. A capacity module in the other cell would draw the tab,
                // the six pages and the dots of the crate CrateLookClientGameTest already photographs.
                echo.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, CrateConfig.rowModuleStackLimit));
                // Twenty-four rows now, thirty-six slots to a page. Filling the third page and the sixth
                // leaves two, four and five empty, so one frame carries a button in both of its states.
                echo.storage().set(74, new ItemStack(Items.COPPER_INGOT, 64));
                echo.storage().set(190, new ItemStack(Items.PRISMARINE_CRYSTALS, 32));
            });
            open(context, server, ECHO);
            context.takeScreenshot("11-six-pages-in-two-columns");

            looseScreen(context, new CrateOpenData(27, 3, 1, 2_000_000, 9), "big counts", "12-counts-cut-to-k-and-m", deepCrateMenu -> {
                deepCrateMenu.getContainer().setItem(4, new ItemStack(Items.REDSTONE, 5_000));
                deepCrateMenu.getContainer().setItem(13, new ItemStack(Items.LAPIS_LAZULI, 2_400_000));
            });

            // The abbreviation hides the real count, so the tooltip has to carry it. That line is a
            // listener on CrateTooltipCallback like anyone else's, and nothing else would notice if it
            // stopped being registered.
            hover(context, Items.LAPIS_LAZULI, "13-the-real-count-under-an-abbreviated-one");

            context.setScreen(() -> null);
            context.waitTicks(10);
        }
    }

    private static void buildTheScene(TestServerContext server) {
        // 1.21.11 spells its gamerules with underscores; the old names are refused outright.
        server.runCommand("gamerule advance_time false");
        server.runCommand("gamerule advance_weather false");
        server.runCommand("gamerule spawn_mobs false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode creative @a");
        server.runCommand("tp @a 0.5 205.0 8.0");
        server.runCommand("fill -14 196 -2 14 214 32 air");
        server.runCommand("fill -14 199 -2 14 199 32 minecraft:smooth_stone");
        // Far enough down the platform to stay out of every world shot, close enough to open.
        server.runCommand("setblock -2 200 24 deepcrate:copper_crate[facing=south,type=single]");
        server.runCommand("setblock 2 200 24 deepcrate:echo_crate[facing=south,type=single]");
    }

    /** One crate of every tier, two apart so no two of them marry, and a chest of the game to read them against. */
    private static void placeSingles(TestServerContext server) {
        for (int i = 0; i < RegistryInit.TIERS.size(); i++) {
            server.runCommand("setblock %d 200 0 %s[facing=south,type=single]".formatted(-5 + i * 2, RegistryInit.TIERS.get(i).id()));
        }

        server.runCommand("setblock 7 200 0 minecraft:chest[facing=south,type=single]");
    }

    /** The same six as pairs: a half is a texture of its own, and neither half is the single. */
    private static void placePairs(TestServerContext server) {
        server.runCommand("fill -12 200 0 12 200 0 air");
        for (int i = 0; i < RegistryInit.TIERS.size(); i++) {
            // A pair is two halves: facing south, a right half's partner sits to its east.
            server.runCommand("setblock %d 200 0 %s[facing=south,type=right]".formatted(-9 + i * 3, RegistryInit.TIERS.get(i).id()));
            server.runCommand("setblock %d 200 0 %s[facing=south,type=left]".formatted(-8 + i * 3, RegistryInit.TIERS.get(i).id()));
        }

        server.runCommand("setblock 9 200 0 minecraft:chest[facing=south,type=right]");
        server.runCommand("setblock 10 200 0 minecraft:chest[facing=south,type=left]");
    }

    /** Several kinds at once, which is what a search has to tell apart and a sort has to reorder. */
    private static void fillTheCopper(TestServerContext server) {
        onCrate(server, COPPER, copper -> {
            copper.storage().set(0, new ItemStack(Items.STONE, 64));
            copper.storage().set(4, new ItemStack(Items.DIAMOND, 12));
            copper.storage().set(7, new ItemStack(Items.OAK_LOG, 40));
            copper.storage().set(11, new ItemStack(Items.COAL, 33));
            copper.storage().set(16, new ItemStack(Items.DIAMOND_BLOCK, 5));
            copper.storage().set(20, new ItemStack(Items.DIRT, 51));
            copper.storage().set(23, new ItemStack(Items.COPPER_INGOT, 28));
            copper.storage().set(26, new ItemStack(Items.GOLD_INGOT, 9));
        });
    }

    /** All of it on the first page, so the second page button stands there with nothing to show. */
    private static void fillTheEcho(TestServerContext server) {
        onCrate(server, ECHO, echo -> {
            echo.storage().set(0, new ItemStack(Items.LAPIS_LAZULI, 64));
            echo.storage().set(13, new ItemStack(Items.ECHO_SHARD, 20));
            echo.storage().set(31, new ItemStack(Items.AMETHYST_SHARD, 48));
        });
    }

    /** @param moduleIndex where the module sits in RegistryInit.MODULE_ITEMS, weakest first */
    private static void showCapacity(ClientGameTestContext context, TestServerContext server, int moduleIndex, String shot) {
        onCrate(server, COPPER, copper -> {
            ItemStack module = new ItemStack(RegistryInit.MODULE_ITEMS.get(moduleIndex));
            copper.setModule(module);
            // Filled to exactly what that module allows, which is the number the slot then carries.
            copper.storage().set(13, new ItemStack(Items.REDSTONE, DeepCrateApi.capacityOf(module)));
        });
        open(context, server, COPPER);
        context.takeScreenshot(shot);
    }

    private static void onCrate(TestServerContext server, BlockPos blockPos, Consumer<DeepCrateBlockEntity> action) {
        server.runOnServer(minecraftServer -> {
            if (minecraftServer.overworld().getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
                action.accept(deepCrateBlockEntity);
                deepCrateBlockEntity.setChanged();
            }
        });
    }

    /**
     * Closes whatever is up before asking for the next crate: waiting for a DeepCrateScreen that is
     * already on screen returns at once, and the picture would then be of the crate before this one.
     */
    private static void open(ClientGameTestContext context, TestServerContext server, BlockPos blockPos) {
        context.setScreen(() -> null);
        context.waitTicks(10);
        server.runOnServer(minecraftServer -> {
            ServerPlayer serverPlayer = minecraftServer.getPlayerList().getPlayers().get(0);
            if (minecraftServer.overworld().getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
                serverPlayer.openMenu(deepCrateBlockEntity);
            }
        });
        context.waitForScreen(DeepCrateScreen.class);
        context.waitTicks(20);
    }

    /**
     * A screen built straight from an opening packet, for what no shipped crate can be made to draw.
     * Everything the panel shows comes from that packet, which is what makes the picture worth taking.
     */
    private static void looseScreen(
        ClientGameTestContext context, CrateOpenData crateOpenData, String title, String shot, Consumer<DeepCrateMenu> fill
    ) {
        context.runOnClient(minecraft -> {
            DeepCrateMenu deepCrateMenu = new DeepCrateMenu(LOOSE_MENU_ID, minecraft.player.getInventory(), crateOpenData);
            fill.accept(deepCrateMenu);
            minecraft.setScreen(new DeepCrateScreen(deepCrateMenu, minecraft.player.getInventory(), Component.literal(title)));
        });
        context.waitTicks(20);
        context.takeScreenshot(shot);
    }

    /**
     * Puts the pointer on the crate slot holding that item and photographs what comes up.
     *
     * The slot is found by what it carries rather than by an index, because the crate slots and the
     * player's share one list and their order is the menu's business, not this test's.
     */
    private static void hover(ClientGameTestContext context, Item item, String shot) {
        double[] at = {-1.0, -1.0};
        context.runOnClient(minecraft -> {
            CrateScreenArea crateScreenArea = LAST_AREA.get();
            if (!(minecraft.screen instanceof DeepCrateScreen deepCrateScreen) || crateScreenArea == null) {
                return;
            }

            // A slot's own x and y are read from the panel's corner, which is what the area carries.
            double scale = minecraft.getWindow().getGuiScale();
            for (Slot slot : deepCrateScreen.getMenu().slots) {
                if (slot instanceof DeepCrateSlot && slot.getItem().is(item)) {
                    at[0] = (crateScreenArea.left() + slot.x + 8) * scale;
                    at[1] = (crateScreenArea.top() + slot.y + 8) * scale;
                }
            }
        });

        if (at[0] < 0.0) {
            DeepCrate.LOGGER.warn("[DeepCrate] no crate slot holds {} on this screen, shot skipped", item);
            return;
        }

        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTicks(20);
        context.takeScreenshot(shot);
    }

    /**
     * Hands the search field the focus and types into it. A click would have to be aimed at pixels
     * whose place follows the width of the word "Inventory" in the language the client is reading.
     */
    private static void search(ClientGameTestContext context, String query) {
        context.runOnClient(minecraft -> {
            if (minecraft.screen == null) {
                return;
            }

            for (GuiEventListener guiEventListener : minecraft.screen.children()) {
                if (guiEventListener instanceof SearchBox searchBox) {
                    minecraft.screen.setFocused(searchBox);
                }
            }
        });

        context.getInput().typeChars(query);
        context.waitTicks(10);
    }

    /**
     * Clicks a button with the real cursor. The framework has a press of its own, but it only knows
     * Button and CycleButton, and both of ours are plain AbstractButtons it walks straight past.
     */
    private static void press(ClientGameTestContext context, String translationKey) {
        double[] at = {-1.0, -1.0};
        context.runOnClient(minecraft -> {
            if (minecraft.screen == null) {
                return;
            }

            String wanted = Component.translatable(translationKey).getString();
            double scale = minecraft.getWindow().getGuiScale();
            for (GuiEventListener guiEventListener : minecraft.screen.children()) {
                if (guiEventListener instanceof AbstractButton button && button.getMessage().getString().equals(wanted)) {
                    at[0] = (button.getX() + button.getWidth() / 2.0) * scale;
                    at[1] = (button.getY() + button.getHeight() / 2.0) * scale;
                }
            }
        });

        if (at[0] < 0.0) {
            DeepCrate.LOGGER.warn("[DeepCrate] no button carries {} on this screen, shot skipped", translationKey);
            return;
        }

        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTicks(2);
        context.getInput().pressMouse(0);
        context.waitTicks(20);
    }

    /**
     * Stands the player on the platform and aims the camera by angle, yaw 180 being north, at the face
     * of a crate placed facing south. Angles rather than a target point: the facing clause of the
     * teleport command leaves the pitch where it was, and half the shots came back looking at the sky.
     */
    private static void look(ClientGameTestContext context, TestServerContext server, double x, double z, int pitch) {
        server.runCommand("tp @a %s 200.0 %s 180 %d".formatted(x, z, pitch));
        context.waitTicks(20);
    }
}
