package com.ultramega.ae2insertexportcard.screen;

import appeng.api.config.IncludeExclude;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.slot.FakeSlot;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import com.ultramega.ae2insertexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2insertexportcard.network.UpgradeUpdateMessage;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class UpgradeScreen extends AEBaseScreen<UpgradeContainerMenu> {
    private static final ResourceLocation CHECKMARK = new ResourceLocation(AE2InsertExportCard.MOD_ID, "textures/gui/checkmark.png");
    private static final ResourceLocation XMARK = new ResourceLocation(AE2InsertExportCard.MOD_ID, "textures/gui/xmark.png");

    private final UpgradeType type;
    private final int[] selectedInventorySlots;

    public UpgradeScreen(UpgradeType type, UpgradeContainerMenu containerMenu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(containerMenu, playerInventory, title, style);
        this.type = type;
        this.selectedInventorySlots = containerMenu.getUpgradeHost().getSelectedInventorySlots();

        AESubScreen.addBackButton(menu, "back", widgets);

        if(type == UpgradeType.INSERT) {
            widgets.add("filter_mode", new IconButton(button -> menu.setFilterMode(menu.getFilterMode() == IncludeExclude.WHITELIST ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST)) {
                @Override
                protected Icon getIcon() {
                    return menu.getFilterMode() == IncludeExclude.WHITELIST ? Icon.WHITELIST : Icon.BLACKLIST;
                }

                @Override
                @NotNull
                public Component getMessage() {
                    return Component.translatable("gui.ae2insertexportcard." + menu.getFilterMode().toString().toLowerCase());
                }
            });
        }
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);

            if (slot instanceof FakeSlot) {
                if (type != UpgradeType.INSERT) {
                    renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, true, i + 1);
                }
            } else if (selectedInventorySlots[i - 18] >= 1) {
                renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, true, selectedInventorySlots[i - 18]);
            } else if (selectedInventorySlots[i - 18] == 0) {
                renderSlotHighlight(graphics, type, font, slot.x + leftPos, slot.y + topPos, false, -1);
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);

        int realSlotId = slotId - 18;
        if(!(slot instanceof FakeSlot) && realSlotId >= 0 && type != ClickType.PICKUP_ALL) {
            if(this.type == UpgradeType.INSERT) {
                selectedInventorySlots[realSlotId] = selectedInventorySlots[realSlotId] == 0 ? 1 : 0;
            } else {
                if (mouseButton == 0) {
                    //Left click
                    if(selectedInventorySlots[realSlotId] >= 18) {
                        selectedInventorySlots[realSlotId] = 0;
                    } else {
                        selectedInventorySlots[realSlotId] += 1;
                    }
                } else {
                    //Right click
                    selectedInventorySlots[realSlotId] = 0;
                }
            }

            sendUpdate();
        }
    }

    public static void renderSlotHighlight(GuiGraphics graphics, UpgradeType type, Font font, int x, int y, boolean checked, int filterIndex) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300.0F);

        if (checked) {
            if (type == UpgradeType.INSERT) {
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
        AE2InsertExportCard.NETWORK_HANDLER.sendToServer(new UpgradeUpdateMessage(type.getId(), selectedInventorySlots));
    }
}
