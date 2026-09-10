package oas.dreyka.deepcrate.config.bounds;

/**
 * The pair of ends an option is clamped to, as the rest of the mod is allowed to see it.
 *
 * {@code ConfigBoundsTable} keeps its own record package-private so nothing can reach into the table
 * and edit a range. A screen still has to say what the accepted span is before the player types a
 * number outside it, so the range travels out as this copy, which is data and nothing else.
 */
public record ConfigRange(double min, double max) {}
