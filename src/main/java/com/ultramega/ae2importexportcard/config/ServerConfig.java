package com.ultramega.ae2importexportcard.config;

import com.ultramega.ae2importexportcard.compat.appflux.AppFluxBridge;

import java.util.List;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_ENERGY_EXPORT = BUILDER
        .comment("Allow (Applied Flux) energy exports, including terminal charging.")
        .define("allowEnergyExport", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_ITEMS = BUILDER
        .comment("Item registry IDs that cards may not export, regardless of components.")
        .defineListAllowEmpty("blockedItems", List.of(), () -> "minecraft:diamond", ServerConfig::isIdentifier);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_FLUIDS = BUILDER
        .comment("Fluid registry IDs that cards may not export, regardless of components.")
        .defineListAllowEmpty("blockedFluids", List.of(), () -> "minecraft:lava", ServerConfig::isIdentifier);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }

    private static boolean isIdentifier(Object value) {
        return value instanceof String id && id.contains(":") && Identifier.tryParse(id) != null;
    }

    public static boolean allowsExport(AEKey key) {
        if (key instanceof AEItemKey) {
            return !BLOCKED_ITEMS.get().contains(key.getId().toString());
        }
        if (key instanceof AEFluidKey) {
            return !BLOCKED_FLUIDS.get().contains(key.getId().toString());
        }
        return ALLOW_ENERGY_EXPORT.get() || !AppFluxBridge.isFluxKey(key);
    }
}
