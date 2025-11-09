package com.ultramega.ae2importexportcard.registry;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.util.IntegerArrayCodec;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AE2ImportExportCard.MODID);

    private static final Consumer<DataComponentType.Builder<CompoundTag>> COMPOUND_TAG_CODECS = builder -> builder
            .persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG);

    private static final Consumer<DataComponentType.Builder<IntList>> INTEGER_ARRAY_TAG_CODECS = builder -> builder
            .persistent(IntegerArrayCodec.INT_LIST_CODEC).networkSynchronized(IntegerArrayCodec.INT_LIST_STREAM_CODEC);

    public static final DataComponentType<CompoundTag> FILTER_CONFIG = register("filter_config", COMPOUND_TAG_CODECS);

    public static final DataComponentType<IntList> SELECTED_INVENTORY_SLOTS = register("selected_inventory_slots", INTEGER_ARRAY_TAG_CODECS);

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DATA_COMPONENTS.register(name, () -> componentType);
        return componentType;
    }
}
