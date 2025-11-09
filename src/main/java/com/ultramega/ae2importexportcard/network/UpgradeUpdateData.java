package com.ultramega.ae2importexportcard.network;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.util.IntegerArrayCodec;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record UpgradeUpdateData(int slotId, IntList selectedInventorySlots) implements CustomPacketPayload {
    public static final Type<UpgradeUpdateData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "upgrade_update_data"));

    public static final StreamCodec<FriendlyByteBuf, UpgradeUpdateData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UpgradeUpdateData::slotId,
            IntegerArrayCodec.INT_LIST_STREAM_CODEC, UpgradeUpdateData::selectedInventorySlots,
            UpgradeUpdateData::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof UpgradeContainerMenu containerMenu) {
                containerMenu.getUpgradeHost().setSelectedInventorySlots(this.selectedInventorySlots.toIntArray());
            }
        }).exceptionally(e -> null);
    }
}
