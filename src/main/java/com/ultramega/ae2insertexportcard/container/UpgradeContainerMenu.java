package com.ultramega.ae2insertexportcard.container;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.ISubMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import com.ultramega.ae2insertexportcard.item.UpgradeHost;
import com.ultramega.ae2insertexportcard.registry.ModSlotSemantics;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class UpgradeContainerMenu extends AEBaseMenu implements ISubMenu {
    public static final String INSERT_CARD_ID = "insert_card";
    public static final String EXPORT_CARD_ID = "export_card";
    public static final MenuType<UpgradeContainerMenu> TYPE_INSERT = MenuTypeBuilder.create((id, inventory, host) -> new UpgradeContainerMenu(UpgradeType.INSERT, id, inventory, host, new UpgradeHost(UpgradeType.INSERT, id, inventory, host)), WirelessTerminalMenuHost.class)
            .build(INSERT_CARD_ID);
    public static final MenuType<UpgradeContainerMenu> TYPE_EXPORT = MenuTypeBuilder.create((id, inventory, host) -> new UpgradeContainerMenu(UpgradeType.EXPORT, id, inventory, host, new UpgradeHost(UpgradeType.EXPORT, id, inventory, host)), WirelessTerminalMenuHost.class)
            .build(EXPORT_CARD_ID);

    private final static String SET_FILTER_MODE = "setFilterMode";

    private final UpgradeType type;
    private final WirelessTerminalMenuHost host;
    private final UpgradeHost upgradeHost;

    private IncludeExclude currentFilterMode;
    @GuiSync(97)
    private IncludeExclude filterMode;

    public UpgradeContainerMenu(UpgradeType type, int id, Inventory playerInventory, WirelessTerminalMenuHost host, UpgradeHost upgradeHost) {
        super(type == UpgradeType.INSERT ? TYPE_INSERT : TYPE_EXPORT, id, playerInventory, host);
        this.type = type;
        this.host = host;
        this.upgradeHost = upgradeHost;
        this.filterMode = upgradeHost.getFilterMode();

        for(int i = 0; i < playerInventory.items.size(); i++) {
            lockPlayerInventorySlot(i);
        }

        addConfigSlots(upgradeHost.filterConfig, type == UpgradeType.INSERT ? ModSlotSemantics.INSERT_CONFIG : ModSlotSemantics.EXPORT_CONFIG);
        createPlayerInventorySlots(playerInventory);
        registerClientAction(SET_FILTER_MODE, IncludeExclude.class, this::setFilterMode);
    }

    private void addConfigSlots(ConfigInventory config, SlotSemantic slotSemantic) {
        ConfigMenuInventory inv = config.createMenuWrapper();

        for (int i = 0; i < 18; ++i) {
            addSlot(new FakeSlot(inv, i), slotSemantic);
        }
    }

    @Override
    public void onServerDataSync() {
        super.onServerDataSync();

        if (this.currentFilterMode != this.filterMode) {
            this.setFilterMode(this.filterMode);
            this.currentFilterMode = filterMode;
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (isServerSide()) {
            if (this.filterMode != getFilterMode()) {
                this.setFilterMode(getFilterMode());
            }
        }
    }

    public void setFilterMode(IncludeExclude filterMode) {
        if (isClientSide()) {
            sendClientAction(SET_FILTER_MODE, filterMode);
        } else {
            this.filterMode = filterMode;
            getUpgradeHost().toggleFilterMode(filterMode);
        }
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }

    public UpgradeHost getUpgradeHost() {
        return upgradeHost;
    }

    public IncludeExclude getFilterMode() {
        return filterMode;
    }
}
