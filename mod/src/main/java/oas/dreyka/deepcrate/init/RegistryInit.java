package oas.dreyka.deepcrate.init;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.api.module.CrateModule;
import oas.dreyka.deepcrate.api.module.CrateModuleSlot;
import oas.dreyka.deepcrate.api.module.RowModule;
import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.inventory.CrateOpenData;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
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

    /** One row of nine slots each. */
    private static final String ROW_MODULE_NAME = "module_row";

    public static final List<CrateTier> TIERS = TIER_SPECS.stream().map(RegistryInit::registerTier).toList();
    public static final List<Item> MODULE_ITEMS = MODULE_SPECS.stream().map(RegistryInit::registerModule).toList();

    public static final Item ROW_MODULE_ITEM = registerRowModule();

    /** The two cells the mod ships, in the order they are drawn. */
    public static final Identifier CAPACITY_SLOT = id("capacity");
    public static final Identifier ROWS_SLOT = id("rows");

    static {
        // The filters ask the module registries rather than a tag of their own, so adding an item to
        // deepcrate:module_512 still makes it placeable with no second data file to write.
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(CAPACITY_SLOT, 0, 1, id("container/slot/module"), itemStack -> DeepCrateApi.moduleFor(itemStack) != null)
        );
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(
                ROWS_SLOT, 1, CrateConfig.rowModuleStackLimit, id("container/slot/row_module"),
                itemStack -> DeepCrateApi.rowModuleFor(itemStack) != null
            )
        );
    }

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
     *
     * That same touch is what reads {@code rowModuleStackLimit}, twice and for good: once for the row
     * cell and once for the item's own stack limit. The settings file has to be loaded before this is
     * called, or the option is frozen at its default while looking like it works.
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
            ResourceKey.create(Registries.ITEM, identifier), Item::new, new Item.Properties().stacksTo(CrateConfig.rowModuleStackLimit)
        );
        DeepCrateApi.registerRowModule(new RowModule(identifier, 1, TagKey.create(Registries.ITEM, identifier)));
        return item;
    }

    /** @param rows rows of nine slots, before any page split */
    private record TierSpec(String name, int rows, MapColor mapColor) {}

    private record ModuleSpec(String name, int capacity) {}
}
