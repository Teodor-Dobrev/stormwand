package com.teddy.stormwand.spell;

import com.teddy.stormwand.item.WandFireMode;
import com.teddy.stormwand.item.WandTier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ChainLightningSpell implements WandSpell {
    private static final int MAX_IMPACT_BRANCHES = 4;
    private static final DustParticleOptions ARC_CORE = new DustParticleOptions(new Vector3f(0.18F, 0.78F, 1.0F), 0.95F);
    private static final DustParticleOptions ARC_EDGE = new DustParticleOptions(new Vector3f(0.05F, 0.35F, 0.95F), 0.7F);

    private final ResourceLocation id;

    public ChainLightningSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public SpellCastResult cast(SpellCastContext context) {
        WandTier tier = WandTier.fromStack(context.wandStack());
        WandFireMode fireMode = WandFireMode.fromStack(context.wandStack());
        LivingEntity primaryTarget = fireMode.isSplitMode() ? findSplitTarget(context) : findFocusedTarget(context);
        if (primaryTarget == null) {
            return SpellCastResult.NO_TARGET;
        }

        Predicate<LivingEntity> chainPredicate = primaryTarget instanceof Enemy ? this::isHostileTarget : this::isAnimalTarget;
        List<ZapConnection> connections = fireMode.isSplitMode()
                ? buildSplitConnections(context, primaryTarget, chainPredicate, tier)
                : List.of(new ZapConnection(context.impactPosition(), primaryTarget));

        if (connections.isEmpty()) {
            return SpellCastResult.NO_TARGET;
        }

        if (!context.player().getAbilities().instabuild && fireMode.isSplitMode()) {
            context.player().getCooldowns().addCooldown(context.wandStack().getItem(), com.teddy.stormwand.config.StormWandConfig.getSpellCooldownTicks());
        }

        applyEffects(context, connections, tier);
        return SpellCastResult.SUCCESS;
    }

    private LivingEntity findFocusedTarget(SpellCastContext context) {
        LivingEntity directHitTarget = context.directHitTarget();
        if (directHitTarget == null || !directHitTarget.isAlive()) {
            return null;
        }

        if (this.isHostileTarget(directHitTarget)) {
            return directHitTarget;
        }

        if (com.teddy.stormwand.config.StormWandConfig.allowAnimalFallback() && this.isAnimalTarget(directHitTarget)) {
            return directHitTarget;
        }

        return null;
    }

    private LivingEntity findSplitTarget(SpellCastContext context) {
        LivingEntity directHitTarget = context.directHitTarget();
        if (directHitTarget != null && directHitTarget.isAlive() && this.isHostileTarget(directHitTarget)) {
            return directHitTarget;
        }

        LivingEntity nearbyHostile = findNearestTarget(context.player().level(), context.impactPosition(), this::isHostileTarget);
        if (nearbyHostile != null) {
            return nearbyHostile;
        }

        if (!com.teddy.stormwand.config.StormWandConfig.allowAnimalFallback()) {
            return null;
        }

        if (directHitTarget != null && directHitTarget.isAlive() && this.isAnimalTarget(directHitTarget)) {
            return directHitTarget;
        }

        return findNearestTarget(context.player().level(), context.impactPosition(), this::isAnimalTarget);
    }

    private LivingEntity findNearestTarget(Level level, Vec3 center, Predicate<LivingEntity> predicate) {
        return collectCandidates(level, center, predicate).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .orElse(null);
    }

    private List<ZapConnection> buildSplitConnections(SpellCastContext context, LivingEntity primaryTarget, Predicate<LivingEntity> predicate, WandTier tier) {
        Level level = context.player().level();
        Vec3 impactPosition = context.impactPosition();
        int maxTargets = com.teddy.stormwand.config.StormWandConfig.getMaxTargets(tier);
        int branchCount = Math.min(MAX_IMPACT_BRANCHES, maxTargets);

        Set<UUID> hitEntities = new HashSet<>();
        List<BranchState> activeBranches = new ArrayList<>();
        List<ZapConnection> connections = new ArrayList<>();

        for (LivingEntity initialTarget : findInitialTargets(level, impactPosition, primaryTarget, predicate, branchCount)) {
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

                LivingEntity nextTarget = findNextTarget(level, branch.tail(), predicate, hitEntities);
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

    private List<LivingEntity> findInitialTargets(Level level, Vec3 impactPosition, LivingEntity primaryTarget, Predicate<LivingEntity> predicate, int branchCount) {
        List<LivingEntity> candidates = collectCandidates(level, impactPosition, predicate);
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

    private List<LivingEntity> collectCandidates(Level level, Vec3 center, Predicate<LivingEntity> predicate) {
        double searchRadius = Math.max(com.teddy.stormwand.config.StormWandConfig.getChainRadius(), 4.5D);
        AABB searchBox = new AABB(center, center).inflate(searchRadius);

        return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && predicate.test(entity)));
    }

    private LivingEntity findNextTarget(Level level, LivingEntity currentTarget, Predicate<LivingEntity> predicate, Set<UUID> hitEntities) {
        double chainRadius = com.teddy.stormwand.config.StormWandConfig.getChainRadius();
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

    private void applyEffects(SpellCastContext context, List<ZapConnection> connections, WandTier tier) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        float baseDamage = com.teddy.stormwand.config.StormWandConfig.getChainDamage(tier);

        level.playSound(null, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.85F, 1.45F);
        level.playSound(null, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.12F, 1.9F);
        level.sendParticles(ParticleTypes.FLASH, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ARC_CORE, context.impactPosition().x, context.impactPosition().y, context.impactPosition().z, 12, 0.12D, 0.12D, 0.12D, 0.0D);

        for (ZapConnection connection : connections) {
            Vec3 targetCenter = getTargetCenter(connection.target());
            spawnArc(level, connection.start(), targetCenter);

            float damage = baseDamage;
            if (context.directHitTarget() != null && connection.target().getUUID().equals(context.directHitTarget().getUUID())) {
                damage += com.teddy.stormwand.config.StormWandConfig.getDirectHitBonusDamage();
                damage += getDirectEnchantBonus(context.wandStack(), connection.target());
                int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, context.wandStack());
                if (fireAspectLevel > 0) {
                    connection.target().setSecondsOnFire(2 * fireAspectLevel);
                }
            }

            connection.target().setLastHurtByPlayer(player);
            connection.target().hurt(player.damageSources().indirectMagic(context.projectile(), player), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetCenter.x, targetCenter.y, targetCenter.z, 54, 0.35D, 0.45D, 0.35D, 0.02D);
            level.sendParticles(ARC_CORE, targetCenter.x, targetCenter.y, targetCenter.z, 26, 0.2D, 0.2D, 0.2D, 0.0D);
            level.sendParticles(ARC_EDGE, targetCenter.x, targetCenter.y, targetCenter.z, 24, 0.28D, 0.28D, 0.28D, 0.0D);
            level.playSound(null, connection.target().getX(), connection.target().getY(), connection.target().getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.32F, 1.9F);
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
        int steps = Math.max(18, Mth.ceil(delta.length() * 12.0D));

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

    private record BranchState(LivingEntity tail) {
    }

    private record ZapConnection(Vec3 start, LivingEntity target) {
    }
}