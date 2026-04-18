package com.teddy.stormwand.spell;

import com.teddy.stormwand.entity.BallLightningProjectile;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class BallLightningSpell implements WandSpell {
    private static final DustParticleOptions CORE = new DustParticleOptions(new Vector3f(0.55F, 0.95F, 1.0F), 1.1F);
    private static final int[] MANA_COST = {12, 14, 16, 20, 24, 28, 32, 36, 40, 44};
    private static final int[] COOLDOWN = {20, 24, 28, 32, 36, 40, 44, 48, 52, 56};
    private static final double[] RANGE = {24.0D, 28.0D, 32.0D, 36.0D, 42.0D, 48.0D, 56.0D, 64.0D, 72.0D, 80.0D};
    private static final float[] PULSE_DAMAGE = {1.8F, 2.5F, 3.3F, 4.2F, 5.2F, 6.2F, 7.2F, 8.2F, 9.2F, 10.2F};
    private static final double[] PULSE_RADIUS = {2.5D, 3.0D, 3.4D, 3.9D, 4.4D, 5.0D, 5.6D, 6.2D, 6.8D, 7.5D};
    private static final int[] DURATION = {70, 95, 125, 160, 200, 245, 295, 350, 410, 475};
    private static final int[] MAX_TARGETS = {1, 1, 2, 2, 3, 3, 4, 5, 6, 8};

    private final ResourceLocation id;

    public BallLightningSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.ball_lightning");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return pulse(context.player(), context.sourceEntity(), context.wandStack(), context.impactPosition(), spellLevel);
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        BallLightningProjectile projectile = new BallLightningProjectile(player.level(), player, wandStack, id(), spellLevel);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.95F, 0.0F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.45F, 1.4F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return valueForLevel(MANA_COST, spellLevel);
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return valueForLevel(COOLDOWN, spellLevel);
    }

    @Override
    public double getCastRange(int spellLevel) {
        return valueForLevel(RANGE, spellLevel);
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.ball_lightning.damage", format(getPulseDamage(spellLevel))),
                Component.translatable("tooltip.stormwand.ball_lightning.radius", format(getPulseRadius(spellLevel))),
                Component.translatable("tooltip.stormwand.ball_lightning.duration", format(getDurationTicks(spellLevel) / 20.0D))
        );
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    public SpellCastResult pulse(ServerPlayer player, net.minecraft.world.entity.Entity sourceEntity, ItemStack wandStack, Vec3 center, int spellLevel) {
        Predicate<LivingEntity> hostile = this::isHostile;
        List<LivingEntity> hostiles = collect(player.level(), center, hostile, getPulseRadius(spellLevel));
        List<LivingEntity> targets = !hostiles.isEmpty()
                ? hostiles
                : (com.teddy.stormwand.config.StormWandConfig.allowAnimalFallback() ? collect(player.level(), center, this::isAnimal, getPulseRadius(spellLevel)) : List.of());
        if (targets.isEmpty()) {
            return SpellCastResult.NO_TARGET;
        }

        targets = targets.stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .limit(getMaxTargets(spellLevel))
                .toList();

        ServerLevel level = player.serverLevel();
        level.sendParticles(CORE, center.x, center.y, center.z, 16, 0.25D, 0.25D, 0.25D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 24, 0.3D, 0.3D, 0.3D, 0.04D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.15F, 1.9F);

        for (LivingEntity target : targets) {
            Vec3 targetCenter = getCenter(target);
            spawnArc(level, center, targetCenter);
            int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, wandStack);
            if (fireAspectLevel > 0) {
                target.setSecondsOnFire(2 * fireAspectLevel);
            }
            float damage = getPulseDamage(spellLevel) + getEnchantBonus(wandStack, target);
            target.setLastHurtByPlayer(player);
            target.hurt(player.damageSources().playerAttack(player), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetCenter.x, targetCenter.y, targetCenter.z, 18, 0.18D, 0.24D, 0.18D, 0.02D);
        }
        return SpellCastResult.SUCCESS;
    }

    public int getDurationTicks(int spellLevel) {
        return valueForLevel(DURATION, spellLevel);
    }

    private List<LivingEntity> collect(Level level, Vec3 center, Predicate<LivingEntity> predicate, double radius) {
        return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), entity ->
                entity.isAlive() && !entity.isSpectator() && predicate.test(entity)));
    }

    private void spawnArc(ServerLevel level, Vec3 start, Vec3 end) {
        int steps = Math.max(10, (int) Math.ceil(start.distanceTo(end) * 6.0D));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            Vec3 point = start.lerp(end, progress);
            level.sendParticles(CORE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (step % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 2, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }
    }

    private float getEnchantBonus(ItemStack wandStack, LivingEntity target) {
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

    private boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy;
    }

    private boolean isAnimal(LivingEntity entity) {
        return entity instanceof Animal;
    }

    private Vec3 getCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private float getPulseDamage(int spellLevel) {
        return valueForLevel(PULSE_DAMAGE, spellLevel);
    }

    private double getPulseRadius(int spellLevel) {
        return valueForLevel(PULSE_RADIUS, spellLevel);
    }

    private int getMaxTargets(int spellLevel) {
        return valueForLevel(MAX_TARGETS, spellLevel);
    }

    private int valueForLevel(int[] values, int spellLevel) {
        return values[getMappedLevelIndex(values.length, spellLevel)];
    }

    private float valueForLevel(float[] values, int spellLevel) {
        return values[getMappedLevelIndex(values.length, spellLevel)];
    }

    private double valueForLevel(double[] values, int spellLevel) {
        return values[getMappedLevelIndex(values.length, spellLevel)];
    }

    private int getMappedLevelIndex(int valueCount, int spellLevel) {
        if (valueCount <= 5) {
            return Math.max(0, Math.min(valueCount - 1, spellLevel - 1));
        }

        int clamped = Math.max(1, Math.min(getMaxLevel(), spellLevel));
        int mappedLevel = switch (clamped) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            default -> 10;
        };
        return Math.max(0, Math.min(valueCount - 1, mappedLevel - 1));
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
