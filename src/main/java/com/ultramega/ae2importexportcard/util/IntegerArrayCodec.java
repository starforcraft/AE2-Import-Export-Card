package com.ultramega.ae2importexportcard.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;

public interface IntegerArrayCodec {
    Codec<IntList> INT_LIST_CODEC = Codec.INT.listOf().xmap(IntArrayList::new, ArrayList::new);

    StreamCodec<ByteBuf, IntList> INT_LIST_STREAM_CODEC = ByteBufCodecs.INT
            .apply(ByteBufCodecs.list())
            .map(IntArrayList::new, ArrayList::new);
}
