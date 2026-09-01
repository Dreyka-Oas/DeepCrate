package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

/** What automation does to a crate, and what the patched hopper does to everything else. */
public class CrateHopperGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = new BlockPos(1, 2, 1);
    private static final BlockPos BELOW = new BlockPos(1, 0, 1);

    @GameTest(maxTicks = 200)
    public void aHopperFillsACrateBeyondSixtyFour(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        deepCrateBlockEntity.storage().set(0, new ItemStack(Items.DIRT, 64));

        gameTestHelper.setBlock(ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(ABOVE, HopperBlockEntity.class);
        // A hopper moves one item every eight ticks, so eight items is what fits in a test.
        hopperBlockEntity.setItem(0, new ItemStack(Items.DIRT, 8));

        int expected = DeepCrateApi.AUTOMATION_LIMITED ? 64 : 72;
        gameTestHelper.succeedWhen(() -> {
            DeepCrateBlockEntity crate = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
            int inCrate = crate.storage().get(0).getCount();
            int inHopper = hopperBlockEntity.getItem(0).getCount();
            int elsewhere = 0;
            for (int i = 1; i < crate.storage().size(); i++) {
                elsewhere += crate.storage().get(i).getCount();
            }

            if (inCrate + inHopper + elsewhere != 72) {
                gameTestHelper.fail("dirt went missing: " + inCrate + " here, " + elsewhere + " elsewhere, " + inHopper + " in the hopper");
            }

            if (inCrate != expected) {
                gameTestHelper.fail("the hopper stopped at " + inCrate + " instead of " + expected);
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void aCrateWhoseSlotsAllHoldSixtyFourIsNotFull(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        for (int i = 0; i < deepCrateBlockEntity.storage().size(); i++) {
            deepCrateBlockEntity.storage().set(i, new ItemStack(Items.DIRT, 64));
        }

        gameTestHelper.setBlock(ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(ABOVE, HopperBlockEntity.class);
        hopperBlockEntity.setItem(0, new ItemStack(Items.DIRT, 4));

        if (DeepCrateApi.AUTOMATION_LIMITED) {
            // With lithium the crate deliberately looks full at 64 a slot, so nothing should move and
            // nothing should vanish.
            gameTestHelper.runAtTickTime(80, () -> {
                if (hopperBlockEntity.getItem(0).getCount() != 4) {
                    gameTestHelper.fail("items left the hopper anyway: " + hopperBlockEntity.getItem(0).getCount());
                }

                gameTestHelper.succeed();
            });
            return;
        }

        gameTestHelper.succeedWhen(() -> {
            if (!hopperBlockEntity.getItem(0).isEmpty()) {
                gameTestHelper.fail("the hopper still holds " + hopperBlockEntity.getItem(0).getCount());
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void anEmptyCrateWithNoModuleTakesWhatIsPushed(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block());
        gameTestHelper.setBlock(ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(ABOVE, HopperBlockEntity.class);
        hopperBlockEntity.setItem(0, new ItemStack(Items.DIRT, 8));

        gameTestHelper.succeedWhen(() -> {
            DeepCrateBlockEntity crate = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
            int inCrate = 0;
            for (int i = 0; i < crate.storage().size(); i++) {
                inCrate += crate.storage().get(i).getCount();
            }

            int inHopper = hopperBlockEntity.getItem(0).getCount();
            if (inCrate + inHopper != 8) {
                gameTestHelper.fail("dirt went missing: " + inCrate + " in the crate, " + inHopper + " in the hopper");
            }

            if (inCrate != 8) {
                gameTestHelper.fail("only " + inCrate + " arrived");
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void aHopperUnderACrateTakesFromIt(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));
        deepCrateBlockEntity.storage().set(0, new ItemStack(Items.DIRT, 100));

        gameTestHelper.setBlock(BELOW, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(BELOW, HopperBlockEntity.class);

        gameTestHelper.succeedWhen(() -> {
            DeepCrateBlockEntity crate = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
            int inCrate = crate.storage().get(0).getCount();
            int inHopper = 0;
            for (int i = 0; i < hopperBlockEntity.getContainerSize(); i++) {
                inHopper += hopperBlockEntity.getItem(i).getCount();
            }

            if (inCrate + inHopper != 100) {
                gameTestHelper.fail("dirt went missing: " + inCrate + " in the crate, " + inHopper + " in the hopper");
            }

            if (inHopper < 5) {
                gameTestHelper.fail("the hopper pulled only " + inHopper);
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void aHopperFillsTheHalfItTouches(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.LEFT));
        gameTestHelper.setBlock(new BlockPos(2, 1, 1), RegistryInit.TIERS.get(0).block().defaultBlockState().setValue(DeepCrateBlock.TYPE, ChestType.RIGHT));

        DeepCrateBlockEntity holder = gameTestHelper.getBlockEntity(new BlockPos(2, 1, 1), DeepCrateBlockEntity.class);
        holder.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));

        gameTestHelper.setBlock(ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(ABOVE, HopperBlockEntity.class);
        hopperBlockEntity.setItem(0, new ItemStack(Items.DIRT, 8));

        // The pair is not joined for hoppers, on purpose: joining it there crashes lithium. What the
        // hopper must still get right is the shared capacity of the half it touches.
        gameTestHelper.succeedWhen(() -> {
            DeepCrateBlockEntity near = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
            int dirt = 0;
            for (int i = 0; i < near.storage().size(); i++) {
                if (near.storage().get(i).is(Items.DIRT)) {
                    dirt += near.storage().get(i).getCount();
                }
            }

            if (dirt != 8) {
                gameTestHelper.fail("only " + dirt + " dirt reached the near half");
            }

            if (near.storage().capacity() != 128) {
                gameTestHelper.fail("the near half is at " + near.storage().capacity() + " instead of the pair's 128");
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void avanillaChestStillStopsAtSixtyFour(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, Blocks.CHEST);
        net.minecraft.world.level.block.entity.ChestBlockEntity chestBlockEntity = gameTestHelper.getBlockEntity(
            CRATE, net.minecraft.world.level.block.entity.ChestBlockEntity.class
        );
        chestBlockEntity.setItem(0, new ItemStack(Items.DIRT, 64));

        gameTestHelper.setBlock(ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperBlockEntity = gameTestHelper.getBlockEntity(ABOVE, HopperBlockEntity.class);
        hopperBlockEntity.setItem(0, new ItemStack(Items.DIRT, 8));

        // Read late on purpose: the point is where the hopper STOPS, so the check must come after it
        // has had every chance to overfill the first slot.
        gameTestHelper.runAtTickTime(150, () -> {
            if (chestBlockEntity.getItem(0).getCount() != 64) {
                gameTestHelper.fail("the patched hopper overfilled a vanilla chest: " + chestBlockEntity.getItem(0).getCount());
            }

            if (chestBlockEntity.getItem(1).getCount() != 8) {
                gameTestHelper.fail("the vanilla chest did not take the overflow in its next slot");
            }

            gameTestHelper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void aComparatorReadsAFullCrate(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        deepCrateBlockEntity.setModule(new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        for (int i = 0; i < deepCrateBlockEntity.storage().size(); i++) {
            deepCrateBlockEntity.storage().set(i, new ItemStack(Items.STONE, 512));
        }

        gameTestHelper.succeedWhen(() -> {
            int signal = deepCrateBlockEntity.getBlockState()
                .getAnalogOutputSignal(gameTestHelper.getLevel(), gameTestHelper.absolutePos(CRATE), Direction.UP);
            if (signal != 15) {
                gameTestHelper.fail("a full crate reads " + signal + " instead of 15");
            }
        });
    }
}
