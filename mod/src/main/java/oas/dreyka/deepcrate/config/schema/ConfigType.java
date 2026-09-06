package oas.dreyka.deepcrate.config.schema;

/** Turning what the file says into the type the field has. */
public final class ConfigType {
    private ConfigType() {}

    /** Throws on anything the field's type cannot hold, which the caller turns into a refused line. */
    public static Object parse(Class<?> type, String raw) {
        if (type == boolean.class) {
            // 0 and 1 as well as the words, because a hand-edited file often carries the number.
            if (raw.equalsIgnoreCase("true") || raw.equals("1")) {
                return Boolean.TRUE;
            }
            if (raw.equalsIgnoreCase("false") || raw.equals("0")) {
                return Boolean.FALSE;
            }

            throw new IllegalArgumentException("expected true or false, got " + raw);
        }
        if (type == int.class) {
            return Integer.parseInt(raw.trim());
        }
        if (type == long.class) {
            return Long.parseLong(raw.trim());
        }
        if (type == double.class) {
            return Double.parseDouble(raw.trim());
        }
        if (type == float.class) {
            return Float.parseFloat(raw.trim());
        }

        throw new IllegalArgumentException("unsupported option type " + type.getSimpleName());
    }
}
