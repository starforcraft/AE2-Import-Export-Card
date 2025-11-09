package com.ultramega.ae2importexportcard.registry;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;

import java.util.Locale;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

public final class ModSlotSemantics {
    public static final SlotSemantic IMPORT_CONFIG = SlotSemantics.register(AE2ImportExportCard.MODID.toUpperCase(Locale.ROOT) + "_IMPORT_CONFIG", false);
    public static final SlotSemantic EXPORT_CONFIG = SlotSemantics.register(AE2ImportExportCard.MODID.toUpperCase(Locale.ROOT) + "_EXPORT_CONFIG", false);
}
