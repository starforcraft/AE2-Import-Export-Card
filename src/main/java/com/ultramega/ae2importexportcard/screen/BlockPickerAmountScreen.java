package com.ultramega.ae2importexportcard.screen;

import com.ultramega.ae2importexportcard.util.BlockPickerCardConfig;

import java.util.function.IntConsumer;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.me.common.MEStorageMenu;
import appeng.util.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BlockPickerAmountScreen<C extends MEStorageMenu> extends AEBaseScreen<C> {
    private static final String STYLE = "/screens/block_picker_amount.json";
    private static final Identifier BUTTON_SPRITE = Identifier.fromNamespaceAndPath("ae2", "button");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE = Identifier.fromNamespaceAndPath("ae2", "button_highlighted");
    private static final int[] AMOUNT_STEPS = {1, 16, 32};
    private static final int BUTTON_Y_ABOVE = 17;
    private static final int BUTTON_Y_BELOW = 75;
    private static final int BUTTON_WIDTH = 48;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_TEXTURE_HEIGHT = 20;

    private final AEBaseScreen<C> parent;
    private final IntConsumer amountChanged;
    private EditBox amountField;
    private int amount;
    private boolean updatingAmountField;

    public BlockPickerAmountScreen(final AEBaseScreen<C> parent, final int initialAmount, final IntConsumer amountChanged) {
        super(parent.getMenu(), parent.getMenu().getPlayerInventory(), parent.getTitle(), StyleManager.loadStyleDoc(STYLE));
        this.parent = parent;
        this.amount = Mth.clamp(initialAmount, BlockPickerCardConfig.DEFAULT_AMOUNT, BlockPickerCardConfig.MAX_AMOUNT);
        this.amountChanged = amountChanged;
        this.setTextHidden("player_inventory_title", true);

        this.widgets.add("back", new TabButton(Icon.BACK, Component.literal("Back"), button -> this.switchToScreen(this.parent)));
    }

    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        this.amountField = new EditBox(this.font, this.leftPos + 10, this.topPos + 55, 156, 12, this.title);
        this.amountField.setBordered(false);
        this.amountField.setMaxLength(2);
        this.amountField.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        this.amountField.setValue(Integer.toString(this.amount));
        this.amountField.setResponder(this::applyAmountField);
        this.addRenderableWidget(this.amountField);
        this.setInitialFocus(this.amountField);

        for (int index = 0; index < AMOUNT_STEPS.length; index++) {
            final int step = AMOUNT_STEPS[index];
            final int x = this.leftPos + 10 + index * 53;
            this.addRenderableWidget(new AmountButton(x, this.topPos + BUTTON_Y_ABOVE,
                Component.literal("+" + step), button -> this.changeAmount(step)));
            this.addRenderableWidget(new AmountButton(x, this.topPos + BUTTON_Y_BELOW,
                Component.literal("-" + step), button -> this.changeAmount(-step)));
        }
    }

    private void applyAmountField(final String value) {
        if (this.updatingAmountField || value.isEmpty()) {
            return;
        }

        try {
            this.setAmount(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private void changeAmount(final int delta) {
        this.setAmount(this.amount + delta);
    }

    private void setAmount(final int newAmount) {
        final int clampedAmount = Mth.clamp(newAmount, BlockPickerCardConfig.DEFAULT_AMOUNT, BlockPickerCardConfig.MAX_AMOUNT);

        if (!Integer.toString(clampedAmount).equals(this.amountField.getValue())) {
            this.updatingAmountField = true;
            this.amountField.setValue(Integer.toString(clampedAmount));
            this.updatingAmountField = false;
        }

        if (clampedAmount != this.amount) {
            this.amount = clampedAmount;
            this.amountChanged.accept(clampedAmount);
        }
    }

    @Override
    public void onClose() {
        this.switchToScreen(this.parent);
    }

    private static final class AmountButton extends Button {
        private AmountButton(final int x, final int y, final Component message, final OnPress onPress) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, message, onPress, supplier -> Component.empty());
        }

        @Override
        protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
            final int yOffset = this.isHoveredOrFocused() ? 1 : 0;
            final int textureY = this.getY() + (this.getHeight() - BUTTON_TEXTURE_HEIGHT) / 2;

            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE,
                this.getX(),
                textureY,
                this.getWidth(),
                BUTTON_TEXTURE_HEIGHT
            );

            graphics.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.getWidth() / 2,
                textureY + (BUTTON_TEXTURE_HEIGHT - 10) / 2 + yOffset,
                this.active ? 0xFFFFFFFF : 0xFF777777
            );
        }
    }
}
