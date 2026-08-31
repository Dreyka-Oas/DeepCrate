package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.init.RegistryInit;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/**
 * The crafting grid, asked of the game itself rather than read off the JSON. Each recipe is laid out
 * as a player would lay it out, and the result is compared with what the tier should give.
 */
public class CrateRecipeGameTest {
    @GameTest
    public void everyCrateHasAWorkingRecipe(GameTestHelper gameTestHelper) {
        ServerLevel serverLevel = gameTestHelper.getLevel();

        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.COPPER_INGOT), new ItemStack(Blocks.COPPER_CHEST), RegistryInit.TIERS.get(0));
        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.IRON_INGOT), crate(RegistryInit.TIERS.get(0)), RegistryInit.TIERS.get(1));
        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.AMETHYST_SHARD), crate(RegistryInit.TIERS.get(1)), RegistryInit.TIERS.get(2));
        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.PRISMARINE_CRYSTALS), crate(RegistryInit.TIERS.get(2)), RegistryInit.TIERS.get(3));
        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.BREEZE_ROD), crate(RegistryInit.TIERS.get(3)), RegistryInit.TIERS.get(4));
        assertCrafts(gameTestHelper, serverLevel, new ItemStack(Items.ECHO_SHARD), crate(RegistryInit.TIERS.get(4)), RegistryInit.TIERS.get(5));

        gameTestHelper.succeed();
    }

    @GameTest
    public void everyModuleHasAWorkingRecipe(GameTestHelper gameTestHelper) {
        ServerLevel serverLevel = gameTestHelper.getLevel();

        assertCraftsInto(gameTestHelper, serverLevel, new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.AMETHYST_SHARD), RegistryInit.MODULE_ITEMS.get(0));
        assertCraftsInto(
            gameTestHelper, serverLevel, new ItemStack(Items.LAPIS_LAZULI), new ItemStack(RegistryInit.MODULE_ITEMS.get(0)), RegistryInit.MODULE_ITEMS.get(1)
        );
        assertCraftsInto(
            gameTestHelper, serverLevel, new ItemStack(Items.REDSTONE), new ItemStack(RegistryInit.MODULE_ITEMS.get(1)), RegistryInit.MODULE_ITEMS.get(2)
        );
        assertCraftsInto(
            gameTestHelper, serverLevel, new ItemStack(Items.QUARTZ), new ItemStack(RegistryInit.MODULE_ITEMS.get(2)), RegistryInit.MODULE_ITEMS.get(3)
        );

        gameTestHelper.succeed();
    }

    @GameTest
    public void anAxeIsTheRightToolForACrate(GameTestHelper gameTestHelper) {
        for (CrateTier crateTier : RegistryInit.TIERS) {
            gameTestHelper.setBlock(new BlockPos(1, 1, 1), crateTier.block());
            gameTestHelper.assertBlockTag(BlockTags.MINEABLE_WITH_AXE, new BlockPos(1, 1, 1));
        }

        gameTestHelper.succeed();
    }

    private static ItemStack crate(CrateTier crateTier) {
        return new ItemStack(crateTier.block());
    }

    private static void assertCrafts(GameTestHelper gameTestHelper, ServerLevel serverLevel, ItemStack ring, ItemStack core, CrateTier expected) {
        ItemStack result = craft(serverLevel, ring, core);
        if (!result.is(expected.block().asItem())) {
            gameTestHelper.fail("the grid for " + expected.id() + " gave " + result);
        }
    }

    private static void assertCraftsInto(
        GameTestHelper gameTestHelper, ServerLevel serverLevel, ItemStack ring, ItemStack core, net.minecraft.world.item.Item expected
    ) {
        ItemStack result = craft(serverLevel, ring, core);
        if (!result.is(expected)) {
            gameTestHelper.fail("the grid for " + expected + " gave " + result);
        }
    }

    /** Eight of one item around one of another, the shape every crate and module recipe uses. */
    private static ItemStack craft(ServerLevel serverLevel, ItemStack ring, ItemStack core) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(i == 4 ? core.copy() : ring.copy());
        }

        CraftingInput craftingInput = CraftingInput.of(3, 3, grid);
        return serverLevel.recipeAccess()
            .getRecipeFor(RecipeType.CRAFTING, craftingInput, serverLevel)
            .map(holder -> holder.value().assemble(craftingInput, serverLevel.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }
}
