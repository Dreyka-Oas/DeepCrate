package oas.dreyka.deepcrate.init;

import java.util.List;
import net.minecraft.world.level.material.MapColor;

final class TierSpecs {
    /**
     * The eleven tiers, ordered by how hard the mineral is to obtain rather than by the usual
     * iron-gold-diamond scale. Each tier adds one row of nine.
     */
    static final List<TierSpec> TIER_SPECS = List.of(
        new TierSpec("coal_crate", 3, MapColor.COLOR_BLACK),
        new TierSpec("copper_crate", 4, MapColor.COLOR_ORANGE),
        new TierSpec("iron_crate", 5, MapColor.METAL),
        new TierSpec("redstone_crate", 6, MapColor.FIRE),
        new TierSpec("lapis_crate", 7, MapColor.LAPIS),
        new TierSpec("gold_crate", 8, MapColor.GOLD),
        new TierSpec("amethyst_crate", 9, MapColor.COLOR_PURPLE),
        new TierSpec("quartz_crate", 10, MapColor.QUARTZ),
        new TierSpec("emerald_crate", 11, MapColor.EMERALD),
        new TierSpec("diamond_crate", 12, MapColor.DIAMOND),
        new TierSpec("netherite_crate", 13, MapColor.COLOR_BLACK)
    );

    private TierSpecs() {}

    /** @param rows rows of nine slots, before any page split */
    record TierSpec(String name, int rows, MapColor mapColor) {}
}
