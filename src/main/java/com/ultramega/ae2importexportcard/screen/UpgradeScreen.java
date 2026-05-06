package com.ultramega.ae2importexportcard.screen;

import com.ultramega.ae2importexportcard.container.CardPlayerSlot;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

public class UpgradeScreen extends AEBaseScreen<UpgradeContainerMenu> {
    private static final Identifier CHECKMARK = makeId("textures/gui/checkmark.png");
    private static final Identifier XMARK = makeId("textures/gui/xmark.png");
    private static final Identifier MASS_SELECT = makeId("textures/gui/mass_select.png");

    private final UpgradeType type;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final int[] selectedInventorySlots;

    private boolean cancel = false;
    private boolean dragging = false;
    private boolean pickedUpDraggedStack = false;
    private boolean suppressReleaseAfterDrag = false;
    private boolean blockedQuickCraftDrag = false;
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
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractContents(graphics, mouseX, mouseY, a);

        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            final int x = this.leftPos + slot.x;
            final int y = this.topPos + slot.y;

            if (slot instanceof FakeSlot) {
                if (this.type != UpgradeType.IMPORT) {
                    drawSlotHighlight(graphics, this.type, this.font, x, y, true, i - 3 + 1);
                }
                continue;
            }

            if (!(slot instanceof CardPlayerSlot)) {
                continue;
            }

            int index = slot.getContainerSlot();
            if (index < 0 || index >= this.selectedInventorySlots.length) {
                continue;
            }

            int selectedSlot = this.selectedInventorySlots[index];
            if (selectedSlot >= 1) {
                drawSlotHighlight(graphics, this.type, this.font, x, y, true, selectedSlot);
            } else if (selectedSlot == 0) {
                drawSlotHighlight(graphics, this.type, this.font, x, y, false, -1);
            }
        }

        drawMassSelect(graphics, this.leftPos + 23 + (16 * 10), this.topPos + 76);
    }

    public static void drawSlotHighlight(GuiGraphicsExtractor graphics, UpgradeType type, Font font, int x, int y, boolean checked, int filterIndex) {
        Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();

        if (checked) {
            if (type == UpgradeType.IMPORT) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, CHECKMARK, x, y, 0, 0, 16, 16, 16, 16);
            } else {
                poseStack.pushMatrix();
                poseStack.scale(0.5F, 0.5F);

                String text = String.valueOf(filterIndex);
                graphics.text(font, text, (x + 16) * 2 - font.width(text), y * 2, Color.GREEN.hashCode());

                poseStack.popMatrix();
            }
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, XMARK, x, y, 0, 0, 16, 16, 16, 16);
        }

        poseStack.popMatrix();
    }

    public static void drawMassSelect(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.pose().pushMatrix();

        graphics.blit(RenderPipelines.GUI_TEXTURED, MASS_SELECT, x, y, 0, 0, 16, 16, 16, 16);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MASS_SELECT, x, y + (16 * 3) + 10, 0, 0, 16, 16, 16, 16);

        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        ItemStack carried = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (carried.isEmpty()) {
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            if (slot instanceof CardPlayerSlot) {
                // Let vanilla handle shift-clicks normally
                if (Minecraft.getInstance().hasShiftDown() && !slot.getItem().isEmpty()) {
                    return super.mouseClicked(event, doubleClick);
                }

                this.cancel = false;
                this.dragging = false;
                this.pickedUpDraggedStack = false;
                this.clickedSlotId = slot.index;

                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        this.dragging = true;

        ItemStack carried = this.getCarriedOrDragged();
        if (!carried.isEmpty()) {
            this.stopQuickCrafting();
            this.blockedQuickCraftDrag = true;
            this.suppressReleaseAfterDrag = true;

            return true;
        }

        if (this.clickedSlotId != -1 && !this.pickedUpDraggedStack) {
            if (carried.isEmpty()) {
                this.slotClicked(this.menu.slots.get(this.clickedSlotId), this.clickedSlotId, event.button(), ContainerInput.PICKUP);
                this.pickedUpDraggedStack = true;
                this.suppressReleaseAfterDrag = true;
                this.stopQuickCrafting();

                return true;
            }
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        boolean handled = false;

        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack carried = this.getCarriedOrDragged();

        boolean suppressThisRelease = this.suppressReleaseAfterDrag || this.pickedUpDraggedStack || this.blockedQuickCraftDrag;
        if (!suppressThisRelease && !this.cancel && !this.dragging && slot instanceof CardPlayerSlot && slot.index == this.clickedSlotId) {
            if (carried.isEmpty()) {
                int slotId = slot.getContainerSlot();
                if (slotId >= 0 && slotId < this.selectedInventorySlots.length) {
                    if (event.button() == 0) {
                        this.increaseSelectedInventorySlot(this.type, slotId);
                        handled = true;
                    } else if (event.button() == 1) {
                        this.selectedInventorySlots[slotId] = 0;
                        handled = true;
                    }

                    if (handled) {
                        this.sendUpdate();
                        this.playClickSound();
                    }
                }
            }
        }

        this.stopQuickCrafting();

        this.cancel = false;
        this.dragging = false;
        this.pickedUpDraggedStack = false;
        this.suppressReleaseAfterDrag = false;
        this.blockedQuickCraftDrag = false;
        this.clickedSlotId = -1;

        // If the user dragged an item, consume the release
        // This keeps the item on the cursor and requires a new click
        if (suppressThisRelease) {
            return true;
        }

        // Check mass select buttons
        boolean clickedInv = this.isHovering(24 + (16 * 10), 77, 4, 5, event.x(), event.y());
        boolean clickedHotbar = this.isHovering(24 + (16 * 10), 77 + (16 * 3) + 10, 4, 5, event.x(), event.y());

        if (clickedInv || clickedHotbar) {
            int start = clickedHotbar ? 0 : 9;
            int end = clickedHotbar ? 9 : 36;

            for (int i = start; i < end; i++) {
                if (event.button() == 0) {
                    this.increaseSelectedInventorySlot(this.type, i);
                } else if (event.button() == 1) {
                    this.selectedInventorySlots[i] = 0;
                }
            }

            this.sendUpdate();
            this.playClickSound();

            handled = true;
        }

        return handled || super.mouseReleased(event);
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

    public void sendUpdate() {
        ClientPacketDistributor.sendToServer(new UpgradeUpdateData(this.type.getId(), new IntArrayList(this.selectedInventorySlots)));
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

    private void stopQuickCrafting() {
        this.isQuickCrafting = false;
        this.quickCraftSlots.clear();
    }

    private ItemStack getCarriedOrDragged() {
        return this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
    }
}
