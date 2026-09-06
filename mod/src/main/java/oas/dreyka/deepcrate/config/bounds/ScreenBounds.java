package oas.dreyka.deepcrate.config.bounds;

import oas.dreyka.deepcrate.config.BoundsRegistrar;

/** Clamp ranges for the screen options. */
public final class ScreenBounds {
    private ScreenBounds() {}

    public static void register(BoundsRegistrar registrar) {
        // Under 999 a count of ten is drawn as "0k": below a million the shortening divides by a
        // thousand and keeps no decimal. Integer.MAX_VALUE means never shorten, which is the point of
        // the upper end.
        registrar.b("abbreviateAbove", 999, Integer.MAX_VALUE);
    }
}
