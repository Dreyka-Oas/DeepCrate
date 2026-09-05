package com.dreykaoas.deepcrate.api;

import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;

/** Adds to a registry list and refuses a name already taken, which is what four registrations do. */
public final class Registrations {
    private Registrations() {}

    /** @param kind what to call the thing in the refusal, so the message names what the caller tried to add */
    public static <T> T addUnique(List<T> list, T entry, Function<T, Identifier> id, String kind) {
        Identifier identifier = id.apply(entry);
        for (T existing : list) {
            if (id.apply(existing).equals(identifier)) {
                throw new IllegalStateException(kind + " " + identifier + " registered twice");
            }
        }

        list.add(entry);
        return entry;
    }
}
