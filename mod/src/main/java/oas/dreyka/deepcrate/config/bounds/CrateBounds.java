package oas.dreyka.deepcrate.config.bounds;

import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.config.BoundsRegistrar;

/**
 * Clamp ranges for the crate options.
 *
 * {@code limitAutomationWithLithium} has no line: a boolean crosses the clamp untouched, and a range
 * on one would be a pair of numbers nothing reads.
 */
public final class CrateBounds {
    private CrateBounds() {}

    public static void register(BoundsRegistrar registrar) {
        // Under 1, CrateStorage's constructor throws and every crate takes its chunk down with it.
        // Over MAX_CAPACITY the capacity data slot wraps, because that packet writes a short.
        //
        // MAX_CAPACITY is a compile-time constant, so javac copies 32767 in here and the class file
        // keeps no reference to DeepCrateApi. That matters: this table is built inside a JUnit run
        // where no game has started, and DeepCrateApi's own initialiser asks FabricLoader a question.
        registrar.b("baseCapacity", 1, DeepCrateApi.MAX_CAPACITY);
        // Over 6 a page stands taller than the largest chest of the base game, and the player's own
        // inventory leaves the bottom of a 240-pixel screen.
        registrar.b("maxRowsPerPage", 1, 6);
        // Over 20 a pair of eight-row crates has more slots than the sort packet can name, and a crate
        // full of items all different produces a list CrateSortPayload's codec refuses.
        registrar.b("rowModuleStackLimit", 1, 20);
    }
}
