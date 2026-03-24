package com.teddy.stormwand.entity;

import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class StormGrenadeProjectile extends ThrowableItemProjectile {
    private int fuseTicks = -1;
    private int storedTargetId = -1;

    public StormGrenadeProjectile(EntityType<? extends StormGrenadeProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public StormGrenadeProjectile(Level level, LivingEntity shooter, ItemStack wandStack, ResourceLocation spellId, int spellLevel) {
        super(ModEntities.STORM_GRENADE.get(), shooter, level);
        ItemStack projectileStack = wandStack.copy();
        projectileStack.setCount(1);
        WandSpellData.setSelectedSpell(projectileStack, spellId);
        WandSpellData.setSpellLevel(projectileStack, spellId, spellLevel);
        this.setItem(projectileStack);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }

        if (fuseTicks >= 0) {
            if (fuseTicks-- <= 0) {
                explode();
            }
            return;
        }

        WandSpell spell = WandSpellData.getSelectedSpell(this.getItem());
        int spellLevel = WandSpellData.getSpellLevel(this.getItem(), spell.id());
        double maxRange = spell.getCastRange(spellLevel);
        if (this.getOwner() != null && this.distanceToSqr(this.getOwner()) >= maxRange * maxRange) {
            explode();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.fuseTicks = 12;
        if (result instanceof EntityHitResult entityHitResult) {
            this.storedTargetId = entityHitResult.getEntity().getId();
        }
    }

    private void explode() {
        if (!(this.getOwner() instanceof ServerPlayer player)) {
            this.discard();
            return;
        }

        LivingEntity target = this.storedTargetId >= 0 && this.level().getEntity(this.storedTargetId) instanceof LivingEntity living ? living : null;
        WandSpell spell = WandSpellData.getSelectedSpell(this.getItem());
        int spellLevel = WandSpellData.getSpellLevel(this.getItem(), spell.id());
        if (spell instanceof com.teddy.stormwand.spell.StormGrenadeSpell grenadeSpell) {
            grenadeSpell.explode(player, this, this.getItem(), this.position(), target, spellLevel);
        }
        this.discard();
    }
}