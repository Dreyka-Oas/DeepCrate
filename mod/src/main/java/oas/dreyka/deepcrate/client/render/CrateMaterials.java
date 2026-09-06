package oas.dreyka.deepcrate.client.render;

import oas.dreyka.deepcrate.api.CrateTier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * The chest atlas entry of each tier, in all three shapes.
 *
 * The atlas takes them without any registration: its definition is a directory listing over
 * textures/entity/chest, and the resource manager walks every namespace, modded ones included.
 */
public final class CrateMaterials {
    public static final Material MISSING = Sheets.CHEST_MAPPER.apply(Identifier.withDefaultNamespace("normal"));

    private static final Map<Identifier, Material> SINGLE = new HashMap<>();
    private static final Map<Identifier, Material> LEFT = new HashMap<>();
    private static final Map<Identifier, Material> RIGHT = new HashMap<>();

    private CrateMaterials() {}

    public static Material of(CrateTier crateTier, ChestType chestType) {
        Map<Identifier, Material> map = switch (chestType) {
            case SINGLE -> SINGLE;
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
        };

        return map.computeIfAbsent(crateTier.id(), id -> material(id, chestType));
    }

    private static Material material(Identifier identifier, ChestType chestType) {
        String suffix = switch (chestType) {
            case SINGLE -> "";
            case LEFT -> "_left";
            case RIGHT -> "_right";
        };

        return Sheets.CHEST_MAPPER.apply(identifier.withSuffix(suffix));
    }
}
