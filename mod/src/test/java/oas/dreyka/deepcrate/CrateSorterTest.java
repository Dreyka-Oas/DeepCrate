package oas.dreyka.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.inventory.CrateSorter;
import oas.dreyka.deepcrate.inventory.CrateStorage;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateSorterTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void scatteredStacksOfTheSameThingBecomeOne() {
        CrateStorage crateStorage = new CrateStorage(27, 512);
        crateStorage.set(3, new ItemStack(Items.DIRT, 40));
        crateStorage.set(11, new ItemStack(Items.DIRT, 50));
        crateStorage.set(20, new ItemStack(Items.STONE, 12));

        CrateSorter.arrange(List.of(crateStorage), List.of(Items.DIRT, Items.STONE));

        assertEquals(90, crateStorage.get(0).getCount());
        assertTrue(crateStorage.get(0).is(Items.DIRT));
        assertEquals(12, crateStorage.get(1).getCount());
        assertTrue(crateStorage.get(1).is(Items.STONE));
        assertTrue(crateStorage.get(2).isEmpty());
    }

    @Test
    void apileTooBigForOneSlotSpillsIntoTheNextOne() {
        CrateStorage crateStorage = new CrateStorage(27, 128);
        crateStorage.set(5, new ItemStack(Items.DIRT, 100));
        crateStorage.set(9, new ItemStack(Items.DIRT, 100));

        CrateSorter.arrange(List.of(crateStorage), List.of(Items.DIRT));

        assertEquals(128, crateStorage.get(0).getCount());
        assertEquals(72, crateStorage.get(1).getCount());
    }

    @Test
    void whatTheOrderDoesNotNameGoesLast() {
        CrateStorage crateStorage = new CrateStorage(27, 64);
        crateStorage.set(0, new ItemStack(Items.STONE, 5));
        crateStorage.set(1, new ItemStack(Items.DIRT, 5));

        CrateSorter.arrange(List.of(crateStorage), List.of(Items.DIRT));

        assertTrue(crateStorage.get(0).is(Items.DIRT));
        assertTrue(crateStorage.get(1).is(Items.STONE));
    }

    @Test
    void bothHalvesOfApairAreOneRun() {
        CrateStorage holder = new CrateStorage(9, 128);
        CrateStorage follower = new CrateStorage(9, 128);
        holder.set(8, new ItemStack(Items.DIRT, 100));
        follower.set(0, new ItemStack(Items.DIRT, 100));
        follower.set(4, new ItemStack(Items.STONE, 7));

        CrateSorter.arrange(List.of(holder, follower), List.of(Items.DIRT, Items.STONE));

        assertEquals(128, holder.get(0).getCount());
        assertEquals(72, holder.get(1).getCount());
        assertEquals(7, holder.get(2).getCount());
        assertTrue(holder.get(2).is(Items.STONE));
        assertTrue(follower.isEmpty());
    }

    @Test
    void aslotLeftAboveTheCapacityIsNotCutIntoMorePiecesThanTheCrateHasRoomFor() {
        // What a crate looks like between a module being pulled out and the screen closing.
        CrateStorage crateStorage = new CrateStorage(2, 64);
        crateStorage.restore(0, new ItemStack(Items.DIRT, 200));
        crateStorage.restore(1, new ItemStack(Items.DIRT, 200));

        CrateSorter.arrange(List.of(crateStorage), List.of(Items.DIRT));

        assertEquals(400, crateStorage.get(0).getCount() + crateStorage.get(1).getCount());
    }

    @Test
    void anemptyCrateComesOutEmpty() {
        CrateStorage crateStorage = new CrateStorage(9, 64);

        CrateSorter.arrange(List.of(crateStorage), List.of());

        assertTrue(crateStorage.isEmpty());
    }
}
