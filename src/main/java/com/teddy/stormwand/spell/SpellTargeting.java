package com.teddy.stormwand.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

public final class SpellTargeting {
    private SpellTargeting() {
    }

    public static boolean isValidAutoTarget(LivingEntity entity, ServerPlayer caster) {
        if (entity == null || caster == null || !entity.isAlive() || entity.isSpectator() || entity == caster) {
            return false;
        }

        if (entity instanceof Enemy) {
            return true;
        }

        if (entity instanceof NeutralMob neutralMob && neutralMob.isAngryAt(caster)) {
            return true;
        }

        if (entity instanceof Mob mob) {
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget != null && currentTarget.getUUID().equals(caster.getUUID())) {
                return true;
            }
        }

        LivingEntity lastHurt = entity.getLastHurtMob();
        return lastHurt != null && lastHurt.getUUID().equals(caster.getUUID());
    }
}
