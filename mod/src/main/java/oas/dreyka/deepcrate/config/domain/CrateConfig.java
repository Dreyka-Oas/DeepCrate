package oas.dreyka.deepcrate.config.domain;

/** What a crate holds, how far it grows, and what automation is allowed to push into it. */
public final class CrateConfig {
    private CrateConfig() {}

    /** A slot with no module in it, which is what the rest of the game holds. */
    public static int baseCapacity = 64;

    /**
     * Whether a crate answers 64 to hoppers and pipes while lithium is installed.
     *
     * Lithium replaces the hopper wholesale and keeps its own copy of the target inventory. Against a
     * container that answers more than 64 it takes items out of the hopper and never writes them in:
     * measured, eight blocks of dirt destroyed per run. Turning this off buys the feature back and
     * loses those items, which is the server owner's call rather than the mod's.
     */
    public static boolean limitAutomationWithLithium = true;

    /** Rows drawn on one page before the screen cuts the crate into several. */
    public static int maxRowsPerPage = 4;

    /** Row modules one crate takes at once, which is also the item's own stack limit. */
    public static int rowModuleStackLimit = 16;
}
