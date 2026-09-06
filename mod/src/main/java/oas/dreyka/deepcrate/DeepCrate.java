package oas.dreyka.deepcrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dependency-free constants and shared logger. Kept apart from {@link DeepCrateMod} so the classes
 * that only need the mod id depend on a leaf rather than on the entry point.
 */
public final class DeepCrate {
    private DeepCrate() {}

    public static final String MOD_ID = "deepcrate";
    public static final Logger LOGGER = LoggerFactory.getLogger("DeepCrate");
}
