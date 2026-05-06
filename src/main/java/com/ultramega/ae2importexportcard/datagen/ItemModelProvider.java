package com.ultramega.ae2importexportcard.datagen;

import com.ultramega.ae2importexportcard.registry.ModItems;

import appeng.datagen.providers.models.ModelSubProvider;
import appeng.datagen.providers.models.PartModelOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.ItemLike;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

public class ItemModelProvider extends ModelSubProvider {
    public ItemModelProvider(final BlockModelGenerators blockModels,
                             final ItemModelGenerators itemModels,
                             final PartModelOutput partModels) {
        super(blockModels, itemModels, partModels);
    }

    @Override
    protected void register() {
        this.flatSingleLayer(ModItems.IMPORT_CARD, "item/import_card");
        this.flatSingleLayer(ModItems.EXPORT_CARD, "item/export_card");
    }

    private void flatSingleLayer(ItemLike item, String texture) {
        var model = ModelTemplates.FLAT_ITEM.create(item.asItem(), TextureMapping.layer0(new Material(makeId(texture))), this.itemModels.modelOutput);
        this.itemModels.itemModelOutput.accept(item.asItem(), ItemModelUtils.plainModel(model));
    }
}