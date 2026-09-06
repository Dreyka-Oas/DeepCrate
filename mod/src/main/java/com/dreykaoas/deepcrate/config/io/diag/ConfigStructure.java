package com.dreykaoas.deepcrate.config.io.diag;

import com.dreykaoas.deepcrate.config.schema.ConfigSchema;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shape of the file, never its values.
 *
 * Anyone may write any number they like, and one outside its range is clamped on the way in. What is
 * looked for here is a file whose keys no longer name the options the mod has: a misspelling, an
 * option filed under two categories at once, a category that does not exist, or a file where nothing
 * at all is recognisable.
 *
 * An option written at the root rather than inside its category is read and not reported. That
 * tolerance is for the administrator who adds a line without wondering which object it belongs under;
 * the rewrite files it where it goes.
 */
public final class ConfigStructure {
    private ConfigStructure() {}

    /**
     * @param root the parsed file, always an object: a root of any other shape throws in the parse and
     *             the loader has already set the file aside
     * @param knownNames every real option name, taken as a parameter so this answers against a small
     *                   fixed set under JUnit
     */
    public static ConfigDrift.Report check(JsonObject root, Set<String> knownNames) {
        Set<String> knownCategories = new HashSet<>();
        for (String name : knownNames) {
            knownCategories.add(ConfigSchema.categoryOf(name));
        }

        Set<String> present = presentOptions(root, knownNames);
        List<ConfigDrift.Unknown> unknown = new ArrayList<>();
        List<String> duplicated = new ArrayList<>();
        List<String> bogusCategory = new ArrayList<>();
        List<String> misplaced = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Unrecognised keys in file order, each with the name to print and the name it may be repaired
        // onto. Split into unknown and renamed only once the whole file has been read, because whether
        // a repair is safe depends on the other keys.
        List<ConfigDrift.Unknown> candidates = new ArrayList<>();
        List<String> targets = new ArrayList<>();
        Map<String, Integer> claims = new HashMap<>();
        int keysInFile = 0;
        int recognised = 0;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                keysInFile++;
                if (!knownNames.contains(entry.getKey())) {
                    RenameRepair.collect(entry.getKey(), knownNames, present, candidates, targets, claims);
                    continue;
                }

                recognised++;
                if (!seen.add(entry.getKey())) {
                    duplicated.add(entry.getKey());
                }

                continue;
            }

            if (!knownCategories.contains(entry.getKey())) {
                bogusCategory.add(entry.getKey());
            }

            for (String name : entry.getValue().getAsJsonObject().keySet()) {
                keysInFile++;
                if (!knownNames.contains(name)) {
                    RenameRepair.collect(name, knownNames, present, candidates, targets, claims);
                    continue;
                }

                recognised++;
                if (!seen.add(name)) {
                    duplicated.add(name);
                } else if (!entry.getKey().equals(ConfigSchema.categoryOf(name)) && knownCategories.contains(entry.getKey())) {
                    // An option inside a category that does not exist is already covered above.
                    misplaced.add(name);
                }
            }
        }

        List<ConfigDrift.Rename> renamed = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            String target = targets.get(i);
            // Two typos meeting on one option are as ambiguous as one typo between two options:
            // neither can be repaired without guessing which line was meant.
            if (target != null && claims.get(target) == 1) {
                renamed.add(new ConfigDrift.Rename(candidates.get(i).name(), target));
            } else {
                unknown.add(candidates.get(i));
            }
        }

        return new ConfigDrift.Report(
            keysInFile, recognised, List.copyOf(unknown), List.copyOf(renamed),
            List.copyOf(duplicated), List.copyOf(bogusCategory), List.copyOf(misplaced)
        );
    }

    /** The real options the file already sets, gathered before anything is classified. */
    private static Set<String> presentOptions(JsonObject root, Set<String> knownNames) {
        Set<String> present = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                if (knownNames.contains(entry.getKey())) {
                    present.add(entry.getKey());
                }

                continue;
            }

            for (String name : entry.getValue().getAsJsonObject().keySet()) {
                if (knownNames.contains(name)) {
                    present.add(name);
                }
            }
        }

        return present;
    }
}
