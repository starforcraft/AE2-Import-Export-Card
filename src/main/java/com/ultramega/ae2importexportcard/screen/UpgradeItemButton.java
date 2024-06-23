package com.ultramega.ae2importexportcard.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class UpgradeItemButton extends IconButton {
    private final ResourceLocation texture;

    public UpgradeItemButton(OnPress onPress, ResourceLocation texture) {
        super(onPress);
        this.texture = texture;
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if(this.visible) {
            if (this.isHalfSize()) {
                this.width = 8;
                this.height = 8;
            }

            var yOffset = isHoveredOrFocused() ? 1 : 0;

            Blitter blitter = Blitter.texture(texture, 16, 16).src(0, 0, width, height);

            if (this.isHalfSize()) {
                if (!isDisableBackground()) {
                    Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).zOffset(10).blit(guiGraphics);
                }

                if (!this.active) {
                    blitter.opacity(0.5f);
                }
                blitter.dest(getX(), getY()).zOffset(20).blit(guiGraphics);
            } else {
                if (!isDisableBackground()) {
                    Icon bgIcon = isHoveredOrFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                            : Icon.TOOLBAR_BUTTON_BACKGROUND;
                    bgIcon.getBlitter()
                            .dest(getX() - 1, getY() + yOffset, 18, 20)
                            .zOffset(2)
                            .blit(guiGraphics);
                }

                blitter.dest(getX(), getY() + 1 + yOffset).zOffset(3).blit(guiGraphics);
            }
        }
    }
}