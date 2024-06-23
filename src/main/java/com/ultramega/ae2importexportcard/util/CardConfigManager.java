package com.ultramega.ae2importexportcard.util;

import appeng.api.config.Setting;
import appeng.api.ids.AEComponents;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigManagerBuilder;
import appeng.util.ConfigManager;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface CardConfigManager {
    static IConfigManagerBuilder builder(UpgradeType type, ItemStack gridStack, ItemStack stack) {
        return builder(type, gridStack, () -> stack);
    }

    static IConfigManagerBuilder builder(UpgradeType type, ItemStack gridStack, Supplier<ItemStack> stack) {
        var manager = new ConfigManager((mgr, settingName) -> {
            ItemContainerContents upgrades = gridStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < upgrades.getSlots(); i++) {
                ItemStack stackInSlot = upgrades.getStackInSlot(i);
                if (AE2ImportExportCard.isImportOrExportCard(type, stackInSlot)) {
                    stackInSlot.set(AEComponents.EXPORTED_SETTINGS, mgr.exportSettings());
                }
                stacks.add(stackInSlot);
            }

            gridStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(stacks));
        });

        return new IConfigManagerBuilder() {
            @Override
            public <T extends Enum<T>> IConfigManagerBuilder registerSetting(Setting<T> setting, T defaultValue) {
                manager.registerSetting(setting, defaultValue);
                return this;
            }

            @Override
            public IConfigManager build() {
                manager.importSettings(stack.get().getOrDefault(AEComponents.EXPORTED_SETTINGS, Map.of()));
                return manager;
            }
        };
    }
}
