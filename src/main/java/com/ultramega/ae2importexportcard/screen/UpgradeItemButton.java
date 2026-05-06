package com.ultramega.ae2importexportcard.screen;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class UpgradeItemButton extends IconButton {
    private final Identifier texture;

    public UpgradeItemButton(OnPress onPress, Identifier texture) {
        super(onPress);
        this.texture = texture;
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.visible) {
            if (this.isHalfSize()) {
                this.width = 8;
                this.height = 8;
            }

            var yOffset = this.isHoveredOrFocused() ? 1 : 0;

            Blitter blitter = Blitter.texture(this.texture, 16, 16).src(0, 0, this.width, this.height);

            if (this.isHalfSize()) {
                if (!this.isDisableBackground()) {
                    Blitter.icon(Icon.TOOLBAR_BUTTON_BACKGROUND).dest(this.getX(), this.getY()).blit(graphics);
                }

                if (!this.active) {
                    blitter.opacity(0.5f);
                }
                blitter.dest(this.getX(), this.getY()).blit(graphics);
            } else {
                if (!this.isDisableBackground()) {
                    Icon bgIcon = this.isHoveredOrFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                            : Icon.TOOLBAR_BUTTON_BACKGROUND;
                    Blitter.icon(bgIcon)
                            .dest(this.getX() - 1, this.getY() + yOffset, 18, 20)
                            .blit(graphics);
                }

                blitter.dest(this.getX(), this.getY() + 1 + yOffset).blit(graphics);
            }
        }
    }
}