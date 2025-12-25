package com.ultramega.ae2insertexportcard.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final CreativeModeTab TAB_AE2INSERTEXPORTCARD = new CreativeModeTab("ae2insertexportcard") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.INSERT_CARD.get());
        }
    };
}