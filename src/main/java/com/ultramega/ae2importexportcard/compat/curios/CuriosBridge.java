package com.ultramega.ae2importexportcard.compat.curios;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class CuriosBridge {
    private CuriosBridge() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static List<Slot> getSlots(Player player) {
        return isLoaded() ? CuriosCompat.getSlots(player) : List.of();
    }

    public static boolean isEquipped(Player player, ItemStack stack) {
        return isLoaded() && CuriosCompat.isEquipped(player, stack);
    }

    public record Slot(String identifier,
                       int index,
                       ItemStack stack,
                       ResourceHandler<ItemResource> handler,
                       Identifier icon) {
        public String key() {
            return this.identifier + "/" + this.index;
        }

        public ItemAccess access() {
            return ItemAccess.forHandlerIndex(this.handler, this.index);
        }
    }
}
