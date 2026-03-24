package com.teddy.stormwand.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public enum WandTier {
    STORM("storm_wand", 512, 0.00D, 0.00D, false, Rarity.UNCOMMON),
    SPARK("spark_wand", 768, 0.20D, 0.10D, false, Rarity.UNCOMMON),
    ARC("arc_wand", 1024, 0.40D, 0.20D, false, Rarity.RARE),
    TEMPEST("tempest_wand", 1536, 0.60D, 0.30D, false, Rarity.RARE),
    MAELSTROM("maelstrom_wand", 2048, 0.80D, 0.40D, false, Rarity.EPIC),
    ETERNAL("eternal_storm_wand", 0, 0.80D, 0.40D, true, Rarity.EPIC);

    private final String registryName;
    private final int durability;
    private final double manaDiscount;
    private final double cooldownReduction;
    private final boolean eternal;
    private final Rarity rarity;

    WandTier(String registryName, int durability, double manaDiscount, double cooldownReduction, boolean eternal, Rarity rarity) {
        this.registryName = registryName;
        this.durability = durability;
        this.manaDiscount = manaDiscount;
        this.cooldownReduction = cooldownReduction;
        this.eternal = eternal;
        this.rarity = rarity;
    }

    public String getRegistryName() {
        return this.registryName;
    }

    public int getDurability() {
        return this.durability;
    }

    public double getManaDiscount() {
        return this.manaDiscount;
    }

    public double getCooldownReduction() {
        return this.cooldownReduction;
    }

    public boolean isEternal() {
        return this.eternal;
    }

    public Rarity getRarity() {
        return this.rarity;
    }

    public static WandTier fromStack(ItemStack stack) {
        return stack.getItem() instanceof StormWandItem wandItem ? wandItem.getTier() : STORM;
    }
}