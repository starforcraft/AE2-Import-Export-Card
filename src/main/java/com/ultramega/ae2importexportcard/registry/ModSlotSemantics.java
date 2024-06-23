package com.ultramega.ae2importexportcard.registry;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;

public final class ModSlotSemantics {
    public static final SlotSemantic IMPORT_CONFIG = SlotSemantics.register(AE2ImportExportCard.MODID.toUpperCase() + "_IMPORT_CONFIG", false);
    public static final SlotSemantic EXPORT_CONFIG = SlotSemantics.register(AE2ImportExportCard.MODID.toUpperCase() + "_EXPORT_CONFIG", false);
}
