package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.api.CrateModule;
import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.api.RowModule;
import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.inventory.CrateOpenData;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Everything the mod puts into a registry: six crates, three capacity modules, one row module, one block entity, one menu. */
public final class RegistryInit {
    /**
     * The six tiers, ordered by how dangerous the material is to fetch rather than by the usual
     * iron-gold-diamond ladder. Each one adds a row of nine.
     */
    private static final List<TierSpec> TIER_SPECS = List.of(
        new TierSpec("copper_crate", 3, MapColor.COLOR_ORANGE),
        new TierSpec("iron_crate", 4, MapColor.METAL),
        new TierSpec("amethyst_crate", 5, MapColor.COLOR_PURPLE),
        new TierSpec("prismarine_crate", 6, MapColor.COLOR_CYAN),
        new TierSpec("breeze_crate", 7, MapColor.COLOR_LIGHT_BLUE),
        new TierSpec("echo_crate", 8, MapColor.COLOR_BLACK)
    );

    private static final List<ModuleSpec> MODULE_SPECS = List.of(
        new ModuleSpec("module_128", 128),
        new ModuleSpec("module_256", 256),
        new ModuleSpec("module_512", 512)
    );

    /** One row of nine slots each, sixteen to a crate. */
    private static final String ROW_MODULE_NAME = "module_row";

    public static final List<CrateTier> TIERS = TIER_SPECS.stream().map(RegistryInit::registerTier).toList();
    public static final List<Item> MODULE_ITEMS = MODULE_SPECS.stream().map(RegistryInit::registerModule).toList();

    public static final Item ROW_MODULE_ITEM = registerRowModule();

    public static final BlockEntityType<DeepCrateBlockEntity> BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        id("crate"),
        FabricBlockEntityTypeBuilder.create(DeepCrateBlockEntity::new, TIERS.stream().map(CrateTier::block).toArray(Block[]::new)).build()
    );

    public static final ExtendedScreenHandlerType<DeepCrateMenu, CrateOpenData> MENU = Registry.register(
        BuiltInRegistries.MENU, id("crate"), new ExtendedScreenHandlerType<>(DeepCrateMenu::new, CrateOpenData.STREAM_CODEC)
    );

    private RegistryInit() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DeepCrate.MOD_ID, path);
    }

    /**
     * Touching the class runs its static fields, which is where the registration happens. Java would
     * otherwise defer them until the first read, long after the registries are frozen.
     */
    public static void register() {
        // A tier registered later by an addon has to join the shared block entity type, or the game
        // refuses to attach a block entity to its block.
        DeepCrateApi.onTierRegistered(RegistryInit::adoptNewTiers);
        adoptNewTiers();
        DeepCrate.LOGGER.info("[DeepCrate] {} tiers, {} modules", TIERS.size(), MODULE_ITEMS.size());
    }

    private static void adoptNewTiers() {
        for (CrateTier crateTier : DeepCrateApi.tiers()) {
            BLOCK_ENTITY.addSupportedBlock(crateTier.block());
        }
    }

    private static CrateTier registerTier(TierSpec tierSpec) {
        Block block = Blocks.register(
            ResourceKey.create(Registries.BLOCK, id(tierSpec.name())),
            DeepCrateBlock::new,
            BlockBehaviour.Properties.of().mapColor(tierSpec.mapColor()).strength(3.0F, 6.0F).sound(SoundType.WOOD)
        );
        Items.registerBlock(block);
        return DeepCrateApi.registerTier(new CrateTier(id(tierSpec.name()), tierSpec.rows(), block));
    }

    private static Item registerModule(ModuleSpec moduleSpec) {
        Identifier identifier = id(moduleSpec.name());
        Item item = Items.registerItem(ResourceKey.create(Registries.ITEM, identifier), Item::new, new Item.Properties().stacksTo(16));
        // The module points at a tag of the same name, holding just this item, so another mod can add
        // its own item to it without writing a line of Java.
        DeepCrateApi.registerModule(new CrateModule(identifier, moduleSpec.capacity(), TagKey.create(Registries.ITEM, identifier)));
        return item;
    }

    private static Item registerRowModule() {
        Identifier identifier = id(ROW_MODULE_NAME);
        Item item = Items.registerItem(
            ResourceKey.create(Registries.ITEM, identifier), Item::new, new Item.Properties().stacksTo(RowModule.STACK_LIMIT)
        );
        DeepCrateApi.registerRowModule(new RowModule(identifier, 1, TagKey.create(Registries.ITEM, identifier)));
        return item;
    }

    /** @param rows rows of nine slots, before any page split */
    private record TierSpec(String name, int rows, MapColor mapColor) {}

    private record ModuleSpec(String name, int capacity) {}
}
