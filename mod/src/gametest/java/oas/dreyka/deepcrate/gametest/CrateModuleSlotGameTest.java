package oas.dreyka.deepcrate.gametest;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The cells of the module tab, now that there is a registry behind them rather than two fields. */
public class CrateModuleSlotGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);

    @GameTest
    public void theMenuOpensOneSlotPerRegisteredKind(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        int kinds = DeepCrateApi.moduleSlots().size();
        if (deepCrateMenu.crateSlotStart() != kinds) {
            gameTestHelper.fail(
                "the crate slots should start after the " + kinds + " module cells, they start at " + deepCrateMenu.crateSlotStart()
            );
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void aCapacityModuleRaisesTheCrateFromWhicheverCellItSitsIn(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        // The rows cell, not the capacity one: which cell an item sits in no longer says what it does.
        deepCrateBlockEntity.setModuleIn(RegistryInit.ROWS_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));

        if (deepCrateBlockEntity.storage().capacity() != 512) {
            gameTestHelper.fail("the crate should hold 512 a slot, it holds " + deepCrateBlockEntity.storage().capacity());
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void theStrongestOfTwoModulesWins(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        deepCrateBlockEntity.setModuleIn(RegistryInit.CAPACITY_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateBlockEntity.setModuleIn(RegistryInit.ROWS_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));

        if (deepCrateBlockEntity.storage().capacity() != 512) {
            gameTestHelper.fail("the better of 512 and 128 should win, the crate holds " + deepCrateBlockEntity.storage().capacity());
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void aCellNobodyRegisteredKeepsItsStackRatherThanLosingIt(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        Identifier stranger = Identifier.fromNamespaceAndPath("somemod", "filter");

        deepCrateBlockEntity.setModuleIn(stranger, new ItemStack(Items.HOPPER));

        if (deepCrateBlockEntity.modules().get(stranger).isEmpty()) {
            gameTestHelper.fail("a stack in an unregistered cell should stay in the table");
        }

        if (DeepCrateApi.moduleSlot(stranger) != null) {
            gameTestHelper.fail("nobody registered that cell, the api should not know it");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void anAddonCellTakesWhatItsFilterAllowsAndNothingElse(GameTestHelper gameTestHelper) {
        CrateModuleSlot crateModuleSlot = new CrateModuleSlot(
            Identifier.fromNamespaceAndPath("somemod", "hoppers"),
            9,
            4,
            RegistryInit.id("container/slot/module"),
            itemStack -> itemStack.is(Items.HOPPER)
        );

        if (!crateModuleSlot.accepts(new ItemStack(Items.HOPPER)) || crateModuleSlot.accepts(new ItemStack(Items.STONE))) {
            gameTestHelper.fail("the cell should take a hopper and refuse a stone");
        }

        if (crateModuleSlot.accepts(ItemStack.EMPTY)) {
            gameTestHelper.fail("no cell takes an empty stack");
        }

        gameTestHelper.succeed();
    }
}
