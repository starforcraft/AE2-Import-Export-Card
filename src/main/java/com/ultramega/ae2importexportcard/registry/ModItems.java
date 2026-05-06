package com.ultramega.ae2importexportcard.registry;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;

import appeng.api.upgrades.Upgrades;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2ImportExportCard.MOD_ID);

    public static final DeferredItem<Item> IMPORT_CARD = ITEMS.registerItem(AE2ImportExportCard.IMPORT_CARD_ID, (properties) ->
        Upgrades.createUpgradeCardItem(properties.stacksTo(1)));
    public static final DeferredItem<Item> EXPORT_CARD = ITEMS.registerItem(AE2ImportExportCard.EXPORT_CARD_ID, (properties) ->
        Upgrades.createUpgradeCardItem(properties.stacksTo(1)));
}
