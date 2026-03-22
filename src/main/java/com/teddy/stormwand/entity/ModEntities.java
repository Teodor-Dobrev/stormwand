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

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}