package com.ultramega.ae2insertexportcard.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.me.helpers.PlayerSource;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import com.ultramega.ae2insertexportcard.item.UpgradeHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(WirelessTerminalItem.class)
public class MixinWirelessTerminalItem extends Item {
    public MixinWirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide())
            return;
        if (!(entity instanceof ServerPlayer player))
            return;

        if(!stack.hasTag() || level.isClientSide)
            return;

        //Check if in access point range
        boolean inRange = false;
        IGrid grid = null;
        WirelessTerminalMenuHost host = null;

        if(stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem) {
            grid = wirelessTerminalItem.getLinkedGrid(stack, level, player);

            if(wirelessTerminalItem.getMenuHost(player, slotId, stack, null) instanceof WirelessTerminalMenuHost menuHost) {
                host = menuHost;
                inRange = host.rangeCheck();
            }
        }

        if(!inRange) return;

        if(stack.getTag().contains("upgrades")) {
            ListTag tagList = stack.getTag().getList("upgrades", Tag.TAG_COMPOUND);

            for (int i = 0; i < tagList.size(); i++) {
                boolean isInsertUpgrade = tagList.getCompound(i).getString("id").equals(new ResourceLocation(AE2InsertExportCard.MOD_ID, "insert_card").toString());
                boolean isExportUpgrade = tagList.getCompound(i).getString("id").equals(new ResourceLocation(AE2InsertExportCard.MOD_ID, "export_card").toString());
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if(tag != null && (isInsertUpgrade || isExportUpgrade)) {
                    int[] selectedInventorySlots = tag.getIntArray(UpgradeHost.NBT_SELECTED_INVENTORY_SLOTS);
                    boolean filterMode = isInsertUpgrade && tag.getBoolean(UpgradeHost.NBT_FILTER_MODE);

                    for(int j = 0; j < selectedInventorySlots.length; j++) {
                        if(selectedInventorySlots[j] >= 1) {
                            ItemStack itemInInventory = player.getInventory().getItem(j);

                            if((isExportUpgrade || itemInInventory.getItem() != Items.AIR) && itemInInventory != stack) {
                                List<Item> filters = new ArrayList<>();
                                if(tag.contains("filterConfig")) {
                                    ListTag tagList2 = tag.getList("filterConfig", Tag.TAG_COMPOUND);

                                    for(int k = 0; k < tagList2.size(); k++) {
                                        String tag2 = tagList2.getCompound(k).getString("id");
                                        filters.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag2)));
                                    }
                                }

                                var node = host.getActionableNode();
                                if(node == null) return;

                                if (filters.isEmpty() || (isExportUpgrade || (filterMode == filters.contains(itemInInventory.getItem())))) {
                                    if (isInsertUpgrade) {
                                        if(filters.isEmpty() && filterMode) return;

                                        long amount = StorageHelper.poweredInsert(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), AEItemKey.of(itemInInventory), itemInInventory.getCount(), new PlayerSource(player), Actionable.MODULATE);
                                        if(amount > 0) {
                                            player.getInventory().setItem(j, ItemStack.EMPTY);
                                            player.containerMenu.broadcastChanges();
                                        }
                                    } else {
                                        if(filters.isEmpty()) return;

                                        for(int k = 0; k < filters.size(); k++) {
                                            if (filters.get(k) == null || filters.get(k) == Items.AIR) continue;
                                            if (k != selectedInventorySlots[j] - 1) continue;

                                            Optional<IItemHandler> playerInventory = player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve();
                                            if (playerInventory.isPresent()) {
                                                if(itemInInventory.isEmpty() || itemInInventory.getItem() == filters.get(k)) {
                                                    int extractAmount = Math.min(itemInInventory.getMaxStackSize() - itemInInventory.getCount(), itemInInventory.getMaxStackSize());
                                                    int realExtractAmount = Math.min(filters.get(k).getDefaultInstance().getMaxStackSize(), extractAmount);

                                                    long extracted = StorageHelper.poweredExtraction(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), AEItemKey.of(filters.get(k)), realExtractAmount, new PlayerSource(player), Actionable.MODULATE);
                                                    if(extracted <= 0) {
                                                        continue;
                                                    }

                                                    playerInventory.get().insertItem(j, new ItemStack(filters.get(k), (int)extracted), false);
                                                    player.containerMenu.broadcastChanges();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
