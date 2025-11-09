package com.ultramega.ae2importexportcard.screen;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.container.CardPlayerSlot;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.network.LockSlotUpdateData;
import com.ultramega.ae2importexportcard.network.UpgradeUpdateData;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class UpgradeScreen extends AEBaseScreen<UpgradeContainerMenu> {
    private static final ResourceLocation CHECKMARK = ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/checkmark.png");
    private static final ResourceLocation XMARK = ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/xmark.png");
    private static final ResourceLocation MASS_SELECT = ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/mass_select.png");

    private final UpgradeType type;

    private final SettingToggleButton<FuzzyMode> fuzzyMode;

    private final int[] selectedInventorySlots;
    private boolean cancel = false;
    private boolean dragging = false;
    private int clickedSlotId = -1;

    public UpgradeScreen(UpgradeType type, UpgradeContainerMenu containerMenu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(containerMenu, playerInventory, title, style);
        this.type = type;
        this.selectedInventorySlots = containerMenu.getUpgradeHost().getSelectedInventorySlots();
        this.widgets.add("upgrades", new UpgradesPanel(this.menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));

        AESubScreen.addBackButton(this.menu, "back", this.widgets);

        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.addToLeftToolbar(this.fuzzyMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.fuzzyMode.set(this.menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(this.menu.hasUpgrade(AEItems.FUZZY_CARD));
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);

            int index = i - 18 - (this.type == UpgradeType.EXPORT ? 3 : 2);

            if (slot instanceof FakeSlot) {
                if (this.type != UpgradeType.IMPORT) {
                    renderSlotHighlight(graphics, this.type, this.font, slot.x + this.leftPos, slot.y + this.topPos, true, i - 3 + 1);
                }
            } else if (i >= 18 && this.selectedInventorySlots.length > index) {
                if (this.selectedInventorySlots[index] >= 1) {
                    renderSlotHighlight(graphics, this.type, this.font, slot.x + this.leftPos, slot.y + this.topPos, true, this.selectedInventorySlots[index]);
                } else if (this.selectedInventorySlots[index] == 0) {
                    renderSlotHighlight(graphics, this.type, this.font, slot.x + this.leftPos, slot.y + this.topPos, false, -1);
                }
            }
        }

        renderMassSelect(graphics, this.leftPos + 8 + (16 * 10), this.topPos + 76);
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (itemstack.isEmpty()) {
            Slot slot = this.findSlot(xCoord, yCoord);
            if (slot instanceof CardPlayerSlot) {
                if (hasShiftDown() && !slot.getItem().isEmpty()) {
                    this.cancel = true;
                }
                if (!this.cancel) {
                    this.clickedSlotId = slot.index;
                    PacketDistributor.sendToServer(new LockSlotUpdateData(this.clickedSlotId, true));
                }
            }
        }

        return super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Slot slot = this.findSlot(mouseX, mouseY);
        if (!this.cancel && slot instanceof CardPlayerSlot) {
            ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
            if ((!this.dragging || (slot.getItem().isEmpty() && slot.index == this.clickedSlotId)) && itemstack.isEmpty()) {
                int slotId = slot.index - (18 + (this.type == UpgradeType.EXPORT ? 3 : 2));

                if (button == 0) {
                    this.increaseSelectedInventorySlot(this.type, slotId);
                } else if (button == 1) {
                    this.selectedInventorySlots[slotId] = 0;
                }
                this.sendUpdate();
                this.playClickSound();
            }
        }

        this.cancel = false;
        this.dragging = false;
        this.clickedSlotId = -1;

        // Check mass select buttons
        boolean clickedStorage = this.isHovering(9 + (16 * 10), 77, 4, 5, mouseX, mouseY);
        boolean clickedHotbar = this.isHovering(9 + (16 * 10), 77 + (16 * 3) + 10, 4, 5, mouseX, mouseY);

        if (clickedStorage || clickedHotbar) {
            int start = clickedHotbar ? 0 : 9;
            int end = clickedHotbar ? 9 : 36;

            for (int i = start; i < end; i++) {
                if (button == 0) {
                    this.increaseSelectedInventorySlot(this.type, i);
                } else if (button == 1) {
                    this.selectedInventorySlots[i] = 0;
                }
            }
            this.sendUpdate();
            this.playClickSound();
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void increaseSelectedInventorySlot(UpgradeType type, int index) {
        if (type == UpgradeType.EXPORT) {
            if (this.selectedInventorySlots[index] >= 18) {
                this.selectedInventorySlots[index] = 0;
            } else {
                this.selectedInventorySlots[index] += 1;
            }
        } else {
            this.selectedInventorySlots[index] = this.selectedInventorySlots[index] == 0 ? 1 : 0;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        this.dragging = true;
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (this.clickedSlotId != -1 && itemstack.isEmpty()) {
            this.slotClicked(this.menu.slots.get(this.clickedSlotId), this.clickedSlotId, button, ClickType.PICKUP);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static void renderSlotHighlight(GuiGraphics graphics, UpgradeType type, Font font, int x, int y, boolean checked, int filterIndex) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300.0F);

        if (checked) {
            if (type == UpgradeType.IMPORT) {
                graphics.blit(CHECKMARK, x, y, 0, 0, 16, 16, 16, 16);
            } else {
                graphics.drawString(font, String.valueOf(filterIndex), x + 16 - font.width(String.valueOf(filterIndex)), y, Color.GREEN.hashCode());
            }
        } else {
            graphics.blit(XMARK, x, y, 0, 0, 16, 16, 16, 16);
        }

        graphics.pose().popPose();
    }

    public static void renderMassSelect(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300.0F);

        graphics.blit(MASS_SELECT, x, y, 0, 0, 16, 16, 16, 16);
        graphics.blit(MASS_SELECT, x, y + (16 * 3) + 10, 0, 0, 16, 16, 16, 16);

        graphics.pose().popPose();
    }

    public void sendUpdate() {
        PacketDistributor.sendToServer(new UpgradeUpdateData(this.type.getId(), new IntArrayList(this.selectedInventorySlots)));
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private List<Component> getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(this.menu.getUpgrades().getUpgradableItem()));
        return list;
    }
}
