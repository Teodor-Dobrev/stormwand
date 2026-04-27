package com.teddy.stormwand.entity;

import com.teddy.stormwand.StormWandMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StormWandMod.MOD_ID);

    public static final RegistryObject<EntityType<StormBoltProjectile>> STORM_BOLT = ENTITY_TYPES.register(
            "storm_bolt",
            () -> EntityType.Builder.<StormBoltProjectile>of(StormBoltProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(12)
                    .updateInterval(5)
                    .build("storm_bolt")
    );

    public static final RegistryObject<EntityType<BallLightningProjectile>> BALL_LIGHTNING = ENTITY_TYPES.register(
            "ball_lightning",
            () -> EntityType.Builder.<BallLightningProjectile>of(BallLightningProjectile::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .updateInterval(4)
                    .build("ball_lightning")
    );

    public static final RegistryObject<EntityType<ArcMineProjectile>> ARC_MINE = ENTITY_TYPES.register(
            "arc_mine",
            () -> EntityType.Builder.<ArcMineProjectile>of(ArcMineProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(4)
                    .build("arc_mine")
    );

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
