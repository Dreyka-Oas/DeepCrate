package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.client.DeepCrateScreen;
import com.dreykaoas.deepcrate.init.RegistryInit;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Photographs the two things a headless server cannot answer for: what a crate looks like in the
 * world, and what its screen looks like.
 *
 * The scene is built with commands rather than left to the world generator, so the shots are the
 * same every run: a stone platform in cleared air, noon, no weather, no HUD. Nothing is asserted
 * here beyond the buttons being reachable; the pictures are for a person to look at.
 */
public class CrateLookClientGameTest implements FabricClientGameTest {
    private static final BlockPos SINGLE = new BlockPos(-2, 200, 0);
    private static final BlockPos PAGED = new BlockPos(-5, 200, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getClientWorld().waitForChunksRender();

            buildTheScene(server);
            context.getInput().resizeWindow(1920, 1080);
            context.runOnClient(minecraft -> minecraft.options.hideGui = true);
            singleplayer.getClientWorld().waitForChunksRender();
            context.waitTicks(40);

            look(context, server, 0.0, 8.0, 180, 12);
            context.takeScreenshot("crates-in-the-world");

            look(context, server, 3.5, 3.4, 180, 28);
            context.takeScreenshot("double-crate-beside-a-chest");

            look(context, server, -1.5, 2.6, 180, 34);
            context.takeScreenshot("single-crate-close");

            look(context, server, 2.0, 2.8, 180, 31);
            context.takeScreenshot("double-crate-close");

            fillTheCrate(server);
            openTheCrate(server, SINGLE);
            context.waitForScreen(DeepCrateScreen.class);
            context.waitTicks(20);
            context.takeScreenshot("screen-before-sorting");

            press(context, "screen.deepcrate.sort.name");
            context.takeScreenshot("screen-sorted-by-name");

            press(context, "screen.deepcrate.sort.count");
            context.takeScreenshot("screen-sorted-by-count");

            context.setScreen(() -> null);
            context.waitTicks(10);

            openTheCrate(server, PAGED);
            context.waitForScreen(DeepCrateScreen.class);
            context.waitTicks(20);
            context.takeScreenshot("screen-paged-crate");

            context.setScreen(() -> null);
            context.waitTicks(20);
            look(context, server, -1.5, 2.6, 180, 34);
            context.takeScreenshot("single-crate-after-sorting");
        }
    }

    /**
     * Stands the player at a spot on the platform and aims the camera by angle.
     *
     * Angles rather than a target point: the facing clause of the teleport command leaves the pitch
     * where it was often enough that half the shots came back looking at the sky.
     */
    private static void look(ClientGameTestContext context, TestServerContext server, double x, double z, int yaw, int pitch) {
        server.runCommand("tp @a %s 200.0 %s %d %d".formatted(x, z, yaw, pitch));
        context.waitTicks(20);
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
        server.runCommand("fill -10 196 -8 10 214 8 air");
        server.runCommand("fill -10 199 -8 10 199 8 minecraft:smooth_stone");
        server.runCommand("setblock -5 200 0 deepcrate:echo_crate[facing=south,type=single]");
        server.runCommand("setblock -2 200 0 deepcrate:copper_crate[facing=south,type=single]");
        // A pair is two halves: facing south, a right half's partner sits to its east.
        server.runCommand("setblock 1 200 0 deepcrate:iron_crate[facing=south,type=right]");
        server.runCommand("setblock 2 200 0 deepcrate:iron_crate[facing=south,type=left]");
        // A chest of the game beside ours, as the control every shot is read against.
        server.runCommand("setblock 4 200 0 minecraft:chest[facing=south,type=right]");
        server.runCommand("setblock 5 200 0 minecraft:chest[facing=south,type=left]");
    }

    private static void fillTheCrate(TestServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerLevel serverLevel = minecraftServer.overworld();
            if (serverLevel.getBlockEntity(SINGLE) instanceof DeepCrateBlockEntity copper) {
                copper.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
                copper.storage().set(2, new ItemStack(Items.STONE, 320));
                copper.storage().set(7, new ItemStack(Items.DIRT, 190));
                copper.storage().set(11, new ItemStack(Items.OAK_LOG, 64));
                copper.storage().set(14, new ItemStack(Items.DIRT, 240));
                copper.storage().set(19, new ItemStack(Items.DIAMOND, 12));
                copper.storage().set(23, new ItemStack(Items.STONE, 128));
                copper.storage().set(25, new ItemStack(Items.COAL, 45));
                copper.setChanged();
            }

            // Eight rows of its own plus sixteen from the modules: twenty-four rows, six pages.
            if (serverLevel.getBlockEntity(PAGED) instanceof DeepCrateBlockEntity echo) {
                echo.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
                echo.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 16));
                echo.storage().set(0, new ItemStack(Items.AMETHYST_SHARD, 512));
                echo.storage().set(5, new ItemStack(Items.ECHO_SHARD, 300));
                echo.setChanged();
            }
        });
    }

    private static void openTheCrate(TestServerContext server, BlockPos blockPos) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer serverPlayer = minecraftServer.getPlayerList().getPlayers().get(0);
            if (minecraftServer.overworld().getBlockEntity(blockPos) instanceof DeepCrateBlockEntity deepCrateBlockEntity) {
                serverPlayer.openMenu(deepCrateBlockEntity);
            }
        });
    }

    /**
     * Clicks a button with the real cursor rather than by reaching into the screen, so the click
     * travels the path a player's does, hit test included.
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
}
