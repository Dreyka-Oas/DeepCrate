package oas.dreyka.deepcrate.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * One kind of crate: how large its grid is, and which block carries it.
 *
 * @param id      namespaced name, also the block and item name
 * @param rows    rows of slots, before any page split
 * @param columns slots to a row; the screen panel is built to whatever this says
 * @param block   the block registered for this tier
 */
public record CrateTier(Identifier id, int rows, int columns, Block block) {
    /** What a chest is wide, and what a tier gets when it does not say. */
    public static final int DEFAULT_COLUMNS = 9;

    public CrateTier {
        if (rows < 1) {
            throw new IllegalArgumentException("Crate tier " + id + " needs at least one row, got " + rows);
        }

        if (columns < 1) {
            throw new IllegalArgumentException("Crate tier " + id + " needs at least one column, got " + columns);
        }
    }

    public CrateTier(Identifier identifier, int rows, Block block) {
        this(identifier, rows, DEFAULT_COLUMNS, block);
    }

    public int slotCount() {
        return this.rows * this.columns;
    }
}
