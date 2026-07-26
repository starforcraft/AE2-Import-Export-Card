package com.ultramega.ae2insertexportcard.network;

import com.ultramega.ae2insertexportcard.container.UpgradeContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeUpdateMessage {
    private static final int INVENTORY_SLOT_COUNT = 36;

    private final int type;
    private final int[] selectedInventorySlots;

    public UpgradeUpdateMessage(int type, int[] selectedInventorySlots) {
        this.type = type;
        this.selectedInventorySlots = selectedInventorySlots;
    }

    public static UpgradeUpdateMessage decode(FriendlyByteBuf buf) {
        return new UpgradeUpdateMessage(buf.readInt(), buf.readVarIntArray(INVENTORY_SLOT_COUNT));
    }

    public static void encode(UpgradeUpdateMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.type);
        buf.writeVarIntArray(message.selectedInventorySlots);
    }

    public static void handle(UpgradeUpdateMessage message, Supplier<NetworkEvent.Context> ctx) {
        Player player = ctx.get().getSender();
        if (player != null) {
            ctx.get().enqueueWork(() -> {
                if (player.containerMenu instanceof UpgradeContainerMenu containerMenu
                        && isValid(message, containerMenu)) {
                    containerMenu.getUpgradeHost().setSelectedInventorySlots(message.selectedInventorySlots);
                }
            });
        }

        ctx.get().setPacketHandled(true);
    }

    private static boolean isValid(UpgradeUpdateMessage message, UpgradeContainerMenu menu) {
        if (message.type != menu.getUpgradeType().getId()
                || message.selectedInventorySlots.length != INVENTORY_SLOT_COUNT) {
            return false;
        }

        int maximum = message.type == 0 ? 1 : 18;
        for (int selection : message.selectedInventorySlots) {
            if (selection < 0 || selection > maximum) {
                return false;
            }
        }
        return true;
    }
}
