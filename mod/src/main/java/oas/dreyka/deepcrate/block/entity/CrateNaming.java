package oas.dreyka.deepcrate.block.entity;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.placement.CratePairing;
import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The name a crate falls back to and the one it shows, once its pair is taken into account. */
public final class CrateNaming {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.crate");

    private CrateNaming() {}

    public static Component defaultName(DeepCrateBlockEntity crate) {
        CrateTier crateTier = DeepCrateApi.tierOf(crate.getBlockState().getBlock());
        if (crateTier == null) {
            return DEFAULT_NAME;
        }

        Component name = Component.translatable(crateTier.block().getDescriptionId());
        return crate.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE
            ? name
            : Component.translatable("container.deepcrate.double", name);
    }

    /**
     * A pair opens under one title: the name given to either half, or the shared default when neither
     * was named on an anvil.
     */
    public static Component displayName(DeepCrateBlockEntity crate) {
        for (DeepCrateBlockEntity deepCrateBlockEntity : CratePairing.cratesFor(crate)) {
            if (deepCrateBlockEntity.getCustomName() != null) {
                return deepCrateBlockEntity.getCustomName();
            }
        }

        // defaultName(crate), not crate.getDefaultName(): that override stays protected to match the
        // vanilla contract, and this class sits outside the block package now, so it reads the same
        // logic through the static method that lives right above instead of the narrower override.
        return defaultName(crate);
    }
}
