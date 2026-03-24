package com.teddy.stormwand.spell;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

public class StormLanceSpell implements WandSpell {
    private static final DustParticleOptions LANCE_CORE = new DustParticleOptions(new Vector3f(0.45F, 0.92F, 1.0F), 1.0F);
    private static final DustParticleOptions LANCE_EDGE = new DustParticleOptions(new Vector3f(0.08F, 0.45F, 1.0F), 0.75F);
    private static final float[] DIRECT_DAMAGE = {5.0F, 7.0F, 9.5F, 12.0F, 15.0F, 17.5F, 20.0F, 23.0F, 26.0F, 30.0F};
    private static final int[] MANA_COST = {7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final double[] RANGE = {48.0D, 60.0D, 72.0D, 84.0D, 96.0D, 112.0D, 128.0D, 144.0D, 160.0D, 176.0D};

    private final ResourceLocation id;

    public StormLanceSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.storm_lance");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        LivingEntity target = context.directHitTarget();
        if (target == null || !target.isAlive()) {
            return SpellCastResult.NO_TARGET;
        }

        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        float damage = getDirectDamage(spellLevel) + getDirectEnchantBonus(context.wandStack(), target);

        target.setLastHurtByPlayer(player);
        target.hurt(player.damageSources().playerAttack(player), damage);

        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, context.wandStack());
        if (fireAspectLevel > 0) {
            target.setSecondsOnFire(2 * fireAspectLevel);
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 42, 0.2D, 0.35D, 0.2D, 0.02D);
        level.sendParticles(LANCE_CORE, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 20, 0.12D, 0.2D, 0.12D, 0.0D);
        level.sendParticles(LANCE_EDGE, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 20, 0.18D, 0.24D, 0.18D, 0.0D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.85F, 1.9F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.22F, 2.0F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return valueForLevel(MANA_COST, spellLevel);
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return 0;
    }

    @Override
    public double getCastRange(int spellLevel) {
        return valueForLevel(RANGE, spellLevel);
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.storm_lance.damage", formatNumber(getDirectDamage(spellLevel))),
                Component.translatable("tooltip.stormwand.storm_lance.range", formatNumber(getCastRange(spellLevel))),
                Component.translatable("tooltip.stormwand.storm_lance.focused")
        );
    }

    @Override
    public boolean resolvesAtMaxRange() {
        return false;
    }

    private float getDirectDamage(int spellLevel) {
        return valueForLevel(DIRECT_DAMAGE, spellLevel);
    }

    private float getDirectEnchantBonus(ItemStack wandStack, LivingEntity target) {
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, wandStack);
        if (sharpnessLevel > 0) {
            return 0.5F * sharpnessLevel + 0.5F;
        }

        if (target.getMobType() == MobType.UNDEAD) {
            int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, wandStack);
            if (smiteLevel > 0) {
                return 2.5F * smiteLevel;
            }
        }

        if (target.getMobType() == MobType.ARTHROPOD) {
            int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, wandStack);
            if (baneLevel > 0) {
                return 2.5F * baneLevel;
            }
        }

        return 0.0F;
    }

    private float valueForLevel(float[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private int valueForLevel(int[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private double valueForLevel(double[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
