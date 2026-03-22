package com.teddy.stormwand.mana;

import com.teddy.stormwand.config.StormWandConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ManaHelper {
    private static final int BONUS_MANA_PER_ENCHANTMENT_LEVEL = 10;

    private ManaHelper() {
    }

    public static int getMaxMana(Player player) {
        return StormWandConfig.getManaMax() + getArmorBonusMana(player);
    }

    private static int getArmorBonusMana(Player player) {
        int bonusMana = 0;

        for (ItemStack armorPiece : player.getArmorSlots()) {
            if (armorPiece.isEmpty()) {
                continue;
            }

            int totalEnchantmentLevels = EnchantmentHelper.getEnchantments(armorPiece).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            bonusMana += totalEnchantmentLevels * BONUS_MANA_PER_ENCHANTMENT_LEVEL;
        }

        return bonusMana;
    }
}