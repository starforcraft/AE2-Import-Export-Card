package com.ultramega.ae2importexportcard;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.init.client.InitScreens;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.network.LockSlotUpdateData;
import com.ultramega.ae2importexportcard.network.UpgradeUpdateData;
import com.ultramega.ae2importexportcard.registry.ModCreativeTabs;
import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.registry.ModItems;
import com.ultramega.ae2importexportcard.screen.UpgradeScreen;
import com.ultramega.ae2importexportcard.util.UpgradeType;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AE2ImportExportCard.MODID)
public class AE2ImportExportCard {
    public static final String MODID = "ae2importexportcard";

    public static final String IMPORT_CARD_ID = "import_card";
    public static final String EXPORT_CARD_ID = "export_card";

    public AE2ImportExportCard(IEventBus modEventBus) {
        registerMenus();

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            PayloadRegistrar registrar = event.registrar(MODID);
            registrar.playBidirectional(LockSlotUpdateData.TYPE, LockSlotUpdateData.STREAM_CODEC, LockSlotUpdateData::handle);
            registrar.playBidirectional(UpgradeUpdateData.TYPE, UpgradeUpdateData.STREAM_CODEC, UpgradeUpdateData::handle);
        });

        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::commonSetup);
    }

    @SuppressWarnings("unused")
    static void registerMenus() {
        var a = UpgradeContainerMenu.TYPE_IMPORT;
        var b = UpgradeContainerMenu.TYPE_EXPORT;
    }

    public void registerScreens(final RegisterMenuScreensEvent event) {
        InitScreens.<UpgradeContainerMenu, UpgradeScreen>register(event, UpgradeContainerMenu.TYPE_IMPORT, (menu, inventory, component, style) -> new UpgradeScreen(UpgradeType.IMPORT, menu, inventory, component, style), "/screens/import_card.json");
        InitScreens.<UpgradeContainerMenu, UpgradeScreen>register(event, UpgradeContainerMenu.TYPE_EXPORT, (menu, inventory, component, style) -> new UpgradeScreen(UpgradeType.EXPORT, menu, inventory, component, style), "/screens/export_card.json");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Upgrades.add(ModItems.IMPORT_CARD.get(), AEItems.WIRELESS_TERMINAL, 1);
        Upgrades.add(ModItems.EXPORT_CARD.get(), AEItems.WIRELESS_TERMINAL, 1);
        Upgrades.add(ModItems.IMPORT_CARD.get(), AEItems.WIRELESS_CRAFTING_TERMINAL, 1);
        Upgrades.add(ModItems.EXPORT_CARD.get(), AEItems.WIRELESS_CRAFTING_TERMINAL, 1);
        if(ModList.get().isLoaded("ae2wtlib")) {
            Upgrades.add(ModItems.IMPORT_CARD.get(), AE2wtlibItems.UNIVERSAL_TERMINAL, 1);
            Upgrades.add(ModItems.EXPORT_CARD.get(), AE2wtlibItems.UNIVERSAL_TERMINAL, 1);
        }

        Upgrades.add(AEItems.FUZZY_CARD, ModItems.IMPORT_CARD.get(), 1);
        Upgrades.add(AEItems.FUZZY_CARD, ModItems.EXPORT_CARD.get(), 1);
        Upgrades.add(AEItems.INVERTER_CARD, ModItems.IMPORT_CARD.get(), 1);
        Upgrades.add(AEItems.CRAFTING_CARD, ModItems.EXPORT_CARD.get(), 1);
        Upgrades.add(AEItems.SPEED_CARD, ModItems.EXPORT_CARD.get(), 1);
    }

    public static boolean isImportOrExportCard(UpgradeType type, ItemStack upgrade) {
        return (type == UpgradeType.IMPORT && upgrade.getItem() == ModItems.IMPORT_CARD.get()) ||
                (type == UpgradeType.EXPORT && upgrade.getItem() == ModItems.EXPORT_CARD.get());
    }
}
