package com.teddy.stormwand.spell;

import com.teddy.stormwand.StormWandMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID)
public final class StupifyControlEvents {
    private static final String STUPIFY_UNTIL_TAG = "StormStupifyUntil";
    private static final String PREVIOUS_NO_AI_TAG = "StormStupifyPreviousNoAi";

    private StupifyControlEvents() {
    }

    public static void applyStupify(LivingEntity target, long untilTick) {
        CompoundTag data = target.getPersistentData();
        data.putLong(STUPIFY_UNTIL_TAG, Math.max(data.getLong(STUPIFY_UNTIL_TAG), untilTick));
        if (target instanceof Mob mob && !data.contains(PREVIOUS_NO_AI_TAG)) {
            data.putBoolean(PREVIOUS_NO_AI_TAG, mob.isNoAi());
        }
    }

    public static boolean isStupified(LivingEntity entity) {
        Level level = entity.level();
        return level != null && entity.getPersistentData().getLong(STUPIFY_UNTIL_TAG) > level.getGameTime();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();
        long untilTick = data.getLong(STUPIFY_UNTIL_TAG);
        if (untilTick <= 0L) {
            return;
        }

        long gameTime = entity.level().getGameTime();
        if (gameTime >= untilTick) {
            clearStupify(entity, data);
            return;
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), 140));

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
            PathNavigation navigation = mob.getNavigation();
            navigation.stop();
        }

        if (!entity.level().isClientSide && entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.6D,
                    entity.getZ(),
                    6,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D
            );
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isStupified(attacker)) {
            event.setCanceled(true);
        }
    }

    private static void clearStupify(LivingEntity entity, CompoundTag data) {
        data.remove(STUPIFY_UNTIL_TAG);
        if (entity instanceof Mob mob && data.contains(PREVIOUS_NO_AI_TAG)) {
            mob.setNoAi(data.getBoolean(PREVIOUS_NO_AI_TAG));
        }
        data.remove(PREVIOUS_NO_AI_TAG);
    }
}
