package com.ultramega.ae2importexportcard.network;

import com.ultramega.ae2importexportcard.util.BlockPickerHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

public record BlockPickerData(BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<BlockPickerData> TYPE = new Type<>(makeId("block_picker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPickerData> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BlockPickerData::blockPos,
        BlockPickerData::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockPickerHandler.handle(player, this.blockPos);
            }
        });
    }
}
