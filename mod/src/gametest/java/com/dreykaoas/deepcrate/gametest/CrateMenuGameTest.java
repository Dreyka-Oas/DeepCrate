package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

/**
 * What JUnit cannot reach: a menu needs a real player and a real level, and every rule about
 * clicking, shift-clicking, paging and closing lives in the menu.
 */
public class CrateMenuGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);
    private static final BlockPos PARTNER = new BlockPos(2, 1, 1);

    @GameTest
    public void anEmptyCrateHoldsSixtyFourPerSlot(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState());

        assertEquals(gameTestHelper, DeepCrateApi.BASE_CAPACITY, deepCrateMenu.capacity(), "capacity with no module");
        assertEquals(gameTestHelper, 27, deepCrateMenu.getContainer().getContainerSize(), "copper crate size");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aModuleInTheTopLeftSlotRaisesTheWholeCrate(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState());

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();

        assertEquals(gameTestHelper, 512, deepCrateMenu.capacity(), "capacity with the 512 module");
        assertEquals(gameTestHelper, 512, deepCrateMenu.getContainer().getMaxStackSize(), "container limit");
        assertEquals(gameTestHelper, 512, deepCrateMenu.getSlot(2).getMaxStackSize(), "crate slot limit");
        gameTestHelper.succeed();
    }

    @GameTest
    public void theModuleSlotSitsOutsideThePanel(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState());

        if (deepCrateMenu.getSlot(0).x >= 0) {
            gameTestHelper.fail("the module slot is at x=" + deepCrateMenu.getSlot(0).x + ", inside the panel");
        }

        // The crate grid starts where a chest's does, so nothing was pushed down to make room.
        assertEquals(gameTestHelper, 18, deepCrateMenu.getSlot(2).y, "the first crate row");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aModuleSlotRefusesAnythingElse(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState());

        if (deepCrateMenu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))) {
            gameTestHelper.fail("the module slot took a block of dirt");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void shiftClickingIntoACrateLosesNothing(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        int firstPlayerSlot = deepCrateMenu.crateSlotStart() + deepCrateMenu.getContainer().getContainerSize();
        deepCrateMenu.getSlot(firstPlayerSlot).set(new ItemStack(Items.DIRT, 64));

        deepCrateMenu.quickMoveStack(serverPlayer, firstPlayerSlot);

        assertEquals(gameTestHelper, 64, totalOf(deepCrateMenu, Items.DIRT), "dirt after shift-click in");
        assertEquals(gameTestHelper, 64, deepCrateMenu.getSlot(2).getItem().getCount(), "the crate slot");
        gameTestHelper.succeed();
    }

    @GameTest
    public void shiftClickingOutOfACrateLosesNothing(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 200));

        deepCrateMenu.quickMoveStack(serverPlayer, 2);

        assertEquals(gameTestHelper, 200, totalOf(deepCrateMenu, Items.DIRT), "dirt after one shift-click out");
        gameTestHelper.succeed();
    }

    @GameTest
    public void takingFromAFullSlotNeverExceedsAHand(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 512));

        deepCrateMenu.clicked(2, 0, ClickType.PICKUP, serverPlayer);

        assertEquals(gameTestHelper, 64, deepCrateMenu.getCarried().getCount(), "what the hand holds");
        assertEquals(gameTestHelper, 448, deepCrateMenu.getSlot(2).getItem().getCount(), "what stays in the slot");
        gameTestHelper.succeed();
    }

    @GameTest
    public void pullingTheModuleOutSpillsOnClose(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 500));

        deepCrateMenu.getSlot(0).set(ItemStack.EMPTY);
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.removed(serverPlayer);

        assertEquals(gameTestHelper, 64, deepCrateMenu.getSlot(2).getItem().getCount(), "what the slot keeps");
        assertEquals(gameTestHelper, 436, droppedCount(gameTestHelper, Items.DIRT), "what fell on the ground");
        gameTestHelper.succeed();
    }

    @GameTest
    public void changingPageTouchesNothingButVisibility(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(5).block().defaultBlockState());
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 40));

        if (deepCrateMenu.layout().pageCount() < 2) {
            gameTestHelper.fail("an echo crate should need more than one page");
        }

        deepCrateMenu.setPage(1);
        boolean firstHidden = !deepCrateMenu.getSlot(2).isActive();
        deepCrateMenu.setPage(0);

        if (!firstHidden) {
            gameTestHelper.fail("slot 1 stayed visible on page two");
        }

        assertEquals(gameTestHelper, 40, deepCrateMenu.getSlot(2).getItem().getCount(), "content after paging");
        assertEquals(gameTestHelper, 72, deepCrateMenu.getContainer().getContainerSize(), "echo crate size");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aDoubleCrateOpensAsOne(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.LEFT));
        gameTestHelper.setBlock(PARTNER, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.RIGHT));

        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        assertEquals(gameTestHelper, 54, deepCrateMenu.getContainer().getContainerSize(), "double copper crate size");

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        deepCrateMenu.getSlot(0).setChanged();

        assertEquals(gameTestHelper, 128, deepCrateMenu.getSlot(2).getMaxStackSize(), "first half limit");
        assertEquals(gameTestHelper, 128, deepCrateMenu.getSlot(28).getMaxStackSize(), "far half limit");
        gameTestHelper.succeed();
    }

    @GameTest
    public void theHalfWithoutTheModuleFollowsItsPartner(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.LEFT));
        gameTestHelper.setBlock(PARTNER, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.RIGHT));

        DeepCrateBlockEntity holder = gameTestHelper.getBlockEntity(PARTNER, DeepCrateBlockEntity.class);
        DeepCrateBlockEntity follower = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        holder.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));

        assertEquals(gameTestHelper, 512, follower.storage().capacity(), "the far half's capacity");

        follower.storage().insert(new ItemStack(Items.DIRT, 300));
        assertEquals(gameTestHelper, 300, follower.storage().get(0).getCount(), "what the far half took in one slot");
        gameTestHelper.succeed();
    }

    @GameTest
    public void breakingOneHalfLeavesTheOtherWhole(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.LEFT));
        gameTestHelper.setBlock(PARTNER, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.RIGHT));

        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(PARTNER, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.storage().set(0, new ItemStack(Items.DIRT, 32));

        gameTestHelper.destroyBlock(CRATE);

        DeepCrateBlockEntity survivor = gameTestHelper.getBlockEntity(PARTNER, DeepCrateBlockEntity.class);
        if (survivor.getBlockState().getValue(DeepCrateBlock.TYPE) != ChestType.SINGLE) {
            gameTestHelper.fail("the surviving half stayed paired");
        }

        assertEquals(gameTestHelper, 32, survivor.storage().get(0).getCount(), "what the survivor kept");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aSwordNeverStacksHoweverBigTheModule(GameTestHelper gameTestHelper) {
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState());
        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();

        assertEquals(gameTestHelper, 1, deepCrateMenu.getSlot(2).getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)), "sword limit");
        assertEquals(gameTestHelper, 512, deepCrateMenu.getSlot(2).getMaxStackSize(new ItemStack(Items.DIRT)), "dirt limit");

        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        ItemStack leftover = deepCrateBlockEntity.storage().insert(new ItemStack(Items.DIAMOND_SWORD, 4));
        assertEquals(gameTestHelper, 1, deepCrateBlockEntity.storage().get(0).getCount(), "swords in the first slot");
        assertEquals(gameTestHelper, 0, leftover.getCount(), "swords left over after four slots took one each");
        gameTestHelper.succeed();
    }

    @GameTest
    public void twoViewersShareOneModule(GameTestHelper gameTestHelper) {
        ServerPlayer first = gameTestHelper.makeMockServerPlayerInLevel();
        ServerPlayer second = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu firstMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), first);

        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        DeepCrateMenu secondMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(2, second.getInventory(), second);

        firstMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        firstMenu.getSlot(0).setChanged();

        assertEquals(gameTestHelper, 1, secondMenu.getSlot(0).getItem().getCount(), "what the second viewer sees in the module slot");

        secondMenu.getSlot(0).set(ItemStack.EMPTY);
        secondMenu.getSlot(0).setChanged();

        assertEquals(gameTestHelper, 0, firstMenu.getSlot(0).getItem().getCount(), "what the first viewer has left");
        assertEquals(gameTestHelper, 0, deepCrateBlockEntity.module().getCount(), "what the crate has left");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aNumberKeyCannotDragAnOversizedStackIntoTheHotbar(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 500));

        deepCrateMenu.clicked(2, 0, ClickType.SWAP, serverPlayer);

        assertEquals(gameTestHelper, 500, deepCrateMenu.getSlot(2).getItem().getCount(), "what stays in the crate");
        assertEquals(gameTestHelper, 0, serverPlayer.getInventory().getItem(0).getCount(), "what reached the hotbar");
        gameTestHelper.succeed();
    }

    @GameTest
    public void aSecondModuleIsStoredRatherThanRefused(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        int firstPlayerSlot = deepCrateMenu.crateSlotStart() + deepCrateMenu.getContainer().getContainerSize();
        deepCrateMenu.getSlot(firstPlayerSlot).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        deepCrateMenu.quickMoveStack(serverPlayer, firstPlayerSlot);
        assertEquals(gameTestHelper, 1, deepCrateMenu.getSlot(0).getItem().getCount(), "the first module went to its slot");

        deepCrateMenu.getSlot(firstPlayerSlot).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(1)));
        deepCrateMenu.quickMoveStack(serverPlayer, firstPlayerSlot);

        assertEquals(gameTestHelper, 1, deepCrateMenu.getSlot(2).getItem().getCount(), "the second module went into storage");
        gameTestHelper.succeed();
    }

    @GameTest
    public void droppingAStackOntoAFullSlotKeepsEveryItem(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        DeepCrateMenu deepCrateMenu = openCrate(gameTestHelper, RegistryInit.TIERS.get(0).block().defaultBlockState(), serverPlayer);

        deepCrateMenu.getSlot(0).set(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateMenu.getSlot(0).setChanged();
        deepCrateMenu.getSlot(2).set(new ItemStack(Items.DIRT, 400));
        deepCrateMenu.setCarried(new ItemStack(Items.DIRT, 64));

        deepCrateMenu.clicked(2, 0, ClickType.PICKUP, serverPlayer);

        assertEquals(gameTestHelper, 464, deepCrateMenu.getSlot(2).getItem().getCount(), "the slot after dropping a stack on it");
        assertEquals(gameTestHelper, 0, deepCrateMenu.getCarried().getCount(), "what stays in hand");
        gameTestHelper.succeed();
    }

    private static DeepCrateMenu openCrate(GameTestHelper gameTestHelper, net.minecraft.world.level.block.state.BlockState blockState) {
        return openCrate(gameTestHelper, blockState, gameTestHelper.makeMockServerPlayerInLevel());
    }

    private static DeepCrateMenu openCrate(
        GameTestHelper gameTestHelper, net.minecraft.world.level.block.state.BlockState blockState, ServerPlayer serverPlayer
    ) {
        gameTestHelper.setBlock(CRATE, blockState);
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        return (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);
    }

    /** Everything the menu can see, crate and player inventory alike. */
    private static int totalOf(DeepCrateMenu deepCrateMenu, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < deepCrateMenu.slots.size(); i++) {
            ItemStack itemStack = deepCrateMenu.getSlot(i).getItem();
            if (itemStack.is(item)) {
                total += itemStack.getCount();
            }
        }

        return total;
    }

    static int droppedCount(GameTestHelper gameTestHelper, net.minecraft.world.item.Item item) {
        AABB aABB = gameTestHelper.getBounds().inflate(4.0);
        int total = 0;
        for (ItemEntity itemEntity : gameTestHelper.getLevel().getEntitiesOfClass(ItemEntity.class, aABB)) {
            if (itemEntity.getItem().is(item)) {
                total += itemEntity.getItem().getCount();
            }
        }

        return total;
    }

    private static void assertEquals(GameTestHelper gameTestHelper, int expected, int actual, String what) {
        if (expected != actual) {
            gameTestHelper.fail(what + ": expected " + expected + ", got " + actual);
        }
    }
}
