package com.teddy.stormwand.spell;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

public class ChainStormSpell implements WandSpell {
    private static final int MAX_IMPACT_BRANCHES = 4;
    private static final DustParticleOptions ARC_CORE = new DustParticleOptions(new Vector3f(0.18F, 0.78F, 1.0F), 0.95F);
    private static final DustParticleOptions ARC_EDGE = new DustParticleOptions(new Vector3f(0.05F, 0.35F, 0.95F), 0.7F);
    private static final int[] MANA_COST = {10, 12, 14, 17, 20, 24, 28, 32, 36, 40};
    private static final int[] COOLDOWN = {12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
    private static final double[] RANGE = {32.0D, 40.0D, 48.0D, 56.0D, 64.0D, 76.0D, 88.0D, 100.0D, 112.0D, 128.0D};
    private static final float[] BASE_DAMAGE = {2.5F, 3.5F, 4.5F, 5.5F, 6.5F, 7.5F, 8.8F, 10.0F, 11.2F, 12.5F};
    private static final int[] MAX_TARGETS = {2, 2, 3, 3, 4, 5, 6, 7, 8, 10};
    private static final double[] CHAIN_RADIUS = {3.0D, 3.5D, 4.0D, 4.5D, 5.0D, 5.5D, 6.0D, 6.5D, 7.0D, 7.5D};
    private static final float[] DIRECT_HIT_BONUS = {1.0F, 1.5F, 2.0F, 2.5F, 3.0F, 3.5F, 4.0F, 4.8F, 5.6F, 6.5F};

    private final ResourceLocation id;

    public ChainStormSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.chain_storm");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        LivingEntity primaryTarget = findPrimaryTarget(context, spellLevel);
        if (primaryTarget == null) {
            return SpellCastResult.NO_TARGET;
        }

        Predicate<LivingEntity> chainPredicate = primaryTarget instanceof Enemy ? this::isHostileTarget : this::isAnimalTarget;
        List<ZapConnection> connections = buildSplitConnections(context, primaryTarget, chainPredicate, spellLevel);
        if (connections.isEmpty()) {
            return SpellCastResult.NO_TARGET;
        }

        applyEffects(context, connections, spellLevel);
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
                Component.translatable("tooltip.stormwand.chain_storm.damage", formatNumber(getBaseDamage(spellLevel))),
                Component.translatable("tooltip.stormwand.chain_storm.targets", getMaxTargets(spellLevel)),
                Component.translatable("tooltip.stormwand.chain_storm.radius", formatNumber(getChainRadius(spellLevel))),
                Component.translatable("tooltip.stormwand.chain_storm.range", formatNumber(getCastRange(spellLevel)))
        );
    }

    private LivingEntity findPrimaryTarget(SpellCastContext context, int spellLevel) {
        LivingEntity directHitTarget = context.directHitTarget();
        if (directHitTarget != null && directHitTarget.isAlive() && this.isHostileTarget(directHitTarget)) {
            return directHitTarget;
        }

        double searchRadius = getChainRadius(spellLevel);
        LivingEntity nearbyHostile = findNearestTarget(context.player().level(), context.impactPosition(), this::isHostileTarget, searchRadius);
        if (nearbyHostile != null) {
            return nearbyHostile;
        }

        if (!com.teddy.stormwand.config.StormWandConfig.allowAnimalFallback()) {
            return null;
        }

        if (directHitTarget != null && directHitTarget.isAlive() && this.isAnimalTarget(directHitTarget)) {
            return directHitTarget;
        }

        return findNearestTarget(context.player().level(), context.impactPosition(), this::isAnimalTarget, searchRadius);
    }

    private LivingEntity findNearestTarget(Level level, Vec3 center, Predicate<LivingEntity> predicate, double radius) {
        return collectCandidates(level, center, predicate, radius).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .orElse(null);
    }

    private List<ZapConnection> buildSplitConnections(SpellCastContext context, LivingEntity primaryTarget, Predicate<LivingEntity> predicate, int spellLevel) {
        Level level = context.player().level();
        Vec3 impactPosition = context.impactPosition();
        int maxTargets = getMaxTargets(spellLevel);
        int branchCount = Math.min(MAX_IMPACT_BRANCHES, maxTargets);
        double chainRadius = getChainRadius(spellLevel);

        Set<UUID> hitEntities = new HashSet<>();
        List<BranchState> activeBranches = new ArrayList<>();
        List<ZapConnection> connections = new ArrayList<>();

        for (LivingEntity initialTarget : findInitialTargets(level, impactPosition, primaryTarget, predicate, branchCount, chainRadius)) {
            connections.add(new ZapConnection(impactPosition, initialTarget));
            hitEntities.add(initialTarget.getUUID());
            activeBranches.add(new BranchState(initialTarget));
        }

        while (hitEntities.size() < maxTargets && !activeBranches.isEmpty()) {
            List<BranchState> nextBranches = new ArrayList<>();

            for (BranchState branch : activeBranches) {
                if (hitEntities.size() >= maxTargets) {
                    break;
                }

                LivingEntity nextTarget = findNextTarget(level, branch.tail(), predicate, hitEntities, chainRadius);
                if (nextTarget == null) {
                    continue;
                }

                connections.add(new ZapConnection(getTargetCenter(branch.tail()), nextTarget));
                hitEntities.add(nextTarget.getUUID());
                nextBranches.add(new BranchState(nextTarget));
            }

            activeBranches = nextBranches;
        }

        return connections;
    }

    private List<LivingEntity> findInitialTargets(Level level, Vec3 impactPosition, LivingEntity primaryTarget, Predicate<LivingEntity> predicate, int branchCount, double chainRadius) {
        List<LivingEntity> candidates = collectCandidates(level, impactPosition, predicate, chainRadius);
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(impactPosition)));

        List<LivingEntity> initialTargets = new ArrayList<>();
        initialTargets.add(primaryTarget);

        for (LivingEntity candidate : candidates) {
            if (initialTargets.size() >= branchCount) {
                break;
            }
            if (candidate.getUUID().equals(primaryTarget.getUUID())) {
                continue;
            }
            initialTargets.add(candidate);
        }

        return initialTargets;
    }

    private List<LivingEntity> collectCandidates(Level level, Vec3 center, Predicate<LivingEntity> predicate, double searchRadius) {
        AABB searchBox = new AABB(center, center).inflate(searchRadius);
        return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive() && !entity.isSpectator() && predicate.test(entity)));
    }

    private LivingEntity findNextTarget(Level level, LivingEntity currentTarget, Predicate<LivingEntity> predicate, Set<UUID> hitEntities, double chainRadius) {
        AABB searchBox = currentTarget.getBoundingBox().inflate(chainRadius);

        return level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                        entity != currentTarget
                                && entity.isAlive()
                                && !entity.isSpectator()
                                && !hitEntities.contains(entity.getUUID())
                                && predicate.test(entity))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(currentTarget)))
                .orElse(null);
    }

    private void applyEffects(SpellCastContext context, List<ZapConnection> connections, int spellLevel) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        float baseDamage = getBaseDamage(spellLevel);
        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, context.wandStack());

        level.playSound(null, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.9F, 1.75F);
        level.playSound(null, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.16F, 1.85F);
        level.playSound(null, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.18F, 1.95F);
        level.sendParticles(ParticleTypes.FLASH, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ARC_CORE, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, 12, 0.12D, 0.12D, 0.12D, 0.0D);

        for (ZapConnection connection : connections) {
            Vec3 targetCenter = getTargetCenter(connection.target());
            spawnArc(level, connection.start(), targetCenter);

            float damage = baseDamage;
            if (fireAspectLevel > 0) {
                connection.target().setSecondsOnFire(2 * fireAspectLevel);
            }
            if (context.directHitTarget() != null && connection.target().getUUID().equals(context.directHitTarget().getUUID())) {
                damage += getDirectHitBonus(spellLevel);
                damage += getDirectEnchantBonus(context.wandStack(), connection.target());
            }

            connection.target().setLastHurtByPlayer(player);
            connection.target().hurt(player.damageSources().playerAttack(player), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetCenter.x, targetCenter.y, targetCenter.z, 72, 0.35D, 0.45D, 0.35D, 0.025D);
            level.sendParticles(ARC_CORE, targetCenter.x, targetCenter.y, targetCenter.z, 30, 0.2D, 0.2D, 0.2D, 0.0D);
            level.sendParticles(ARC_EDGE, targetCenter.x, targetCenter.y, targetCenter.z, 30, 0.28D, 0.28D, 0.28D, 0.0D);
            level.playSound(null, connection.target().getX(), connection.target().getY(), connection.target().getZ(), SoundEvents.ALLAY_HURT, SoundSource.PLAYERS, 0.28F, 1.8F);
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

    private void spawnArc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(20, Mth.ceil(delta.length() * 12.0D));

        for (int branch = 0; branch < 4; branch++) {
            double phaseShift = branch * (Math.PI / 2.0D);
            for (int step = 0; step <= steps; step++) {
                double progress = step / (double) steps;
                Vec3 point = start.lerp(end, progress);
                double angle = progress * Math.PI * 6.0D + phaseShift;
                double branchRadius = 0.06D;
                double offsetX = Math.cos(angle) * branchRadius;
                double offsetY = Math.sin((angle * 1.35D) + branch) * branchRadius * 0.45D;
                double offsetZ = Math.sin(angle) * branchRadius;
                Vec3 arcPoint = point.add(offsetX, offsetY, offsetZ);

                level.sendParticles(ARC_CORE, arcPoint.x, arcPoint.y, arcPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ARC_EDGE, arcPoint.x, arcPoint.y, arcPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (step % 2 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcPoint.x, arcPoint.y, arcPoint.z, 2, 0.015D, 0.015D, 0.015D, 0.0D);
                }
            }
        }
    }

    private Vec3 getTargetCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
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

    private double getChainRadius(int spellLevel) {
        return valueForLevel(CHAIN_RADIUS, spellLevel);
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

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record BranchState(LivingEntity tail) {
    }

    private record ZapConnection(Vec3 start, LivingEntity target) {
    }
}
