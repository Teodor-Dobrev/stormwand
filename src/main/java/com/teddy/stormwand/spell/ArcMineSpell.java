package com.teddy.stormwand.spell;

import com.teddy.stormwand.entity.ArcMineProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class ArcMineSpell implements WandSpell {
    private static final int[] MANA_COST = {18, 22, 26, 30, 34};
    private static final int[] COOLDOWN = {90, 105, 120, 138, 160};
    private static final double[] RANGE = {24.0D, 28.0D, 32.0D, 36.0D, 42.0D};
    private static final double[] TRIGGER_RADIUS = {1.6D, 2.0D, 2.4D, 2.8D, 3.2D};
    private static final double[] SPLASH_RADIUS = {4.0D, 4.8D, 5.6D, 6.5D, 7.5D};
    private static final float[] DAMAGE = {18.0F, 26.0F, 34.0F, 44.0F, 56.0F};

    private final ResourceLocation id;

    public ArcMineSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.arc_mine");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        ArcMineProjectile projectile = new ArcMineProjectile(player.level(), player, wandStack, id(), spellLevel);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 0.08F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.55F, 0.6F);
        return SpellCastResult.SUCCESS;
    }

    public boolean hasTriggerTarget(ArcMineProjectile mine, int spellLevel) {
        if (!(mine.getOwner() instanceof ServerPlayer caster)) {
            return false;
        }

        double triggerRadius = getTriggerRadius(spellLevel);
        return !mine.level().getEntitiesOfClass(Mob.class, mine.getBoundingBox().inflate(triggerRadius), entity ->
                entity.isAlive()
                        && entity != mine.getOwner()
                        && SpellTargeting.isValidAutoTarget(entity, caster)).isEmpty();
    }

    public void detonate(ServerPlayer player, ArcMineProjectile mine, ItemStack wandStack, int spellLevel) {
        ServerLevel level = player.serverLevel();
        Vec3 center = mine.position();
        double splashRadius = getSplashRadius(spellLevel);
        float damage = getDamage(spellLevel);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 0.35D, 0.25D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 90, 1.0D, 0.5D, 1.0D, 0.18D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.35F);

        List<Mob> targets = level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(splashRadius), mob ->
                mob.isAlive()
                        && mob != mine.getOwner()
                        && SpellTargeting.isValidAutoTarget(mob, player));

        for (Mob target : targets) {
            int fireAspectLevel = wandStack.getEnchantmentLevel(Enchantments.FIRE_ASPECT);
            if (fireAspectLevel > 0) {
                target.setSecondsOnFire(2 * fireAspectLevel);
            }
            float dealtDamage = damage + getEnchantBonus(wandStack, target);
            target.setLastHurtByPlayer(player);
            target.hurt(player.damageSources().playerAttack(player), dealtDamage);
        }
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
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.arc_mine.damage", format(getDamage(spellLevel))),
                Component.translatable("tooltip.stormwand.arc_mine.radius", format(getSplashRadius(spellLevel))),
                Component.translatable("tooltip.stormwand.arc_mine.trigger", format(getTriggerRadius(spellLevel)))
        );
    }

    private float getEnchantBonus(ItemStack wandStack, LivingEntity target) {
        int sharpnessLevel = wandStack.getEnchantmentLevel(Enchantments.SHARPNESS);
        if (sharpnessLevel > 0) {
            return 0.5F * sharpnessLevel + 0.5F;
        }
        if (target.getMobType() == MobType.UNDEAD) {
            int smiteLevel = wandStack.getEnchantmentLevel(Enchantments.SMITE);
            if (smiteLevel > 0) {
                return 2.5F * smiteLevel;
            }
        }
        if (target.getMobType() == MobType.ARTHROPOD) {
            int baneLevel = wandStack.getEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS);
            if (baneLevel > 0) {
                return 2.5F * baneLevel;
            }
        }
        return 0.0F;
    }

    private double getTriggerRadius(int spellLevel) {
        return valueForLevel(TRIGGER_RADIUS, spellLevel);
    }

    private double getSplashRadius(int spellLevel) {
        return valueForLevel(SPLASH_RADIUS, spellLevel);
    }

    private float getDamage(int spellLevel) {
        return valueForLevel(DAMAGE, spellLevel);
    }

    private int valueForLevel(int[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private float valueForLevel(float[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private double valueForLevel(double[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
