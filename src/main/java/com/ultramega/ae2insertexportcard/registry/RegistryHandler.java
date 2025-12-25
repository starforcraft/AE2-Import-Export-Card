package com.ultramega.ae2insertexportcard.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class RegistryHandler {
    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(bus);
        ModMenuTypes.init();
    }
}
