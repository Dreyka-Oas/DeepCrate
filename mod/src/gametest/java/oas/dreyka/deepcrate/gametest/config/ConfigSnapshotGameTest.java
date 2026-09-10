package oas.dreyka.deepcrate.gametest.config;

import oas.dreyka.deepcrate.config.bounds.ConfigBounds;
import oas.dreyka.deepcrate.config.access.ConfigOption;
import oas.dreyka.deepcrate.config.access.ConfigRuntime;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * What the settings table says about itself, with no packet and no player in the way.
 *
 * Kept apart from {@link ConfigGateGameTest} because these two ask nothing of the network and the
 * split is what holds both files under the line limit. The option named here is one no sibling
 * method moves: a batch runs its methods at the same time over static fields, so a test that reads a
 * value another test is writing fails for somebody else's reason.
 */
public class ConfigSnapshotGameTest {
    /** Bounded and whole, and touched by no other method in this package. */
    private static final String UNTOUCHED = "rowModuleStackLimit";
    private static final String BOUNDED = "baseCapacity";

    @GameTest
    public void anUnknownNameAndAnUnholdableValueLeaveTheTableWhole(GameTestHelper gameTestHelper) {
        // One option rather than the whole table: the sibling methods are moving two others at this
        // very moment, and a comparison of everything would read their work as this method's damage.
        String before = valueOf(gameTestHelper, UNTOUCHED);
        if (ConfigRuntime.set("noSuchOption", "3")) {
            gameTestHelper.fail("a name the schema does not know was accepted");
        }
        if (ConfigRuntime.set(UNTOUCHED, "wide")) {
            gameTestHelper.fail("a word was accepted for an option holding a whole number");
        }

        assertValue(gameTestHelper, UNTOUCHED, before, "after two refused edits");
        gameTestHelper.succeed();
    }

    @GameTest
    public void theSnapshotCarriesEveryOptionInSchemaOrderWithItsRange(GameTestHelper gameTestHelper) {
        List<Field> schema = ConfigSchema.all();
        List<ConfigOption> options = ConfigRuntime.snapshot();
        if (options.size() != schema.size()) {
            gameTestHelper.fail("the snapshot holds " + options.size() + " options against " + schema.size() + " in the schema");
        }

        for (int index = 0; index < schema.size(); index++) {
            String name = schema.get(index).getName();
            ConfigOption configOption = options.get(index);
            if (!name.equals(configOption.name())) {
                gameTestHelper.fail("position " + index + ": the schema says " + name + ", the snapshot says " + configOption.name());
            }
            if (!Objects.equals(ConfigBounds.rangeOf(name), configOption.range())) {
                gameTestHelper.fail("the range of " + name + " did not survive the snapshot: " + configOption.range());
            }
        }

        // The loop above agrees with a bounds table gone empty, so two options are named to hold it.
        if (ConfigBounds.rangeOf(BOUNDED) == null || ConfigBounds.rangeOf("limitAutomationWithLithium") != null) {
            gameTestHelper.fail(BOUNDED + " should carry a range and limitAutomationWithLithium should not");
        }
        gameTestHelper.succeed();
    }

    private static void assertValue(GameTestHelper gameTestHelper, String name, String expected, String what) {
        String actual = valueOf(gameTestHelper, name);
        if (!expected.equals(actual)) {
            gameTestHelper.fail(name + " " + what + ": expected " + expected + ", got " + actual);
        }
    }

    private static String valueOf(GameTestHelper gameTestHelper, String name) {
        for (ConfigOption configOption : ConfigRuntime.snapshot()) {
            if (configOption.name().equals(name)) {
                return configOption.value();
            }
        }
        gameTestHelper.fail("the schema has no option called " + name);
        return "";
    }
}
