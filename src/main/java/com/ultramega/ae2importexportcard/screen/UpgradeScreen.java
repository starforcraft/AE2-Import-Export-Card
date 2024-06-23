package com.ultramega.ae2importexportcard.screen;

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
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.container.CardPlayerSlot;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.network.LockSlotUpdateData;
import com.ultramega.ae2importexportcard.network.UpgradeUpdateData;
import com.ultramega.ae2importexportcard.util.UpgradeType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UpgradeScreen extends AEBaseScreen<UpgradeContainerMenu> {
    private static final ResourceLocation CHECKMARK = ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/checkmark.png");
    private static final ResourceLocation XMARK = ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/xmark.png");

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
        this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));

        AESubScreen.addBackButton(menu, "back", widgets);

        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(this.fuzzyMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.fuzzyMode.set(menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(menu.hasUpgrade(AEItems.FUZZY_CARD));
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);

            int index = i - 18 - (type == UpgradeType.EXPORT ? 3 : 2);

            if (slot instanceof FakeSlot) {
                if (type != UpgradeType.IMPORT) {
                    renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, true, i - 3 + 1);
                }
            } else  if(i >= 18 && selectedInventorySlots.length > index) {
                if (selectedInventorySlots[index] >= 1) {
                    renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, true, selectedInventorySlots[index]);
                } else if (selectedInventorySlots[index] == 0) {
                    renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, false, -1);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if(itemstack.isEmpty()) {
            Slot slot = findSlot(xCoord, yCoord);
            if (slot instanceof CardPlayerSlot) {
                if(hasShiftDown() && !slot.getItem().isEmpty()) {
                    cancel = true;
                }
                if(!cancel) {
                    clickedSlotId = slot.index;
                    PacketDistributor.sendToServer(new LockSlotUpdateData(clickedSlotId, true));
                }
            }
        }

        return super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Slot slot = findSlot(mouseX, mouseY);
        if(!cancel && slot instanceof CardPlayerSlot) {
            ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
            if((!dragging || (slot.getItem().isEmpty() && slot.index == clickedSlotId)) && itemstack.isEmpty()) {
                int slotId = slot.index - (18 + (type == UpgradeType.EXPORT ? 3 : 2));

                if (this.type == UpgradeType.IMPORT) {
                    selectedInventorySlots[slotId] = selectedInventorySlots[slotId] == 0 ? 1 : 0;
                } else {
                    if (button == 0) {
                        //Left click
                        if (selectedInventorySlots[slotId] >= 18) {
                            selectedInventorySlots[slotId] = 0;
                        } else {
                            selectedInventorySlots[slotId] += 1;
                        }
                    } else {
                        //Right click
                        selectedInventorySlots[slotId] = 0;
                    }
                }

                sendUpdate();
            }
        }

        cancel = false;
        dragging = false;
        clickedSlotId = -1;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        dragging = true;
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if(clickedSlotId != -1 && itemstack.isEmpty()) {
            slotClicked(this.menu.slots.get(clickedSlotId), clickedSlotId, button, ClickType.PICKUP);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static void renderSlotHighlight(GuiGraphics graphics, UpgradeType type, Font font, int x, int y, boolean checked, int filterIndex) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300.0F);

        if (checked) {
            if (type == UpgradeType.IMPORT) {
                graphics.blit(CHECKMARK, x + 7, y, 0, 0, 9, 8, 9, 8);
            } else {
                graphics.drawString(font, String.valueOf(filterIndex), x + 16 - font.width(String.valueOf(filterIndex)), y, Color.GREEN.hashCode());
            }
        } else {
            graphics.blit(XMARK, x + 9, y, 0, 0, 7, 7, 7, 7);
        }

        graphics.pose().popPose();
    }

    public void sendUpdate() {
        PacketDistributor.sendToServer(new UpgradeUpdateData(type.getId(), new IntArrayList(selectedInventorySlots)));
    }

    private List<Component> getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(menu.getUpgrades().getUpgradableItem()));
        return list;
    }
}
