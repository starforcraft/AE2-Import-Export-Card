package com.ultramega.ae2insertexportcard.util;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;

public final class AEKeyFilterUtil {
    private AEKeyFilterUtil() {
    }

    public static boolean passesFilter(AEKey key, ConfigInventory filterConfig, FuzzyMode fuzzyMode,
            boolean fuzzyInstalled, boolean invertFilter) {
        boolean matches = filterConfig.getAvailableStacks().findFuzzy(key, fuzzyMode).stream()
                .anyMatch(entry -> fuzzyInstalled
                        ? key.fuzzyEquals(entry.getKey(), fuzzyMode)
                        : key.equals(entry.getKey()));
        return invertFilter != matches;
    }
}
