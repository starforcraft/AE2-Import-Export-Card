package com.ultramega.ae2importexportcard.network;

import com.ultramega.ae2importexportcard.compat.curios.CuriosBridge;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

public record CurioSlotUpdateData(int containerId, String key, int filter) implements CustomPacketPayload {
    public static final Type<CurioSlotUpdateData> TYPE = new Type<>(makeId("curio_slot_update"));
    public static final StreamCodec<FriendlyByteBuf, CurioSlotUpdateData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CurioSlotUpdateData::containerId,
        ByteBufCodecs.stringUtf8(256), CurioSlotUpdateData::key,
        ByteBufCodecs.VAR_INT, CurioSlotUpdateData::filter,
        CurioSlotUpdateData::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof UpgradeContainerMenu menu)
                || menu.containerId != this.containerId || menu.getUpgradeHost().getType() != UpgradeType.EXPORT
                || this.filter < 0 || this.filter > menu.getUpgradeHost().getActiveFilterSlotCount()) {
                return;
            }
            if (CuriosBridge.getSlots(player).stream().anyMatch(slot -> slot.key().equals(this.key))) {
                menu.getUpgradeHost().setSelectedCurioSlot(this.key, this.filter);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
