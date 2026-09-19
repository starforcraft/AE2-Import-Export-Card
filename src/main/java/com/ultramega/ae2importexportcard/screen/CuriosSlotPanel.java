package com.ultramega.ae2importexportcard.screen;

import com.ultramega.ae2importexportcard.compat.curios.CuriosBridge;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.network.CurioSlotUpdateData;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

public final class CuriosSlotPanel {
    private static final ResourceLocation INVENTORY = makeId("textures/gui/curios/inventory.png");
    private static final int MAX_ROWS = 8;
    private static final int MAX_PAGE_SIZE = 48;

    private final UpgradeContainerMenu menu;
    private final Map<String, Integer> selected;

    private List<CuriosBridge.Slot> slots = List.of();

    private int pageSize = MAX_PAGE_SIZE;
    private int columns = 1;
    private int panelWidth;
    private int panelHeight;
    private int slotTop;
    private int visibleSlots;
    private boolean open;
    private int page;
    private int x;
    private int y;

    public CuriosSlotPanel(UpgradeContainerMenu menu) {
        this.menu = menu;
        this.selected = new HashMap<>(menu.getUpgradeHost().getSelectedCurioSlots());
    }

    public void render(GuiGraphics graphics, Font font, int left, int top, int mouseX, int mouseY) {
        if (!this.open) {
            return;
        }
        this.slots = CuriosBridge.getSlots(Minecraft.getInstance().player);
        if (this.slots.isEmpty()) {
            return;
        }

        int availableColumns = Math.max(1, (left - 22 - 2) / 18);
        this.pageSize = Math.min(MAX_PAGE_SIZE, availableColumns * MAX_ROWS);
        this.page = Math.min(this.page, this.lastPage());
        this.visibleSlots = Math.min(this.pageSize, this.slots.size() - this.page * this.pageSize);
        this.columns = Math.max(1, (this.visibleSlots + MAX_ROWS - 1) / MAX_ROWS);
        int rows = Math.max(1, (this.visibleSlots + this.columns - 1) / this.columns);
        this.panelWidth = 2 + this.columns * 18;
        this.slotTop = this.lastPage() > 0 ? 14 : 0;
        this.panelHeight = this.slotTop + rows * 18 + 4;
        this.x = left - 1 - this.panelWidth;
        this.y = Math.clamp(top, 2, Minecraft.getInstance().getWindow().getGuiScaledHeight() - this.panelHeight - 2);
        this.selected.values().removeIf(filter -> filter > this.menu.getUpgradeHost().getActiveFilterSlotCount());

        this.drawSlotColumns(graphics);
        for (int cell = 0; cell < this.visibleSlots; cell++) {
            int index = this.page * this.pageSize + cell;
            if (index >= this.slots.size()) {
                break;
            }
            CuriosBridge.Slot slot = this.slots.get(index);
            int sx = this.x + 2 + cell % this.columns * 18;
            int sy = this.y + this.slotTop + 2 + cell / this.columns * 18;
            if (slot.stack().isEmpty() && slot.icon() != null) {
                graphics.blit(sx, sy, 0, 16, 16, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(slot.icon()));
            } else {
                graphics.renderItem(slot.stack(), sx, sy);
            }
            int filter = this.selected.getOrDefault(slot.key(), 0);
            UpgradeScreen.renderSlotHighlight(graphics, UpgradeType.EXPORT, font, sx, sy, filter > 0, filter);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                var name = Component.translatableWithFallback("curios.identifier." + slot.identifier(), slot.identifier());
                List<Component> lines = new ArrayList<>();
                lines.add(name);
                if (!slot.stack().isEmpty()) {
                    lines.add(slot.stack().getHoverName());
                }
                graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
        if (this.lastPage() > 0) {
            this.drawPageButton(graphics, false, mouseX, mouseY);
            this.drawPageButton(graphics, true, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean carrying) {
        if (carrying || (button != 0 && button != 1)) {
            return false;
        }
        if (this.lastPage() > 0 && button == 0) {
            if (this.overPageButton(mouseX, mouseY, false) && this.page > 0) {
                this.page--;
                return true;
            }
            if (this.overPageButton(mouseX, mouseY, true) && this.page < this.lastPage()) {
                this.page++;
                return true;
            }
        }

        int rx = (int) mouseX - this.x - 2;
        int ry = (int) mouseY - this.y - this.slotTop - 2;
        if (rx >= 0 && ry >= 0 && rx < this.columns * 18 && rx % 18 < 16 && ry % 18 < 16) {
            int cell = ry / 18 * this.columns + rx / 18;
            int index = this.page * this.pageSize + cell;
            if (cell < this.visibleSlots && index < this.slots.size()) {
                String key = this.slots.get(index).key();
                int filter = this.selected.getOrDefault(key, 0);
                filter = button == 1 || filter >= this.menu.getUpgradeHost().getActiveFilterSlotCount() ? 0 : filter + 1;
                if (filter == 0) {
                    this.selected.remove(key);
                } else {
                    this.selected.put(key, filter);
                }
                PacketDistributor.sendToServer(new CurioSlotUpdateData(this.menu.containerId, key, filter));
                return true;
            }
        }

        return false;
    }

    private void drawSlotColumns(GuiGraphics graphics) {
        for (int cell = 0; cell < this.visibleSlots; cell++) {
            int column = cell % this.columns;
            int row = cell / this.columns;
            int rowColumns = Math.min(this.columns, this.visibleSlots - row * this.columns);
            boolean rightEdge = column == rowColumns - 1;
            boolean bottomEdge = cell + this.columns >= this.visibleSlots;
            int sx = this.x + column * 18;
            int sy = this.y + this.slotTop + row * 18;

            // Atlas columns: left edge at 0, interior at 18, right edge at 36
            // Each slice owns its leading two-pixel separator and 16-pixel item area
            int u = column == 0 ? 0 : 18;
            int v = row == 0 ? 0 : 18;
            this.blit(graphics, sx, sy, u, v, 18, 18);
            if (rightEdge) {
                // Append only the outer rim, also covering the single-column case
                this.blit(graphics, sx + 18, sy, 54, v, 2, 18);
            }
            if (bottomEdge) {
                // Cap only exposed bottom edges, never the gray seam between rows
                this.blit(graphics, sx, sy + 18, u, 144, 18, 4);
                if (rightEdge) {
                    this.blit(graphics, sx + 18, sy + 18, 54, 144, 2, 4);
                }
            }
        }
    }

    ImageButton createButton(int left, int top) {
        WidgetSprites sprites = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("curios", "button"),
            ResourceLocation.fromNamespaceAndPath("curios", "button_highlighted")
        );
        return new ImageButton(left + 4, top - 13, 10, 10, sprites, ignored -> this.open = !this.open) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                graphics.blitSprite(this.sprites.get(this.isActive(), this.isHovered()),
                    this.getX(), this.getY(), this.getWidth(), this.getHeight());
            }
        };
    }

    public boolean contains(double mouseX, double mouseY) {
        if (!this.open) {
            return false;
        }
        if (this.lastPage() > 0 && (this.overPageButton(mouseX, mouseY, false) || this.overPageButton(mouseX, mouseY, true))) {
            return true;
        }
        for (int column = 0; column < this.columns; column++) {
            int rows = this.rowsInColumn(column);
            int cx = this.x + column * 18;
            if (rows > 0 && mouseX >= cx && mouseX < cx + 20
                && mouseY >= this.y + this.slotTop && mouseY < this.y + this.slotTop + rows * 18 + 4) {
                return true;
            }
        }
        return false;
    }

    private int rowsInColumn(int column) {
        return Math.max(0, (this.visibleSlots - column + this.columns - 1) / this.columns);
    }

    private int pageButtonX(boolean next) {
        return this.x + this.panelWidth - (next ? 11 : 22);
    }

    private boolean overPageButton(double mouseX, double mouseY, boolean next) {
        int bx = this.pageButtonX(next);
        return mouseX >= bx && mouseX < bx + 11 && mouseY >= this.y && mouseY < this.y + 14;
    }

    private void drawPageButton(GuiGraphics graphics, boolean next, int mouseX, int mouseY) {
        boolean enabled = next ? this.page < this.lastPage() : this.page > 0;
        boolean hovered = this.overPageButton(mouseX, mouseY, next);
        int u = (next ? 73 : 62) + (enabled && hovered ? 22 : 0);
        this.blit(graphics, this.pageButtonX(next), this.y, u, enabled ? 25 : 39, 11, 14);
        if (hovered) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.curios.page", this.page + 1, this.lastPage() + 1), mouseX, mouseY);
        }
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(INVENTORY, x, y, u, v, width, height, 256, 256);
    }

    private int lastPage() {
        return Math.max(0, (this.slots.size() - 1) / this.pageSize);
    }
}
