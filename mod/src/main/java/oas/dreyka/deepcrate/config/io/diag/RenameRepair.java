package oas.dreyka.deepcrate.config.io.diag;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deciding what an unrecognised name was meant to be.
 *
 * One source today, the edit distance, because DeepCrate has never written a settings file and no
 * option name has ever existed on anyone's disk. The day one is renamed, a table of the old name
 * towards the new one is to be added and read here first: it carries a value across a deliberate
 * change of vocabulary, which the distance cannot do, and its entries are then permanent.
 *
 * A candidate is only ever a candidate here. Whether the repair happens is settled once the whole
 * file has been read, because two typos meeting on one option are as ambiguous as one typo between
 * two options.
 */
final class RenameRepair {
    private RenameRepair() {}

    /**
     * Records one unrecognised key: the name to show, and the option its value may move to.
     *
     * @param present the real options the file already sets, which a repair never aims at: a typo
     *                landing on a line the administrator wrote deliberately would overwrite it
     */
    static void collect(
        String name,
        Set<String> knownNames,
        Set<String> present,
        List<ConfigDrift.Unknown> candidates,
        List<String> targets,
        Map<String, Integer> claims
    ) {
        candidates.add(new ConfigDrift.Unknown(name, NameSuggest.suggest(name, knownNames)));

        String target = NameSuggest.suggestUnique(name, knownNames);
        if (target != null && present.contains(target)) {
            target = null;
        }

        targets.add(target);
        if (target != null) {
            claims.merge(target, 1, Integer::sum);
        }
    }
}
