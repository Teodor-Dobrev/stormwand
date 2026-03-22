package com.teddy.stormwand.config;

import com.teddy.stormwand.item.WandTier;
import net.minecraftforge.common.ForgeConfigSpec;

public final class StormWandConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue MANA_MAX;
    private static final ForgeConfigSpec.IntValue MANA_REGEN_PER_SECOND;
    private static final ForgeConfigSpec.IntValue MANA_COST;
    private static final ForgeConfigSpec.IntValue CHAIN_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.IntValue CAST_RANGE;
    private static final ForgeConfigSpec.DoubleValue CHAIN_RADIUS;
    private static final ForgeConfigSpec.DoubleValue DIRECT_HIT_BONUS_DAMAGE;
    private static final ForgeConfigSpec.BooleanValue ALLOW_ANIMAL_FALLBACK;
    private static final ForgeConfigSpec.DoubleValue[] TIER_DAMAGE = new ForgeConfigSpec.DoubleValue[5];
    private static final ForgeConfigSpec.IntValue[] TIER_MAX_TARGETS = new ForgeConfigSpec.IntValue[5];

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("mana");
        MANA_MAX = BUILDER.comment("Maximum mana available to the player.")
                .defineInRange("maxMana", 100, 1, Integer.MAX_VALUE);
        MANA_REGEN_PER_SECOND = BUILDER.comment("Mana regenerated per second.")
                .defineInRange("manaRegenPerSecond", 5, 0, Integer.MAX_VALUE);
        MANA_COST = BUILDER.comment("Mana spent by the chain lightning spell.")
                .defineInRange("manaCost", 20, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("chainLightning");
        CHAIN_COOLDOWN_TICKS = BUILDER.comment("Item cooldown after a successful cast.")
                .defineInRange("cooldownTicks", 20, 0, Integer.MAX_VALUE);
        CAST_RANGE = BUILDER.comment("Maximum distance the projectile can travel before it expires or resolves.")
                .defineInRange("castRange", 96, 1, 256);
        CHAIN_RADIUS = BUILDER.comment("Maximum distance between chained targets.")
                .defineInRange("chainRadius", 5.0D, 0.5D, 64.0D);
        DIRECT_HIT_BONUS_DAMAGE = BUILDER.comment("Extra damage dealt to the directly struck target before the chain continues.")
                .defineInRange("directHitBonusDamage", 3.0D, 0.0D, Double.MAX_VALUE);
        ALLOW_ANIMAL_FALLBACK = BUILDER.comment("Allow animals to be targeted when no hostiles are available at the impact point.")
                .define("allowAnimalFallback", true);

        double[] defaultDamage = {5.0D, 7.0D, 9.0D, 11.0D, 13.0D};
        int[] defaultMaxTargets = {4, 5, 6, 7, 8};
        String[] tierNames = {"storm", "spark", "arc", "tempest", "maelstrom"};

        for (int index = 0; index < tierNames.length; index++) {
            BUILDER.push(tierNames[index]);
            TIER_DAMAGE[index] = BUILDER.comment("Damage dealt to each target by the " + tierNames[index] + " wand tier.")
                    .defineInRange("damage", defaultDamage[index], 0.0D, Double.MAX_VALUE);
            TIER_MAX_TARGETS[index] = BUILDER.comment("Maximum number of targets hit by the " + tierNames[index] + " wand tier.")
                    .defineInRange("maxTargets", defaultMaxTargets[index], 1, 64);
            BUILDER.pop();
        }

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

    public static int getManaCost() {
        return MANA_COST.get();
    }

    public static int getSpellCooldownTicks() {
        return CHAIN_COOLDOWN_TICKS.get();
    }

    public static double getCastRange() {
        return CAST_RANGE.get();
    }

    public static double getChainRadius() {
        return CHAIN_RADIUS.get();
    }

    public static float getDirectHitBonusDamage() {
        return DIRECT_HIT_BONUS_DAMAGE.get().floatValue();
    }

    public static boolean allowAnimalFallback() {
        return ALLOW_ANIMAL_FALLBACK.get();
    }

    public static float getChainDamage(WandTier tier) {
        return TIER_DAMAGE[tier.getConfigIndex()].get().floatValue();
    }

    public static int getMaxTargets(WandTier tier) {
        return TIER_MAX_TARGETS[tier.getConfigIndex()].get();
    }
}