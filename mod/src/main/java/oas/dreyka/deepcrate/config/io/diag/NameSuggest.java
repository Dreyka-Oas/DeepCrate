package oas.dreyka.deepcrate.config.io.diag;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * "Did you mean" for a key the schema does not know.
 *
 * Package-private: a suggestion is part of a drift report and never an interface of its own.
 */
final class NameSuggest {
    private NameSuggest() {}

    /**
     * The closest known name inside a budget that grows with length, so {@code baseCapcity} finds
     * {@code baseCapacity} while a name sharing nothing with any option finds nothing. A tie goes to
     * the alphabetically first, so the answer does not move between two runs.
     */
    static @Nullable String suggest(String name, Set<String> knownNames) {
        return best(name, knownNames, false);
    }

    /**
     * The same search, but nothing as soon as two names sit at the same best distance.
     *
     * {@link #suggest} may break a tie because it only ever prints a question and the administrator
     * decides. This one feeds the repair, which rewrites the file without asking: a coin toss between
     * two equally close options would put the written value on the wrong setting, which is worse than
     * dropping it and saying so.
     */
    static @Nullable String suggestUnique(String name, Set<String> knownNames) {
        return best(name, knownNames, true);
    }

    /** Levenshtein distance, two rows, abandoned once a whole row is past the budget. */
    static int distance(String a, String b, int budget) {
        if (Math.abs(a.length() - b.length()) > budget) {
            return budget + 1;
        }

        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            int rowMin = current[0];
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                rowMin = Math.min(rowMin, current[j]);
            }

            // A row entirely past the budget can only grow from here, so no later row brings it back.
            if (rowMin > budget) {
                return budget + 1;
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[b.length()];
    }

    private static @Nullable String best(String name, Set<String> knownNames, boolean requireUnique) {
        int budget = Math.max(2, name.length() / 4);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        int atBestDistance = 0;
        for (String candidate : knownNames) {
            int distance = distance(name, candidate, budget);
            if (distance > budget) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
                atBestDistance = 1;
            } else if (distance == bestDistance) {
                atBestDistance++;
                if (candidate.compareTo(best) < 0) {
                    best = candidate;
                }
            }
        }

        return requireUnique && atBestDistance > 1 ? null : best;
    }
}
