package com.ultramega.ae2importexportcard.util;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;

public final class AEKeyFilterUtil {
    private AEKeyFilterUtil() {
    }

    public static boolean passesFilter(AEKey what, ConfigInventory filterConfig, FuzzyMode fuzzyMode, boolean fuzzy, boolean invertFilter) {
        boolean matchesFilter = filterConfig
            .getAvailableStacks()
            .findFuzzy(what, fuzzyMode)
            .stream()
            .anyMatch(entry -> fuzzy
                ? what.fuzzyEquals(entry.getKey(), fuzzyMode)
                : what.equals(entry.getKey())
            );

        return invertFilter != matchesFilter;
    }
}
