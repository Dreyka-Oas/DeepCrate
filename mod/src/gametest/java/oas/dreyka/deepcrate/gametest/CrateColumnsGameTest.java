package oas.dreyka.deepcrate.gametest;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A crate is as wide as its tier says, and the six shipped tiers say nine. */
public class CrateColumnsGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);

    @GameTest
    public void theShippedTiersAreNineWide(GameTestHelper gameTestHelper) {
        for (CrateTier crateTier : RegistryInit.TIERS) {
            if (crateTier.columns() != CrateTier.DEFAULT_COLUMNS) {
                gameTestHelper.fail(crateTier.id() + " should be nine wide, it is " + crateTier.columns());
            }
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void theMenuTellsTheClientHowWideTheCrateIs(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        if (deepCrateMenu.columns() != CrateTier.DEFAULT_COLUMNS) {
            gameTestHelper.fail("the menu should say nine columns, it says " + deepCrateMenu.columns());
        }

        if (deepCrateBlockEntity.getScreenOpeningData(serverPlayer).columns() != CrateTier.DEFAULT_COLUMNS) {
            gameTestHelper.fail("the opening packet should carry nine columns");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void rowsAddAsManySlotsAsTheTierIsWide(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        CrateTier crateTier = RegistryInit.TIERS.get(0);

        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 2));

        int expected = crateTier.slotCount() + 2 * crateTier.columns();
        if (deepCrateBlockEntity.storage().size() != expected) {
            gameTestHelper.fail("two row modules should give " + expected + " slots, the crate has " + deepCrateBlockEntity.storage().size());
        }

        gameTestHelper.succeed();
    }
}
