package com.ultramega.ae2importexportcard.datagen;

import com.ultramega.ae2importexportcard.registry.ModItems;

import java.util.concurrent.CompletableFuture;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

public class MainRecipeProvider extends RecipeProvider {
    protected MainRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        this.shaped(RecipeCategory.MISC, ModItems.IMPORT_CARD, 1)
            .pattern("ERE")
            .pattern("XRA")
            .pattern("CRC")
            .define('X', AEParts.IMPORT_BUS)
            .define('R', Items.REDSTONE_BLOCK)
            .define('A', AEItems.ADVANCED_CARD)
            .define('C', AEItems.CALCULATION_PROCESSOR)
            .define('E', AEItems.ENGINEERING_PROCESSOR)
            .unlockedBy("has_calculation_processor", this.has(AEItems.CALCULATION_PROCESSOR))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.EXPORT_CARD, 1)
            .pattern("ERE")
            .pattern("XRA")
            .pattern("CRC")
            .define('X', AEParts.EXPORT_BUS)
            .define('R', Items.REDSTONE_BLOCK)
            .define('A', AEItems.ADVANCED_CARD)
            .define('C', AEItems.CALCULATION_PROCESSOR)
            .define('E', AEItems.ENGINEERING_PROCESSOR)
            .unlockedBy("has_calculation_processor", this.has(AEItems.CALCULATION_PROCESSOR))
            .save(this.output);
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new MainRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "AE2 Import Export Card Recipes";
        }
    }
}
