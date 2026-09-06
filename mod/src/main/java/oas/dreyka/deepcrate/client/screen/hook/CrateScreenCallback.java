package oas.dreyka.deepcrate.client.screen.hook;

import oas.dreyka.deepcrate.client.screen.DeepCrateScreen;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called once each time a crate screen lays itself out, which is also each time the window is
 * resized. Every widget the screen holds is thrown away between two of those, so a listener adds its
 * own again rather than keeping one across calls.
 */
@FunctionalInterface
public interface CrateScreenCallback {
    Event<CrateScreenCallback> EVENT = EventFactory.createArrayBacked(
        CrateScreenCallback.class,
        listeners -> (screen, area) -> {
            for (CrateScreenCallback listener : listeners) {
                listener.onScreenInit(screen, area);
            }
        }
    );

    void onScreenInit(DeepCrateScreen screen, CrateScreenArea area);
}
