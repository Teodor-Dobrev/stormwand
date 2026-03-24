package com.teddy.stormwand.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record SpellCastContext(
        ServerPlayer player,
        Entity sourceEntity,
        ItemStack wandStack,
        Vec3 impactPosition,
        @Nullable LivingEntity directHitTarget
) {
}