package com.teddy.stormwand.entity;

import com.teddy.stormwand.spell.ArcMineSpell;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ArcMineProjectile extends ThrowableItemProjectile {
    private boolean anchored;
    private int armTicks = -1;

    public ArcMineProjectile(EntityType<? extends ArcMineProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ArcMineProjectile(Level level, LivingEntity shooter, ItemStack wandStack, ResourceLocation spellId, int spellLevel) {
        super(ModEntities.ARC_MINE.get(), shooter, level);
        ItemStack projectileStack = wandStack.copy();
        projectileStack.setCount(1);
        WandSpellData.setSelectedSpell(projectileStack, spellId);
        WandSpellData.setSpellLevel(projectileStack, spellId, spellLevel);
        this.setItem(projectileStack);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SCULK_CATALYST;
    }

    @Override
    public void tick() {
        super.tick();

        if (anchored) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide) {
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
            if (armTicks == -2 && this.tickCount % 5 == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.05D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (armTicks >= 0) {
            armTicks--;
            if (armTicks < 0) {
                armTicks = -2;
            }
        }

        if (armTicks == -2 && this.getOwner() instanceof ServerPlayer player) {
            WandSpell spell = WandSpellData.getSelectedSpell(this.getItem());
            int spellLevel = WandSpellData.getSpellLevel(this.getItem(), spell.id());
            if (spell instanceof ArcMineSpell arcMineSpell && arcMineSpell.hasTriggerTarget(this, spellLevel)) {
                arcMineSpell.detonate(player, this, this.getItem(), spellLevel);
                this.discard();
                return;
            }
        }

        if (this.tickCount > 20 * 45) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        this.anchored = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.armTicks = 14;
    }
}
