package oas.dreyka.deepcrate.config.schema;

/** Turning what the file says into the type the field has. */
public final class ConfigType {
    private ConfigType() {}

    /** Throws on anything the field's type cannot hold, which the caller turns into a refused line. */
    public static Object parse(Class<?> type, String raw) {
        ConfigPrimitive primitive = ConfigPrimitive.of(type);
        if (primitive == null) {
            throw new IllegalArgumentException("unsupported option type " + type.getSimpleName());
        }

        return switch (primitive) {
            // 0 and 1 as well as the words, because a hand-edited file often carries the number.
            case BOOL -> parseBool(raw);
            case INT -> Integer.parseInt(raw.trim());
            case LONG -> Long.parseLong(raw.trim());
            case DOUBLE -> Double.parseDouble(raw.trim());
            case FLOAT -> Float.parseFloat(raw.trim());
        };
    }

    private static Boolean parseBool(String raw) {
        if (raw.equalsIgnoreCase("true") || raw.equals("1")) {
            return Boolean.TRUE;
        }
        if (raw.equalsIgnoreCase("false") || raw.equals("0")) {
            return Boolean.FALSE;
        }

        throw new IllegalArgumentException("expected true or false, got " + raw);
    }
}
