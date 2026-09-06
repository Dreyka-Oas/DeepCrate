package oas.dreyka.deepcrate.config.schema;

import oas.dreyka.deepcrate.config.domain.CrateConfig;
import oas.dreyka.deepcrate.config.domain.ScreenConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Which fields are options, and which category each one is written under.
 *
 * Every public static non-final field of a supported primitive type on a holder is an option, in
 * declaration order, which is also the order it reaches the file inside its category. Adding an
 * option is adding a field, with no second list to keep in step.
 */
public final class ConfigSchema {
    /**
     * Holder to category. Insertion order is the on-disk order, so a holder joins at the end: slipping
     * one into the middle would rewrite every existing file for nothing.
     */
    private static final Map<Class<?>, String> HOLDERS = new LinkedHashMap<>();

    private static volatile @Nullable List<Field> options;

    static {
        registerHolder(CrateConfig.class, "crate");
        registerHolder(ScreenConfig.class, "screen");
    }

    private ConfigSchema() {}

    /**
     * Adds a holder and the category its options are written under, which is what an addon does from
     * {@code onDeepCrateConfig()}.
     *
     * It has to happen before the file is read: the loader drops a name it cannot match and the
     * rewrite that follows deletes the line the administrator wrote.
     */
    public static void registerHolder(Class<?> holder, String category) {
        // Listed twice, every one of its options would be written twice into the file.
        if (HOLDERS.putIfAbsent(holder, category) == null) {
            options = null;
        }
    }

    public static List<Field> all() {
        List<Field> known = options;
        if (known == null) {
            known = Collections.unmodifiableList(scan());
            options = known;
        }

        return known;
    }

    /** Case-insensitive, because a hand-edited file is not held to the letter of the field name. */
    public static @Nullable Field find(String name) {
        for (Field field : all()) {
            if (field.getName().equalsIgnoreCase(name)) {
                return field;
            }
        }

        return null;
    }

    public static String categoryOf(Field option) {
        return HOLDERS.get(option.getDeclaringClass());
    }

    public static @Nullable String categoryOf(String name) {
        Field option = find(name);
        return option == null ? null : categoryOf(option);
    }

    public static boolean isSupported(Class<?> type) {
        return ConfigPrimitive.of(type) != null;
    }

    private static List<Field> scan() {
        List<Field> found = new ArrayList<>();
        for (Class<?> holder : HOLDERS.keySet()) {
            for (Field field : holder.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
                    && isSupported(field.getType())) {
                    found.add(field);
                }
            }
        }

        return found;
    }
}
