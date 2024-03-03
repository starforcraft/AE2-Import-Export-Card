package com.ultramega.ae2insertexportcard.item;

import appeng.api.config.IncludeExclude;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class UpgradeHost {
    public static final String NBT_SELECTED_INVENTORY_SLOTS = "SelectedInventorySlots";
    public static final String NBT_FILTER_MODE = "FilterMode";

    public final ConfigInventory filterConfig = ConfigInventory.configTypes(18, this::updateFilter);

    private final UpgradeType type;
    private final ItemStack itemStack;
    private final WirelessTerminalMenuHost host;

    public UpgradeHost(UpgradeType type, int id, Inventory inventory, WirelessTerminalMenuHost host) {
        this.type = type;
        this.itemStack = host.getItemStack();
        this.host = host;

        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag == null) {
                    tag = new CompoundTag();
                }

                filterConfig.readFromChildTag(tag, "filterConfig");

                tagList.getCompound(i).put("tag", tag);
            }
        }
    }

    public IncludeExclude getFilterMode() {
        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag != null && tag.contains(NBT_FILTER_MODE)) {
                    return tag.getBoolean(NBT_FILTER_MODE) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
                }
            }
        }

        return IncludeExclude.WHITELIST;
    }

    public void toggleFilterMode(IncludeExclude filterMode) {
        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag == null) {
                    tag = new CompoundTag();
                }

                tag.putBoolean(NBT_FILTER_MODE, filterMode == IncludeExclude.WHITELIST);

                tagList.getCompound(i).put("tag", tag);
            }
        }
    }

    private void updateFilter() {
        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag == null) {
                    tag = new CompoundTag();
                }

                filterConfig.writeToChildTag(tag, "filterConfig");

                tagList.getCompound(i).put("tag", tag);
            }
        }
    }

    public void setSelectedInventorySlots(int[] selectedInventorySlots) {
        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag == null) {
                    tag = new CompoundTag();
                }

                tag.putIntArray(NBT_SELECTED_INVENTORY_SLOTS, selectedInventorySlots);

                tagList.getCompound(i).put("tag", tag);
            }
        }
    }

    public int[] getSelectedInventorySlots() {
        ListTag tagList = itemStack.getTag().getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            if (isInsertOrExportCard(type, tagList, i)) {
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if (tag != null && tag.contains(NBT_SELECTED_INVENTORY_SLOTS)) {
                    return tag.getIntArray(NBT_SELECTED_INVENTORY_SLOTS);
                }
            }
        }

        return new int[36];
    }

    public boolean isInsertOrExportCard(UpgradeType type, ListTag tagList, int index) {
        String tagId = tagList.getCompound(index).getString("id");

        return tagId.equals(new ResourceLocation(AE2InsertExportCard.MOD_ID, type.getName() + "_card").toString());
    }
}
