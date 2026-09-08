package com.ultramega.ae2importexportcard.network;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.util.BlockPickerHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BlockPickerData(BlockPos blockPos, Direction direction) implements CustomPacketPayload {
    public static final Type<BlockPickerData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "block_picker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPickerData> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BlockPickerData::blockPos,
        Direction.STREAM_CODEC, BlockPickerData::direction,
        BlockPickerData::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockPickerHandler.handle(player, this.blockPos, this.direction);
            }
        }).exceptionally(e -> null);
    }
}
