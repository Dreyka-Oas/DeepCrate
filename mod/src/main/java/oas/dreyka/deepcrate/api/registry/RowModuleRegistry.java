package oas.dreyka.deepcrate.api.registry;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.api.Registrations;
import oas.dreyka.deepcrate.api.module.RowModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Row modules: registration and how many rows a stack of them adds to a crate. */
public final class RowModuleRegistry {
    private static final List<RowModule> ROW_MODULES = new ArrayList<>();

    private RowModuleRegistry() {}

    public static RowModule registerRowModule(RowModule rowModule) {
        Registrations.addUnique(ROW_MODULES, rowModule, RowModule::id, "Row module");
        DeepCrate.LOGGER.info("[DeepCrate] row module {}: {} rows each", rowModule.id(), rowModule.rows());
        return rowModule;
    }

    public static List<RowModule> rowModules() {
        return Collections.unmodifiableList(ROW_MODULES);
    }

    public static @Nullable RowModule rowModuleFor(ItemStack itemStack) {
        for (RowModule rowModule : ROW_MODULES) {
            if (rowModule.matches(itemStack)) {
                return rowModule;
            }
        }

        return null;
    }

    /** How many rows a stack sitting in the row slot adds, which is why the stack counts. */
    public static int rowsOf(ItemStack rowModuleStack) {
        RowModule rowModule = rowModuleFor(rowModuleStack);
        return rowModule == null ? 0 : rowModule.rows() * rowModuleStack.getCount();
    }

    /** Rows add up, because each row module is a row and two of them are two rows. */
    public static int rowsAmong(Iterable<ItemStack> stacks) {
        int rows = 0;
        for (ItemStack itemStack : stacks) {
            rows += rowsOf(itemStack);
        }

        return rows;
    }
}
