package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Row modules: they add rows, they add them to both halves of a pair, and taking them back spills. */
public class CrateRowModuleGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);
    private static final int COPPER_SLOTS = 27;

    @GameTest
    public void eachRowModuleAddsNineSlots(GameTestHelper gameTestHelper) {
        DeepCrateBlockEntity deepCrateBlockEntity = placeCrate(gameTestHelper);

        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 3));

        assertEquals(gameTestHelper, 3, deepCrateBlockEntity.extraRows(), "rows added");
        assertEquals(gameTestHelper, COPPER_SLOTS + 27, deepCrateBlockEntity.getContainerSize(), "crate size");
        gameTestHelper.succeed();
    }

    @GameTest
    public void theAddedRowsOpenInTheMenu(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateBlockEntity deepCrateBlockEntity = placeCrate(gameTestHelper);
        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 2));

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        assertEquals(gameTestHelper, COPPER_SLOTS + 18, deepCrateMenu.getContainer().getContainerSize(), "slots in the menu");
        gameTestHelper.succeed();
    }

    @GameTest
    public void takingTheRowModulesBackSpillsWhatWasInTheLostRows(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateBlockEntity deepCrateBlockEntity = placeCrate(gameTestHelper);
        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 1));
        deepCrateBlockEntity.storage().set(COPPER_SLOTS + 4, new ItemStack(Items.DIRT, 40));

        deepCrateBlockEntity.setRowModules(ItemStack.EMPTY);
        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);
        deepCrateMenu.removed(serverPlayer);

        assertEquals(gameTestHelper, COPPER_SLOTS, deepCrateBlockEntity.getContainerSize(), "crate size after the trim");
        assertEquals(gameTestHelper, 40, CrateMenuGameTest.droppedCount(gameTestHelper, Items.DIRT), "what fell on the ground");
        gameTestHelper.succeed();
    }

    @GameTest
    public void acrateIsNotCutBackWhileSomeoneElseHasItOpen(GameTestHelper gameTestHelper) {
        ServerPlayer first = gameTestHelper.makeMockServerPlayerInLevel();
        ServerPlayer second = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateBlockEntity deepCrateBlockEntity = placeCrate(gameTestHelper);
        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 1));

        // A mock player is made at the origin of the world; the crate only counts the ones standing
        // next to it, as it does for a real player.
        Vec3 atTheCrate = gameTestHelper.absoluteVec(Vec3.atCenterOf(CRATE));
        first.snapTo(atTheCrate.x, atTheCrate.y, atTheCrate.z);
        second.snapTo(atTheCrate.x, atTheCrate.y, atTheCrate.z);

        DeepCrateMenu firstMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, first.getInventory(), first);
        DeepCrateMenu secondMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(2, second.getInventory(), second);
        // What openMenu does for a real player, and what the crate reads to know who is looking.
        first.containerMenu = firstMenu;
        second.containerMenu = secondMenu;
        deepCrateBlockEntity.setRowModules(ItemStack.EMPTY);

        firstMenu.removed(first);
        first.containerMenu = first.inventoryMenu;
        int whileSecondIsOpen = deepCrateBlockEntity.getContainerSize();

        secondMenu.removed(second);
        second.containerMenu = second.inventoryMenu;
        int onceEveryoneIsGone = deepCrateBlockEntity.getContainerSize();

        assertEquals(gameTestHelper, COPPER_SLOTS + 9, whileSecondIsOpen, "size while the second screen is open");
        assertEquals(gameTestHelper, COPPER_SLOTS, onceEveryoneIsGone, "size once the last screen is gone");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aRowSlotRefusesAnythingThatIsNotARowModule(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateBlockEntity deepCrateBlockEntity = placeCrate(gameTestHelper);
        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        if (deepCrateMenu.getSlot(1).mayPlace(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)))) {
            gameTestHelper.fail("the row slot took a capacity module");
        }

        if (!deepCrateMenu.getSlot(1).mayPlace(new ItemStack(RegistryInit.ROW_MODULE_ITEM))) {
            gameTestHelper.fail("the row slot refused a row module");
        }

        gameTestHelper.succeed();
    }

    private static DeepCrateBlockEntity placeCrate(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        return gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
    }

    private static void assertEquals(GameTestHelper gameTestHelper, int expected, int actual, String what) {
        if (expected != actual) {
            gameTestHelper.fail(what + ": expected " + expected + ", got " + actual);
        }
    }
}
