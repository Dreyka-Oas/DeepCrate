package oas.dreyka.deepcrate.gametest.client;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import oas.dreyka.deepcrate.init.RegistryInit;

/**
 * A real crafting table, the same nine-cell grid a player fills by hand, once per recipe the mod
 * ships. Every crate tier and every module reads as a 3x3 border around one center item, so the
 * grid is set directly on the server-side menu rather than dragged there stack by stack.
 */
public class CraftRecipesClientGameTest implements FabricClientGameTest {
    private static final BlockPos TABLE = new BlockPos(0, 200, 0);
    /** The border fills the eight ring cells, the center is index 5 of the crafting grid's nine slots. */
    private static final int CENTER_SLOT = 5;

    private record Recipe(String shot, Item border, Item center) {}

    private static List<Recipe> recipes() {
        return List.of(
            new Recipe("craft-coal_crate", Items.COAL_BLOCK, Items.CHEST),
            new Recipe("craft-copper_crate", Items.COPPER_BLOCK, RegistryInit.TIERS.get(0).block().asItem()),
            new Recipe("craft-iron_crate", Items.IRON_BLOCK, RegistryInit.TIERS.get(1).block().asItem()),
            new Recipe("craft-redstone_crate", Items.REDSTONE_BLOCK, RegistryInit.TIERS.get(2).block().asItem()),
            new Recipe("craft-lapis_crate", Items.LAPIS_BLOCK, RegistryInit.TIERS.get(3).block().asItem()),
            new Recipe("craft-gold_crate", Items.GOLD_BLOCK, RegistryInit.TIERS.get(4).block().asItem()),
            new Recipe("craft-amethyst_crate", Items.AMETHYST_BLOCK, RegistryInit.TIERS.get(5).block().asItem()),
            new Recipe("craft-quartz_crate", Items.QUARTZ_BLOCK, RegistryInit.TIERS.get(6).block().asItem()),
            new Recipe("craft-emerald_crate", Items.EMERALD_BLOCK, RegistryInit.TIERS.get(7).block().asItem()),
            new Recipe("craft-diamond_crate", Items.DIAMOND_BLOCK, RegistryInit.TIERS.get(8).block().asItem()),
            new Recipe("craft-netherite_crate", Items.NETHERITE_BLOCK, RegistryInit.TIERS.get(9).block().asItem()),
            new Recipe("craft-module_128", Items.COPPER_INGOT, Items.AMETHYST_SHARD),
            new Recipe("craft-module_256", Items.LAPIS_LAZULI, RegistryInit.MODULE_ITEMS.get(0)),
            new Recipe("craft-module_512", Items.REDSTONE, RegistryInit.MODULE_ITEMS.get(1)),
            new Recipe("craft-module_row", Items.OAK_PLANKS, Items.CHEST)
        );
    }

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

            for (Recipe recipe : recipes()) {
                craft(context, server, recipe);
            }

            context.setScreen(() -> null);
            context.waitTicks(10);
        }
    }

    private static void buildTheScene(TestServerContext server) {
        server.runCommand("gamerule advance_time false");
        server.runCommand("gamerule advance_weather false");
        server.runCommand("gamerule spawn_mobs false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode creative @a");
        server.runCommand("tp @a 0.5 205.0 8.0 180 10");
        server.runCommand("fill -14 196 -2 14 214 32 air");
        server.runCommand("fill -14 199 -2 14 199 32 minecraft:smooth_stone");
        server.runCommand("setblock 0 200 0 minecraft:crafting_table");
    }

    /**
     * Closes whatever is up, opens a fresh crafting table menu, fills its nine-cell grid, and forces
     * the result to recompute. slotsChanged(Container) never reads its argument on CraftingMenu, so
     * null stands in for the container it would otherwise expect.
     */
    private static void craft(ClientGameTestContext context, TestServerContext server, Recipe recipe) {
        context.setScreen(() -> null);
        context.waitTicks(10);
        server.runOnServer(minecraftServer -> {
            ServerPlayer serverPlayer = minecraftServer.getPlayerList().getPlayers().get(0);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, player) -> new CraftingMenu(id, inventory, ContainerLevelAccess.create(minecraftServer.overworld(), TABLE)),
                Component.translatable("container.crafting")
            ));

            if (serverPlayer.containerMenu instanceof CraftingMenu craftingMenu) {
                for (int i = 1; i <= 9; i++) {
                    craftingMenu.getSlot(i).set(new ItemStack(i == CENTER_SLOT ? recipe.center() : recipe.border()));
                }

                craftingMenu.slotsChanged(null);
            }
        });

        context.waitForScreen(CraftingScreen.class);
        context.waitTicks(20);
        // Off any slot, or the hotbar item a previous case left the cursor on keeps its tooltip in the frame.
        context.getInput().setCursorPos(40.0, 40.0);
        context.waitTicks(2);
        context.takeScreenshot(recipe.shot());
    }
}
