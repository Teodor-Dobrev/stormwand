package com.teddy.stormwand.enchantment;

import com.teddy.stormwand.item.StormWandItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class InvincibleTradeEnchantment extends Enchantment {
    private static final EnchantmentCategory WAND_CATEGORY = EnchantmentCategory.create("stormwand", item -> item instanceof StormWandItem);

    public InvincibleTradeEnchantment() {
        super(Rarity.VERY_RARE, WAND_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 25;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof StormWandItem;
    }
}