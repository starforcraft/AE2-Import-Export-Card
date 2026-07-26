package com.ultramega.ae2insertexportcard.network;

import com.ultramega.ae2insertexportcard.container.CardPlayerSlot;
import com.ultramega.ae2insertexportcard.container.UpgradeContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LockSlotUpdateMessage {
    private final int slotId;
    private final boolean cancelPickup;

    public LockSlotUpdateMessage(int slotId, boolean cancelPickup) {
        this.slotId = slotId;
        this.cancelPickup = cancelPickup;
    }

    public static LockSlotUpdateMessage decode(FriendlyByteBuf buf) {
        return new LockSlotUpdateMessage(buf.readInt(), buf.readBoolean());
    }

    public static void encode(LockSlotUpdateMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.slotId);
        buf.writeBoolean(message.cancelPickup);
    }

    public static void handle(LockSlotUpdateMessage message, Supplier<NetworkEvent.Context> ctx) {
        Player player = ctx.get().getSender();
        if (player != null) {
            ctx.get().enqueueWork(() -> {
                if (player.containerMenu instanceof UpgradeContainerMenu
                        && message.slotId >= 0 && message.slotId < player.containerMenu.slots.size()
                        && player.containerMenu.slots.get(message.slotId) instanceof CardPlayerSlot playerSlot) {
                    playerSlot.setCancelPickup(message.cancelPickup);
                }
            });
        }

        ctx.get().setPacketHandled(true);
    }
}
