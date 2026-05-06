package com.ultramega.ae2importexportcard.datagen;

import appeng.datagen.providers.models.AE2ModelProvider;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client e) {
        final DataGenerator generator = e.getGenerator();
        final DataGenerator.PackGenerator pack = generator.getVanillaPack(true);
        pack.addProvider(AE2ModelProvider.create(MOD_ID, ItemModelProvider::new));
        pack.addProvider(output -> new MainRecipeProvider.Runner(output, e.getLookupProvider()));
    }
}
