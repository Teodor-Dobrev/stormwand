package com.teddy.stormwand.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public enum WandTier {
    STORM("storm_wand", 0, false, Rarity.UNCOMMON),
    SPARK("spark_wand", 1, false, Rarity.UNCOMMON),
    ARC("arc_wand", 2, false, Rarity.RARE),
    TEMPEST("tempest_wand", 3, false, Rarity.RARE),
    MAELSTROM("maelstrom_wand", 4, false, Rarity.EPIC),
    ETERNAL("eternal_storm_wand", 4, true, Rarity.EPIC);

    private final String registryName;
    private final int configIndex;
    private final boolean eternal;
    private final Rarity rarity;

    WandTier(String registryName, int configIndex, boolean eternal, Rarity rarity) {
        this.registryName = registryName;
        this.configIndex = configIndex;
        this.eternal = eternal;
        this.rarity = rarity;
    }

    public String getRegistryName() {
        return this.registryName;
    }

    public int getConfigIndex() {
        return this.configIndex;
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