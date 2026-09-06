package oas.dreyka.deepcrate.client.screen.config;

import oas.dreyka.deepcrate.net.ConfigSetPayload;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * What carries a change to the server, and the wait a text field gets before its turn comes.
 *
 * A toggle and a reset are one gesture, so they travel at once. Typing 128 into a field is three
 * gestures, and sending each one would have the server clamp the 1 to the low end of the range and
 * answer with a value nobody asked for. The wait makes the three into one. Closing the screen is
 * what flushes whatever the wait still holds, since a value typed a moment before Escape would
 * otherwise die with the screen.
 */
final class ConfigEdits {
    /** Half a second: long enough for the next digit, short enough that the change feels like it took. */
    private static final int DEBOUNCE_TICKS = 10;

    private final Map<String, Pending> pending = new LinkedHashMap<>();

    /** A row asks before taking a value the server sent, so an answer about an older one is not obeyed. */
    boolean isWaiting(String name) {
        return this.pending.containsKey(name);
    }

    void sendNow(String name, String value) {
        this.pending.remove(name);
        ClientPlayNetworking.send(new ConfigSetPayload(name, value));
    }

    void sendSoon(String name, String value) {
        this.pending.put(name, new Pending(value, DEBOUNCE_TICKS));
    }

    void tick() {
        Iterator<Map.Entry<String, Pending>> iterator = this.pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pending> entry = iterator.next();
            String name = entry.getKey();
            Pending pendingEdit = entry.getValue();
            if (pendingEdit.ticksLeft() > 1) {
                entry.setValue(new Pending(pendingEdit.value(), pendingEdit.ticksLeft() - 1));
            } else {
                iterator.remove();
                ClientPlayNetworking.send(new ConfigSetPayload(name, pendingEdit.value()));
            }
        }
    }

    void flush() {
        for (Map.Entry<String, Pending> entry : this.pending.entrySet()) {
            ClientPlayNetworking.send(new ConfigSetPayload(entry.getKey(), entry.getValue().value()));
        }

        this.pending.clear();
    }

    private record Pending(String value, int ticksLeft) {}
}
