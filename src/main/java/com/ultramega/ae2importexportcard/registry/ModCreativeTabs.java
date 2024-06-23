package com.ultramega.ae2importexportcard.registry;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AE2ImportExportCard.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_AE2IMPORTEXPORTCARD = CREATIVE_MODE_TABS.register(AE2ImportExportCard.MODID + "_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AE2ImportExportCard.MODID))
            .icon(() -> ModItems.IMPORT_CARD.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.IMPORT_CARD.get());
                output.accept(ModItems.EXPORT_CARD.get());
            }).build());
}