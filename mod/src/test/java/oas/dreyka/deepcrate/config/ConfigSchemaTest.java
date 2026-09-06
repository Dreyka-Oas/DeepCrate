package oas.dreyka.deepcrate.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.config.domain.ScreenConfig;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigSchemaTest {
    private static final List<Class<?>> HOLDERS = List.of(CrateConfig.class, ScreenConfig.class);

    @Test
    void everyFieldOnAHolderIsAnOption() {
        List<String> options = names(ConfigSchema.all());
        for (Class<?> holder : HOLDERS) {
            for (Field field : holder.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }

                // A field of a type ConfigPrimitive does not know is not an option: it would never be
                // written, never read back, and nothing would say so.
                assertTrue(options.contains(field.getName()), field.getName() + " is declared as an option but of a type the schema drops");
            }
        }
    }

    @Test
    void anOptionIsWrittenUnderTheCategoryOfItsHolder() {
        assertEquals("crate", ConfigSchema.categoryOf("baseCapacity"));
        assertEquals("screen", ConfigSchema.categoryOf("abbreviateAbove"));
    }

    @Test
    void theOrderIsTheOrderTheFieldsAreDeclaredIn() {
        // That order is the order options reach the disk inside their category, so it is not free to
        // move: a shuffle would show a diff on a file nobody edited.
        assertEquals(
            List.of("baseCapacity", "limitAutomationWithLithium", "maxRowsPerPage", "rowModuleStackLimit", "abbreviateAbove"),
            names(ConfigSchema.all())
        );
    }

    @Test
    void anOptionIsFoundWhateverTheCase() {
        assertEquals("baseCapacity", ConfigSchema.find("BASECAPACITY").getName());
    }

    private static List<String> names(List<Field> options) {
        List<String> names = new ArrayList<>();
        for (Field option : options) {
            names.add(option.getName());
        }

        return names;
    }
}
