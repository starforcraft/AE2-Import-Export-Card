package com.ultramega.ae2insertexportcard.screen;

import appeng.client.gui.widgets.ITooltip;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class UpgradeItemButton extends Button implements ITooltip {
    private final ResourceLocation texture;
    public static final ResourceLocation TEXTURE_STATES = new ResourceLocation("ae2", "textures/guis/states.png");

    public UpgradeItemButton(OnPress onPress, ResourceLocation texture) {
        super(0, 0, 16, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.texture = texture;
    }

    public void setVisibility(final boolean vis) {
        visible = vis;
        active = vis;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, final int mouseX, final int mouseY, float partial) {
        if (!visible)
            return;
        graphics.pose().pushPose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        if (isFocused()) {
            graphics.fill(getX() - 1, getY() - 1, getX() + 17, getY() + 17, 0xFFFFFFFF);
        }
        graphics.blit(TEXTURE_STATES, getX(), getY(), 16, 16, 240, 240, 16, 16, 256, 256);
        if (active)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        else
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
        graphics.blit(texture, getX(), getY(), 16, 16, 0, 0, 512, 512, 512, 512);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    @Override
    public @NotNull List<Component> getTooltipMessage() {
        return Collections.singletonList(getMessage());
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), 16, 16);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}