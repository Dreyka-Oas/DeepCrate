package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Everything the mod puts into a registry: the crate, its item, its block entity and its menu. */
public final class RegistryInit {
    private RegistryInit() {}

    public static final Identifier CRATE_ID = Identifier.fromNamespaceAndPath(DeepCrate.MOD_ID, "deep_crate");

    public static final Block BLOCK = Blocks.register(
        ResourceKey.create(Registries.BLOCK, CRATE_ID),
        DeepCrateBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F, 6.0F).sound(SoundType.WOOD)
    );

    public static final Item ITEM = Items.registerBlock(BLOCK);

    public static final BlockEntityType<DeepCrateBlockEntity> BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE, CRATE_ID, FabricBlockEntityTypeBuilder.create(DeepCrateBlockEntity::new, BLOCK).build()
    );

    public static final MenuType<DeepCrateMenu> MENU = Registry.register(
        BuiltInRegistries.MENU, CRATE_ID, new MenuType<>(DeepCrateMenu::new, FeatureFlags.VANILLA_SET)
    );

    /**
     * Touching the class runs its static fields, which is where the registration happens. Java would
     * otherwise defer them until the first read, long after the registries are frozen.
     */
    public static void register() {
        DeepCrate.LOGGER.info("[DeepCrate] {} registered, {} items per slot", CRATE_ID, com.dreykaoas.deepcrate.inventory.CrateStorage.SLOT_LIMIT);
    }
}
