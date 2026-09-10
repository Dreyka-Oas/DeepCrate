package oas.dreyka.deepcrate.gametest;

import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.menu.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import oas.dreyka.deepcrate.net.CrateSortPayload;

/**
 * The sort packet as a forged one arrives: through the player's own listener, with whatever a client
 * chose to put in it.
 *
 * The two things a client controls are the container id and the order, and neither may reach past the
 * screen the player has open. Sorting itself is covered by {@link CrateSortGameTest}; what is measured
 * here is what a made-up packet moves, which should be nothing it does not already own.
 */
public class CrateSortGateGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);

    @GameTest
    public void anidThatMatchesNoOpenScreenSortsNothing(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = open(gameTestHelper);
        int total = totalOf(gameTestHelper);
        ItemStack before = crate(gameTestHelper).storage().get(17).copy();

        send(serverPlayer, serverPlayer.containerMenu.containerId + 7, List.of(Items.STONE, Items.DIRT));
        gameTestHelper.runAfterDelay(1, () -> {
            ItemStack after = crate(gameTestHelper).storage().get(17);
            if (!ItemStack.matches(before, after)) {
                gameTestHelper.fail("a packet naming another screen rearranged the crate anyway");
            }

            assertTotal(gameTestHelper, total);
            gameTestHelper.succeed();
        });
    }

    /**
     * Repeats, an item the crate has never held, and nothing naming what it does hold. A client can
     * send any of that, and the worst it may cost its sender is a crate in an order they did not want.
     */
    @GameTest
    public void anorderFullOfNonsenseKeepsEveryItem(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = open(gameTestHelper);
        int total = totalOf(gameTestHelper);

        send(
            serverPlayer,
            serverPlayer.containerMenu.containerId,
            List.of(Items.BEDROCK, Items.DIRT, Items.DIRT, Items.BEDROCK, Items.NETHER_STAR)
        );
        gameTestHelper.runAfterDelay(1, () -> {
            assertTotal(gameTestHelper, total);
            gameTestHelper.succeed();
        });
    }

    /** No crate screen open at all, which is the packet arriving between two openings. */
    @GameTest
    public void apacketWithNoCrateScreenOpenSortsNothing(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = open(gameTestHelper);
        int total = totalOf(gameTestHelper);
        int containerId = serverPlayer.containerMenu.containerId;
        serverPlayer.closeContainer();

        send(serverPlayer, containerId, List.of(Items.STONE, Items.DIRT));
        gameTestHelper.runAfterDelay(1, () -> {
            assertTotal(gameTestHelper, total);
            gameTestHelper.succeed();
        });
    }

    private static ServerPlayer open(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.storage().set(4, new ItemStack(Items.DIRT, 40));
        deepCrateBlockEntity.storage().set(17, new ItemStack(Items.STONE, 9));
        deepCrateBlockEntity.storage().set(22, new ItemStack(Items.DIRT, 15));
        serverPlayer.openMenu(deepCrateBlockEntity);

        if (!(serverPlayer.containerMenu instanceof DeepCrateMenu)) {
            gameTestHelper.fail("the crate screen did not open, so nothing here reaches the receiver");
        }

        return serverPlayer;
    }

    private static void send(ServerPlayer serverPlayer, int containerId, List<Item> order) {
        serverPlayer.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CrateSortPayload(containerId, order)));
    }

    private static void assertTotal(GameTestHelper gameTestHelper, int expected) {
        int actual = totalOf(gameTestHelper);
        if (actual != expected) {
            gameTestHelper.fail("the crate held " + expected + " items and now holds " + actual);
        }
    }

    private static int totalOf(GameTestHelper gameTestHelper) {
        DeepCrateBlockEntity deepCrateBlockEntity = crate(gameTestHelper);
        int total = 0;
        for (int i = 0; i < deepCrateBlockEntity.storage().size(); i++) {
            total += deepCrateBlockEntity.storage().get(i).getCount();
        }

        return total;
    }

    private static DeepCrateBlockEntity crate(GameTestHelper gameTestHelper) {
        return gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
    }
}
