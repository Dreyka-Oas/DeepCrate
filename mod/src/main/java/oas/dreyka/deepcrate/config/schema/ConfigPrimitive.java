package oas.dreyka.deepcrate.config.schema;

/**
 * The primitive kinds a config field may have.
 *
 * A field of any other type is not an option at all: it is dropped by the schema, never written and
 * never read back. {@code ConfigSchemaTest} is what stops that dropping from being silent.
 */
public enum ConfigPrimitive {
    BOOL(boolean.class), INT(int.class), LONG(long.class), DOUBLE(double.class), FLOAT(float.class);

    private final Class<?> type;

    ConfigPrimitive(Class<?> type) {
        this.type = type;
    }

    /**
     * Null for a type no option may have, which every caller reads as "not an option".
     *
     * The pairing above is the one place the five kinds and their types are matched up; parsing,
     * writing and bounding all classify a field by calling this rather than repeating the match.
     */
    public static ConfigPrimitive of(Class<?> type) {
        for (ConfigPrimitive primitive : values()) {
            if (primitive.type == type) {
                return primitive;
            }
        }

        return null;
    }
}
