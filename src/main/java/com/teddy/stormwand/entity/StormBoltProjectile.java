package com.teddy.stormwand.entity;

import com.teddy.stormwand.config.StormWandConfig;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.item.WandFireMode;
import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.SpellCastContext;
import com.teddy.stormwand.spell.SpellCastResult;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class StormBoltProjectile extends ThrowableItemProjectile {
    private static final DustParticleOptions BOLT_TRAIL = new DustParticleOptions(new Vector3f(0.18F, 0.78F, 1.0F), 0.8F);
    private static final DustParticleOptions BOLT_EDGE = new DustParticleOptions(new Vector3f(0.05F, 0.35F, 0.95F), 0.55F);
    private static final double NORMAL_DRAG = 0.99D;
    private static final double WATER_DRAG = 0.8D;
    private static final double DESPAWN_WITHOUT_VIEWER_RANGE = 192.0D;

    public StormBoltProjectile(EntityType<? extends StormBoltProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public StormBoltProjectile(Level level, LivingEntity shooter, ItemStack wandStack) {
        super(ModEntities.STORM_BOLT.get(), shooter, level);
        ItemStack projectileStack = wandStack.copy();
        projectileStack.setCount(1);
        this.setItem(projectileStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.STORM_WAND.get();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(NORMAL_DRAG / WATER_DRAG));
        }

        if (this.level().isClientSide) {
            Vec3 trailOffset = this.getDeltaMovement().scale(-0.18D);
            this.level().addParticle(BOLT_TRAIL, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            this.level().addParticle(BOLT_EDGE, this.getX() + trailOffset.x, this.getY(), this.getZ() + trailOffset.z, 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }

        if (!this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), DESPAWN_WITHOUT_VIEWER_RANGE)) {
            this.discard();
            return;
        }

        if (this.getOwner() != null && this.distanceToSqr(this.getOwner()) >= StormWandConfig.getCastRange() * StormWandConfig.getCastRange()) {
            if (WandFireMode.fromStack(this.getItem()).isSplitMode()) {
                resolveSpell(null, this.position());
            } else {
                this.discard();
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        LivingEntity directHitTarget = result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity livingEntity
                ? livingEntity
                : null;

        resolveSpell(directHitTarget, result.getLocation());
    }

    private void resolveSpell(LivingEntity directHitTarget, Vec3 impactPosition) {
        if (this.level().isClientSide) {
            return;
        }

        if (!(this.getOwner() instanceof ServerPlayer player)) {
            this.discard();
            return;
        }

        SpellCastResult castResult = ModSpells.getDefaultSpell().cast(
                new SpellCastContext(player, this, this.getItem(), impactPosition, directHitTarget)
        );

        if (castResult == SpellCastResult.NOT_ENOUGH_MANA) {
            player.displayClientMessage(Component.translatable("message.stormwand.not_enough_mana"), true);
        }

        this.discard();
    }
}