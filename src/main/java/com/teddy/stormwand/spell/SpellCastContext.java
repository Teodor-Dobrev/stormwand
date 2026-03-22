package com.teddy.stormwand.spell;

import com.teddy.stormwand.entity.StormBoltProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record SpellCastContext(
        ServerPlayer player,
        StormBoltProjectile projectile,
        ItemStack wandStack,
        Vec3 impactPosition,
        @Nullable LivingEntity directHitTarget
) {
}