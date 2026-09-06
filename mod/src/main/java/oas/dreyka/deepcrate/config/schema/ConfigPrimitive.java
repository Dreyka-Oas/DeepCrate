package oas.dreyka.deepcrate.config.schema;

/**
 * The primitive kinds a config field may have.
 *
 * A field of any other type is not an option at all: it is dropped by the schema, never written and
 * never read back. {@code ConfigSchemaTest} is what stops that dropping from being silent.
 */
public enum ConfigPrimitive {
    BOOL, INT, LONG, DOUBLE, FLOAT;

    /** Null for a type no option may have, which every caller reads as "not an option". */
    public static ConfigPrimitive of(Class<?> type) {
        if (type == boolean.class) {
            return BOOL;
        }
        if (type == int.class) {
            return INT;
        }
        if (type == long.class) {
            return LONG;
        }
        if (type == double.class) {
            return DOUBLE;
        }
        if (type == float.class) {
            return FLOAT;
        }

        return null;
    }
}
