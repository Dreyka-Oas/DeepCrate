package oas.dreyka.deepcrate.gametest;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.init.RegistryInit;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * What a hopper pays to ask a crate whether it is full, a single crate against half of a pair.
 *
 * The walk is cheap; what is behind it is not. Both calls the hopper makes for a slot go through
 * storage(), and a paired crate has to work out which of its two halves holds the modules before it
 * can answer either. The day that answer goes back to being computed for every slot, the ratio
 * printed below climbs back past three and this test says so.
 */
public class CrateHopperCostGameTest {
    private static final BlockPos LONE = new BlockPos(1, 1, 1);
    private static final BlockPos NEAR = new BlockPos(3, 1, 1);
    private static final BlockPos FAR = new BlockPos(4, 1, 1);

    private static final int WARMUP_WALKS = 500;
    private static final int TIMED_WALKS = 2000;
    /** The shortest round is the one no collection pause landed in, and the crate walk allocates. */
    private static final int TIMED_ROUNDS = 3;
    /**
     * Five, and wide on purpose. The same walk over the same code gave 2.86 one morning, then 2.67 and
     * 3.86 two minutes apart the next; what moved was what else the machine was doing. A figure close
     * enough to those to be interesting here fails on somebody else's machine for nothing, so what is
     * left is a guard against the paired half going back to costing several times the lone one rather
     * than a decimal anybody should read.
     */
    private static final double CEILING = 5.0;

    @GameTest(maxTicks = 400)
    public void halfAPairAnswersAHopperAtAboutTheCostOfALoneCrate(GameTestHelper gameTestHelper) {
        DeepCrateBlockEntity lone = crateAt(gameTestHelper, LONE, ChestType.SINGLE);
        lone.setRowModules(fullRowStack());
        fill(lone);

        DeepCrateBlockEntity near = crateAt(gameTestHelper, NEAR, ChestType.LEFT);
        DeepCrateBlockEntity far = crateAt(gameTestHelper, FAR, ChestType.RIGHT);
        // The modules go on the far half, so the near one has to reach across for every answer, which
        // is the arrangement a hopper meets: it only ever touches the half it sits against.
        far.setRowModules(fullRowStack());
        fill(near);

        if (near.moduleHolder() != far) {
            gameTestHelper.fail("the near half answers for itself, so nothing paired is being measured here");
        }

        if (near.getContainerSize() != lone.getContainerSize()) {
            gameTestHelper.fail("comparing " + near.getContainerSize() + " slots against " + lone.getContainerSize());
        }

        // Both of them before either is timed. Warming one crate at a time gave the first figure the
        // compiler still making its mind up, and a spread wide enough to drown what is being watched.
        warmUp(lone);
        warmUp(near);
        double alone = nanosPerSlot(gameTestHelper, lone);
        double paired = nanosPerSlot(gameTestHelper, near);
        DeepCrate.LOGGER.info(
            "[DeepCrate] hopper walk over {} slots: {} ns a slot alone, {} ns a slot paired, ratio {}",
            lone.getContainerSize(),
            alone,
            paired,
            paired / alone
        );

        if (paired > alone * CEILING) {
            gameTestHelper.fail("half a pair costs " + paired / alone + " times a lone crate for each slot, over the ceiling of " + CEILING);
        }

        gameTestHelper.succeed();
    }

    /**
     * The two calls HopperBlockEntityMixin makes for each slot, and nothing else. The count it adds up
     * is what keeps the walk alive: a loop whose result nobody reads is a loop the compiler is free to
     * delete, and the measurement with it.
     */
    private static long walk(Container container) {
        long seen = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            seen += itemStack.getCount() + container.getMaxStackSize(itemStack);
        }

        return seen;
    }

    private static void warmUp(Container container) {
        for (int i = 0; i < WARMUP_WALKS; i++) {
            walk(container);
        }
    }

    private static double nanosPerSlot(GameTestHelper gameTestHelper, Container container) {
        long expected = walk(container);
        long shortest = Long.MAX_VALUE;

        for (int round = 0; round < TIMED_ROUNDS; round++) {
            long start = System.nanoTime();
            long seen = 0;
            for (int i = 0; i < TIMED_WALKS; i++) {
                seen += walk(container);
            }

            shortest = Math.min(shortest, System.nanoTime() - start);
            if (seen != expected * TIMED_WALKS) {
                gameTestHelper.fail("the crate moved under the measurement: " + seen + " against " + expected * TIMED_WALKS);
            }
        }

        return (double) shortest / TIMED_WALKS / container.getContainerSize();
    }

    /** The largest crate the mod ships, which is where the per-slot cost is worth anything. */
    private static DeepCrateBlockEntity crateAt(GameTestHelper gameTestHelper, BlockPos blockPos, ChestType chestType) {
        gameTestHelper.setBlock(blockPos, RegistryInit.TIERS.getLast().block().defaultBlockState().setValue(DeepCrateBlock.TYPE, chestType));
        return gameTestHelper.getBlockEntity(blockPos, DeepCrateBlockEntity.class);
    }

    private static ItemStack fullRowStack() {
        return new ItemStack(RegistryInit.ROW_MODULE_ITEM, CrateConfig.rowModuleStackLimit);
    }

    /** A full slot everywhere, the state the hopper's own loop runs furthest on. */
    private static void fill(DeepCrateBlockEntity deepCrateBlockEntity) {
        for (int i = 0; i < deepCrateBlockEntity.storage().size(); i++) {
            deepCrateBlockEntity.storage().set(i, new ItemStack(Items.DIRT, 64));
        }
    }
}
