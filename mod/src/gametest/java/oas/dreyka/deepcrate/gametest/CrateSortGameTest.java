package oas.dreyka.deepcrate.gametest;

import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The buttons above the screen, from the menu down: what the order does to a crate and to a pair. */
public class CrateSortGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);
    private static final BlockPos PARTNER = new BlockPos(2, 1, 1);

    @GameTest
    public void sortingGathersThePilesAndFollowsTheOrder(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateBlockEntity.storage().set(4, new ItemStack(Items.DIRT, 200));
        deepCrateBlockEntity.storage().set(17, new ItemStack(Items.STONE, 9));
        deepCrateBlockEntity.storage().set(22, new ItemStack(Items.DIRT, 150));

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);
        deepCrateMenu.sort(List.of(Items.STONE, Items.DIRT));

        Container container = deepCrateMenu.wiring().crate();
        assertStack(gameTestHelper, container, 0, Items.STONE, 9);
        assertStack(gameTestHelper, container, 1, Items.DIRT, 350);
        if (!container.getItem(2).isEmpty()) {
            gameTestHelper.fail("the third slot should have come out empty");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void apairIsSortedAsOneRunOfSlots(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.LEFT));
        gameTestHelper.setBlock(PARTNER, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.RIGHT));

        DeepCrateBlockEntity first = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        DeepCrateBlockEntity second = gameTestHelper.getBlockEntity(PARTNER, DeepCrateBlockEntity.class);
        first.storage().set(20, new ItemStack(Items.DIRT, 30));
        second.storage().set(3, new ItemStack(Items.DIRT, 30));
        second.storage().set(9, new ItemStack(Items.STONE, 5));

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) first.createMenu(1, serverPlayer.getInventory(), serverPlayer);
        deepCrateMenu.sort(List.of(Items.STONE, Items.DIRT));

        Container container = deepCrateMenu.wiring().crate();
        assertStack(gameTestHelper, container, 0, Items.STONE, 5);
        // One pile of sixty, not two of thirty on either side of the seam.
        assertStack(gameTestHelper, container, 1, Items.DIRT, 60);
        gameTestHelper.succeed();
    }

    @GameTest
    public void sortingLeavesTheModulesWhereTheyAre(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 2));

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);
        deepCrateMenu.sort(List.of(Items.DIRT));

        if (deepCrateMenu.getSlot(0).getItem().isEmpty() || deepCrateMenu.getSlot(1).getItem().isEmpty()) {
            gameTestHelper.fail("a module was swept into the crate");
        }

        if (deepCrateMenu.wiring().crate().getContainerSize() != 45) {
            gameTestHelper.fail("the crate lost its rows: " + deepCrateMenu.wiring().crate().getContainerSize());
        }

        gameTestHelper.succeed();
    }

    private static void assertStack(GameTestHelper gameTestHelper, Container container, int slot, net.minecraft.world.item.Item item, int count) {
        ItemStack itemStack = container.getItem(slot);
        if (!itemStack.is(item) || itemStack.getCount() != count) {
            gameTestHelper.fail("slot " + slot + ": expected " + count + " " + item + ", got " + itemStack.getCount() + " " + itemStack.getItem());
        }
    }
}
