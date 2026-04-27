package com.teddy.stormwand.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class StormWandConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue MANA_MAX;
    private static final ForgeConfigSpec.IntValue MANA_REGEN_PER_SECOND;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("mana");
        MANA_MAX = BUILDER.comment("Base maximum mana before armor enchantment bonuses are applied.")
                .defineInRange("maxMana", 100, 1, Integer.MAX_VALUE);
        MANA_REGEN_PER_SECOND = BUILDER.comment("Mana regenerated per second.")
                .defineInRange("manaRegenPerSecond", 5, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private StormWandConfig() {
    }

    public static int getManaMax() {
        return MANA_MAX.get();
    }

    public static int getManaRegenPerSecond() {
        return MANA_REGEN_PER_SECOND.get();
    }
}
