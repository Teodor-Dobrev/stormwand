package com.teddy.stormwand.spell;

import com.teddy.stormwand.entity.StormGrenadeProjectile;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class StormGrenadeSpell implements WandSpell {
    private static final DustParticleOptions ARC_CORE = new DustParticleOptions(new Vector3f(0.35F, 0.88F, 1.0F), 1.05F);
    private static final DustParticleOptions ARC_EDGE = new DustParticleOptions(new Vector3f(0.10F, 0.48F, 1.0F), 0.8F);
    private static final int[] MANA_COST = {16, 18, 20, 24, 28, 32, 36, 40, 44, 48};
    private static final int[] COOLDOWN = {18, 20, 22, 26, 30, 34, 38, 42, 46, 50};
    private static final double[] RANGE = {28.0D, 34.0D, 40.0D, 46.0D, 54.0D, 62.0D, 72.0D, 84.0D, 96.0D, 110.0D};
    private static final float[] BASE_DAMAGE = {3.0F, 4.2F, 5.4F, 6.7F, 8.0F, 9.3F, 10.7F, 12.1F, 13.6F, 15.2F};
    private static final int[] MAX_TARGETS = {2, 3, 4, 5, 6, 7, 8, 9, 10, 12};
    private static final double[] BLAST_RADIUS = {3.0D, 3.4D, 3.8D, 4.4D, 5.0D, 5.6D, 6.2D, 6.8D, 7.4D, 8.0D};
    private static final float[] DIRECT_HIT_BONUS = {1.5F, 2.0F, 2.6F, 3.2F, 3.8F, 4.4F, 5.1F, 5.8F, 6.5F, 7.3F};

    private final ResourceLocation id;

    public StormGrenadeSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.storm_grenade");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return explode(context.player(), context.sourceEntity(), context.wandStack(), context.impactPosition(), context.directHitTarget(), spellLevel);
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        StormGrenadeProjectile projectile = new StormGrenadeProjectile(player.level(), player, wandStack, id(), spellLevel);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 0.1F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 0.7F);
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
                Component.translatable("tooltip.stormwand.storm_grenade.damage", format(getBaseDamage(spellLevel))),
                Component.translatable("tooltip.stormwand.storm_grenade.targets", getMaxTargets(spellLevel)),
                Component.translatable("tooltip.stormwand.storm_grenade.radius", format(getBlastRadius(spellLevel))),
                Component.translatable("tooltip.stormwand.storm_grenade.range", format(getCastRange(spellLevel)))
        );
    }

    public SpellCastResult explode(ServerPlayer player, net.minecraft.world.entity.Entity sourceEntity, ItemStack wandStack, Vec3 impactPosition, LivingEntity directHitTarget, int spellLevel) {
        LivingEntity primaryTarget = findPrimaryTarget(player.level(), impactPosition, directHitTarget, getBlastRadius(spellLevel));
        if (primaryTarget == null) {
            return SpellCastResult.NO_TARGET;
        }

        Predicate<LivingEntity> predicate = primaryTarget instanceof Enemy ? this::isHostileTarget : this::isAnimalTarget;
        List<ZapConnection> connections = buildConnections(player.level(), impactPosition, primaryTarget, predicate, spellLevel);
        if (connections.isEmpty()) {
            return SpellCastResult.NO_TARGET;
        }

        applyExplosion(player, sourceEntity, wandStack, impactPosition, directHitTarget, connections, spellLevel);
        return SpellCastResult.SUCCESS;
    }

    private LivingEntity findPrimaryTarget(Level level, Vec3 center, LivingEntity directHitTarget, double radius) {
        if (directHitTarget != null && directHitTarget.isAlive() && isHostileTarget(directHitTarget)) {
            return directHitTarget;
        }

        LivingEntity hostile = collect(level, center, this::isHostileTarget, radius).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .orElse(null);
        if (hostile != null) {
            return hostile;
        }

        if (!com.teddy.stormwand.config.StormWandConfig.allowAnimalFallback()) {
            return null;
        }

        if (directHitTarget != null && directHitTarget.isAlive() && isAnimalTarget(directHitTarget)) {
            return directHitTarget;
        }

        return collect(level, center, this::isAnimalTarget, radius).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .orElse(null);
    }

    private List<ZapConnection> buildConnections(Level level, Vec3 center, LivingEntity primaryTarget, Predicate<LivingEntity> predicate, int spellLevel) {
        int maxTargets = getMaxTargets(spellLevel);
        double radius = getBlastRadius(spellLevel);
        Set<UUID> hit = new HashSet<>();
        List<ZapConnection> connections = new ArrayList<>();
        List<LivingEntity> currentWave = new ArrayList<>();

        connections.add(new ZapConnection(center, primaryTarget));
        currentWave.add(primaryTarget);
        hit.add(primaryTarget.getUUID());

        while (hit.size() < maxTargets && !currentWave.isEmpty()) {
            List<LivingEntity> nextWave = new ArrayList<>();
            for (LivingEntity source : currentWave) {
                LivingEntity next = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(radius), entity ->
                                entity != source && entity.isAlive() && !entity.isSpectator() && !hit.contains(entity.getUUID()) && predicate.test(entity))
                        .stream()
                        .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(source)))
                        .orElse(null);
                if (next == null) {
                    continue;
                }
                connections.add(new ZapConnection(getCenter(source), next));
                nextWave.add(next);
                hit.add(next.getUUID());
                if (hit.size() >= maxTargets) {
                    break;
                }
            }
            currentWave = nextWave;
        }

        return connections;
    }

    private void applyExplosion(ServerPlayer player, net.minecraft.world.entity.Entity sourceEntity, ItemStack wandStack, Vec3 center, LivingEntity directHitTarget, List<ZapConnection> connections, int spellLevel) {
        ServerLevel level = player.serverLevel();
        spawnVisualLightning(level, center);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.5F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.25F, 1.8F);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 80, 0.8D, 0.4D, 0.8D, 0.15D);
        level.sendParticles(ARC_CORE, center.x, center.y, center.z, 40, 0.4D, 0.3D, 0.4D, 0.0D);

        float baseDamage = getBaseDamage(spellLevel);
        for (ZapConnection connection : connections) {
            Vec3 targetCenter = getCenter(connection.target());
            spawnArc(level, connection.start(), targetCenter);
            spawnVisualLightning(level, targetCenter);
            float damage = baseDamage;
            if (directHitTarget != null && connection.target().getUUID().equals(directHitTarget.getUUID())) {
                damage += getDirectHitBonus(spellLevel);
                damage += getDirectEnchantBonus(wandStack, connection.target());
            }
            int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, wandStack);
            if (fireAspectLevel > 0) {
                connection.target().setSecondsOnFire(2 * fireAspectLevel);
            }
            connection.target().setLastHurtByPlayer(player);
            connection.target().hurt(player.damageSources().playerAttack(player), damage);
        }
    }

    private List<LivingEntity> collect(Level level, Vec3 center, Predicate<LivingEntity> predicate, double radius) {
        return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), entity ->
                entity.isAlive() && !entity.isSpectator() && predicate.test(entity)));
    }

    private void spawnVisualLightning(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private void spawnArc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(18, Mth.ceil(delta.length() * 10.0D));
        for (int branch = 0; branch < 4; branch++) {
            double phaseShift = branch * (Math.PI / 2.0D);
            for (int step = 0; step <= steps; step++) {
                double progress = step / (double) steps;
                Vec3 point = start.lerp(end, progress);
                double angle = progress * Math.PI * 6.0D + phaseShift;
                Vec3 arcPoint = point.add(Math.cos(angle) * 0.08D, Math.sin(angle * 1.25D) * 0.05D, Math.sin(angle) * 0.08D);
                level.sendParticles(ARC_CORE, arcPoint.x, arcPoint.y, arcPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ARC_EDGE, arcPoint.x, arcPoint.y, arcPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (step % 2 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcPoint.x, arcPoint.y, arcPoint.z, 2, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }
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

    private boolean isHostileTarget(LivingEntity entity) {
        return entity instanceof Enemy;
    }

    private boolean isAnimalTarget(LivingEntity entity) {
        return entity instanceof Animal;
    }

    private float getBaseDamage(int spellLevel) {
        return valueForLevel(BASE_DAMAGE, spellLevel);
    }

    private int getMaxTargets(int spellLevel) {
        return valueForLevel(MAX_TARGETS, spellLevel);
    }

    private double getBlastRadius(int spellLevel) {
        return valueForLevel(BLAST_RADIUS, spellLevel);
    }

    private float getDirectHitBonus(int spellLevel) {
        return valueForLevel(DIRECT_HIT_BONUS, spellLevel);
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

    private Vec3 getCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record ZapConnection(Vec3 start, LivingEntity target) {
    }
}
