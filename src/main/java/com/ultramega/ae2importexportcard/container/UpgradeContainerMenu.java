package com.ultramega.ae2importexportcard.container;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.item.UpgradeHost;
import com.ultramega.ae2importexportcard.registry.ModSlotSemantics;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.storage.ISubMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import com.google.common.base.Preconditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ItemLike;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.MODID;

public class UpgradeContainerMenu extends AEBaseMenu implements ISubMenu {
    public static final MenuType<UpgradeContainerMenu> TYPE_IMPORT = MenuTypeBuilder.create((id, inventory, host) -> new UpgradeContainerMenu(UpgradeType.IMPORT, id, inventory, host, new UpgradeHost(UpgradeType.IMPORT, id, inventory, host)), WirelessTerminalMenuHost.class)
            .build(ResourceLocation.fromNamespaceAndPath(MODID, AE2ImportExportCard.IMPORT_CARD_ID));
    public static final MenuType<UpgradeContainerMenu> TYPE_EXPORT = MenuTypeBuilder.create((id, inventory, host) -> new UpgradeContainerMenu(UpgradeType.EXPORT, id, inventory, host, new UpgradeHost(UpgradeType.EXPORT, id, inventory, host)), WirelessTerminalMenuHost.class)
            .build(ResourceLocation.fromNamespaceAndPath(MODID, AE2ImportExportCard.EXPORT_CARD_ID));

    private final WirelessTerminalMenuHost<?> host;
    private final UpgradeHost upgradeHost;

    @GuiSync(0)
    public FuzzyMode fzMode = FuzzyMode.IGNORE_ALL;

    public UpgradeContainerMenu(UpgradeType type, int id, Inventory playerInventory, WirelessTerminalMenuHost host, UpgradeHost upgradeHost) {
        super(type == UpgradeType.IMPORT ? TYPE_IMPORT : TYPE_EXPORT, id, playerInventory, host);
        this.host = host;
        this.upgradeHost = upgradeHost;

        this.setupUpgrades(this.getUpgradeHost().getUpgrades());

        this.addConfigSlots(upgradeHost.filterConfig, type == UpgradeType.IMPORT ? ModSlotSemantics.IMPORT_CONFIG : ModSlotSemantics.EXPORT_CONFIG);
        this.createCardPlayerInventorySlots(playerInventory);
    }

    private void addConfigSlots(ConfigInventory config, SlotSemantic slotSemantic) {
        ConfigMenuInventory inv = config.createMenuWrapper();

        for (int i = 0; i < 18; ++i) {
            this.addSlot(new FakeSlot(inv, i), slotSemantic);
        }
    }

    private void createCardPlayerInventorySlots(Inventory playerInventory) {
        Preconditions.checkState(this.getSlots(SlotSemantics.PLAYER_INVENTORY).isEmpty(), "Player inventory was already created");

        for (int i = 0; i < playerInventory.items.size(); ++i) {
            CardPlayerSlot slot = new CardPlayerSlot(playerInventory, i);

            SlotSemantic s = i < Inventory.getSelectionSize() ? SlotSemantics.PLAYER_HOTBAR : SlotSemantics.PLAYER_INVENTORY;
            this.addSlot(slot, s);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (this.isServerSide()) {
            if (this.getUpgradeHost().getUpgrades().isInstalled(AEItems.FUZZY_CARD)) {
                this.setFuzzyMode(this.getUpgradeHost().getConfigManager().getSetting(Settings.FUZZY_MODE));
            }
        }
    }

    @Override
    public ISubMenuHost getHost() {
        return this.host;
    }

    public UpgradeHost getUpgradeHost() {
        return this.upgradeHost;
    }

    @Override
    public Object getTarget() {
        return this.upgradeHost;
    }

    public FuzzyMode getFuzzyMode() {
        return this.fzMode;
    }

    public void setFuzzyMode(FuzzyMode fzMode) {
        this.fzMode = fzMode;
    }

    public final IUpgradeInventory getUpgrades() {
        return this.getUpgradeHost().getUpgrades();
    }

    public final boolean hasUpgrade(ItemLike upgradeCard) {
        return this.getUpgrades().isInstalled(upgradeCard);
    }
}
