package oas.dreyka.deepcrate.block;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The name a crate falls back to and the one it shows, once its pair is taken into account. */
final class CrateNaming {
    private static final Component DEFAULT_NAME = Component.translatable("container.deepcrate.crate");

    private CrateNaming() {}

    static Component defaultName(DeepCrateBlockEntity crate) {
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
    static Component displayName(DeepCrateBlockEntity crate) {
        for (DeepCrateBlockEntity deepCrateBlockEntity : CratePairing.cratesFor(crate)) {
            if (deepCrateBlockEntity.getCustomName() != null) {
                return deepCrateBlockEntity.getCustomName();
            }
        }

        return crate.getDefaultName();
    }
}
