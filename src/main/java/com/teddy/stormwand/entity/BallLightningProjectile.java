package com.teddy.stormwand.entity;

import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BallLightningProjectile extends ThrowableItemProjectile {
    private static final DustParticleOptions CORE = new DustParticleOptions(new Vector3f(0.55F, 0.95F, 1.0F), 1.1F);
    private boolean anchored;

    public BallLightningProjectile(EntityType<? extends BallLightningProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public BallLightningProjectile(Level level, LivingEntity shooter, ItemStack wandStack, ResourceLocation spellId, int spellLevel) {
        super(ModEntities.BALL_LIGHTNING.get(), shooter, level);
        ItemStack projectileStack = wandStack.copy();
        projectileStack.setCount(1);
        WandSpellData.setSelectedSpell(projectileStack, spellId);
        WandSpellData.setSpellLevel(projectileStack, spellId, spellLevel);
        this.setItem(projectileStack);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.HEART_OF_THE_SEA;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (anchored) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide) {
            this.level().addParticle(CORE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }

        WandSpell spell = WandSpellData.getSelectedSpell(this.getItem());
        int spellLevel = WandSpellData.getSpellLevel(this.getItem(), spell.id());
        if (this.tickCount % 10 == 0 && this.getOwner() instanceof ServerPlayer player && spell instanceof com.teddy.stormwand.spell.BallLightningSpell ballSpell) {
            ballSpell.pulse(player, this, this.getItem(), this.position(), spellLevel);
        }

        if (this.tickCount >= ((com.teddy.stormwand.spell.BallLightningSpell) spell).getDurationTicks(spellLevel)) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        this.anchored = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }
}