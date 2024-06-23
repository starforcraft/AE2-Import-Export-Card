package com.ultramega.ae2importexportcard.network;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.container.CardPlayerSlot;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record LockSlotUpdateData(int slotId, boolean cancelPickup) implements CustomPacketPayload {
    public static final Type<LockSlotUpdateData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "lock_slot_update_data"));

    public static final StreamCodec<ByteBuf, LockSlotUpdateData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, LockSlotUpdateData::slotId,
            ByteBufCodecs.BOOL, LockSlotUpdateData::cancelPickup,
            LockSlotUpdateData::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if(player.containerMenu instanceof UpgradeContainerMenu) {
                if(player.containerMenu.slots.get(slotId) instanceof CardPlayerSlot cardSlot) {
                    cardSlot.setCancelPickup(cancelPickup);
                }
            }
        }).exceptionally(e -> null);
    }
}
