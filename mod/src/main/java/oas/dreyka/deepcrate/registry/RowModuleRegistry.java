package oas.dreyka.deepcrate.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.module.RowModule;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Row modules: registration and how many rows a stack of them adds to a crate. */
public final class RowModuleRegistry {
    private static final ModuleList<RowModule> ROW_MODULES = new ModuleList<>(RowModule::id, RowModule::matches, "Row module");

    private RowModuleRegistry() {}

    public static RowModule registerRowModule(RowModule rowModule) {
        RowModule registered = ROW_MODULES.register(rowModule, null);
        DeepCrate.LOGGER.info("[DeepCrate] row module {}: {} rows each", rowModule.id(), rowModule.rows());
        return registered;
    }

    public static List<RowModule> rowModules() {
        return ROW_MODULES.all();
    }

    public static @Nullable RowModule rowModuleFor(ItemStack itemStack) {
        return ROW_MODULES.find(itemStack);
    }

    /** How many rows a stack sitting in the row slot adds, which is why the stack counts. */
    public static int rowsOf(ItemStack rowModuleStack) {
        RowModule rowModule = rowModuleFor(rowModuleStack);
        return rowModule == null ? 0 : rowModule.rows() * rowModuleStack.getCount();
    }

    /** Rows add up, because each row module is a row and two of them are two rows. */
    public static int rowsAmong(Iterable<ItemStack> stacks) {
        return ROW_MODULES.among(stacks, 0, RowModuleRegistry::rowsOf, Integer::sum);
    }
}
