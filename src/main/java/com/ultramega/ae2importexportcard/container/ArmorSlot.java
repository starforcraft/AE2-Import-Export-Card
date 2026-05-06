package com.ultramega.ae2importexportcard.container;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Almost exact copy of {@link net.minecraft.world.inventory.ArmorSlot}
 */
public class ArmorSlot extends CardPlayerSlot {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    private final Identifier emptyIcon;

    public ArmorSlot(Container container, LivingEntity owner, EquipmentSlot slot, int slotIndex, int x, int y, Identifier emptyIcon) {
        super(container, slotIndex, x, y);
        this.owner = owner;
        this.slot = slot;
        this.emptyIcon = emptyIcon;
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        this.owner.onEquipItem(this.slot, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.canEquip(this.slot, this.owner);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack itemstack = this.getItem();
        return (itemstack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
            && super.mayPickup(player);
    }

    @Override
    public Identifier getNoItemIcon() {
        return this.emptyIcon;
    }
}
