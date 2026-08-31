package com.dreykaoas.deepcrate.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * One kind of crate: how many rows its screen holds, and which block carries it.
 *
 * @param id    namespaced name, also the block and item name
 * @param rows  rows of nine slots, before any page split
 * @param block the block registered for this tier
 */
public record CrateTier(Identifier id, int rows, Block block) {
    public static final int COLUMNS = 9;

    public CrateTier {
        if (rows < 1) {
            throw new IllegalArgumentException("Crate tier " + id + " needs at least one row, got " + rows);
        }
    }

    public int slotCount() {
        return this.rows * COLUMNS;
    }
}
