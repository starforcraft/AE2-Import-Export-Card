package com.ultramega.ae2importexportcard.registry;

import appeng.core.AppEng;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static void init() {
        registerMenuType(AE2ImportExportCard.IMPORT_CARD_ID, UpgradeContainerMenu.TYPE_IMPORT);
        registerMenuType(AE2ImportExportCard.EXPORT_CARD_ID, UpgradeContainerMenu.TYPE_EXPORT);
    }

    public static void registerMenuType(String id, MenuType<?> menuType) {
        Registry.register(BuiltInRegistries.MENU, AppEng.makeId(id), menuType);
    }
}
